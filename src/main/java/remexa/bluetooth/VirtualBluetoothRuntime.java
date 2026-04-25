package remexa.bluetooth;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import remexa.host.LaunchConfig;
import remexa.probes.DebugLog;
import remexa.probes.LogCategory;

public final class VirtualBluetoothRuntime {
    public static final int SIGNAL_START = 0;
    public static final int SIGNAL_END = 1;
    public static final int SIGNAL_PAUSE = 2;
    public static final int SIGNAL_WAIT = 3;
    public static final int SIGNAL_REJECT = 4;
    public static final int CONN_OPENED = 5;
    public static final int CONN_CLOSED = 6;
    public static final int CONN_FAILED = 7;
    public static final int SUCCESS = 0;
    public static final int ERROR_NO_CONNECTION = 1;
    public static final int ERROR_GOT_NACK = 2;
    public static final int ERROR_ACK_TIMEOUT = 3;

    private static final int MAGIC = 0x52425431;
    private static final byte REQUEST_LIST_SERVICES = 1;
    private static final byte REQUEST_CONNECT = 2;
    private static final byte RESPONSE_OK = 0;
    private static final byte RESPONSE_ERROR = 1;
    private static final byte FRAME_STRING = 10;
    private static final byte FRAME_BYTES = 11;
    private static final byte FRAME_SIGNAL = 12;
    private static final byte FRAME_CLOSE = 13;
    private static final int CONNECT_TIMEOUT_MS = 3000;
    private static final VirtualBluetoothRuntime INSTANCE = new VirtualBluetoothRuntime();

    private final AtomicInteger nextConnectionId = new AtomicInteger(1);
    private final AtomicInteger nextListenId = new AtomicInteger(-1);
    private final AtomicInteger nextThreadId = new AtomicInteger(1);
    private final ExecutorService ioExecutor = Executors.newCachedThreadPool(runnable -> {
        var thread = new Thread(runnable, "ReMEXA-Bluetooth-" + nextThreadId.getAndIncrement());
        thread.setDaemon(true);
        return thread;
    });
    private final Object serverLock = new Object();
    private final ConcurrentHashMap<Integer, HostedService> hostedServices = new ConcurrentHashMap<>();

    private volatile ServerSocket serverSocket;
    private volatile int boundPort = -1;

    private VirtualBluetoothRuntime() {
    }

    public static VirtualBluetoothRuntime getInstance() {
        return INSTANCE;
    }

    public String localFriendlyName() {
        return currentSettings().localName();
    }

    public SessionHandle createSession(SessionCallbacks callbacks) {
        return new SessionHandle(callbacks == null ? new SessionCallbacks() { } : callbacks);
    }

    public DeviceInfo discoverRemoteDevice() throws IOException {
        var settings = requireClientSettings();
        var services = listRemoteServices(settings.remoteHost(), settings.port());
        var deviceName = services.hostName().isBlank() ? settings.remoteHost() : services.hostName();
        return new DeviceInfo(settings.remoteHost(), deviceName);
    }

    public ServiceInfo[] discoverServices(DeviceInfo device) throws IOException {
        Objects.requireNonNull(device, "device");
        var settings = requireClientSettings();
        var services = listRemoteServices(device.address(), settings.port());
        return services.services().toArray(ServiceInfo[]::new);
    }

    private ServiceListing listRemoteServices(String host, int port) throws IOException {
        try (var socket = openSocket(host, port);
             var output = new DataOutputStream(socket.getOutputStream());
             var input = new DataInputStream(socket.getInputStream())) {
            output.writeInt(MAGIC);
            output.writeByte(REQUEST_LIST_SERVICES);
            output.flush();

            var response = input.readByte();
            if (response != RESPONSE_OK) {
                throw new IOException(readString(input));
            }

            var hostName = readString(input);
            var serviceCount = input.readInt();
            var services = new ArrayList<ServiceInfo>(Math.max(0, serviceCount));
            for (int index = 0; index < serviceCount; index++) {
                services.add(readServiceInfo(input, host, hostName));
            }
            return new ServiceListing(hostName, services);
        }
    }

    private Socket openSocket(String host, int port) throws IOException {
        var socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
        socket.setTcpNoDelay(true);
        return socket;
    }

    private RuntimeSettings requireClientSettings() throws IOException {
        var settings = currentSettings();
        if (settings.backend() != LaunchConfig.BluetoothBackend.VIRTUAL_IP) {
            throw new IOException("Virtual Bluetooth is disabled.");
        }
        if (settings.role() != LaunchConfig.BluetoothRole.CLIENT) {
            throw new IOException("Bluetooth role must be set to client for discovery.");
        }
        return settings;
    }

    private RuntimeSettings requireHostSettings() throws IOException {
        var settings = currentSettings();
        if (settings.backend() != LaunchConfig.BluetoothBackend.VIRTUAL_IP) {
            throw new IOException("Virtual Bluetooth is disabled.");
        }
        if (settings.role() != LaunchConfig.BluetoothRole.HOST) {
            throw new IOException("Bluetooth role must be set to host for inbound connections.");
        }
        return settings;
    }

    private RuntimeSettings currentSettings() {
        return new RuntimeSettings(
                LaunchConfig.BluetoothBackend.resolveConfigured(),
                LaunchConfig.BluetoothRole.resolveConfigured(),
                LaunchConfig.resolveConfiguredBluetoothLocalName(),
                LaunchConfig.resolveConfiguredBluetoothRemoteHost(),
                LaunchConfig.resolveConfiguredBluetoothPort()
        );
    }

    private void ensureHostServer() throws IOException {
        var settings = requireHostSettings();
        synchronized (serverLock) {
            if (serverSocket != null && !serverSocket.isClosed() && boundPort == settings.port()) {
                return;
            }
            closeServerSocket();
            var socket = new ServerSocket(settings.port());
            socket.setReuseAddress(true);
            serverSocket = socket;
            boundPort = settings.port();
            ioExecutor.execute(() -> acceptLoop(socket));
            DebugLog.log(
                    LogCategory.BLUETOOTH,
                    VirtualBluetoothRuntime.class.getName(),
                    "Virtual Bluetooth host listening on port " + settings.port()
            );
        }
    }

    private void closeServerSocket() {
        var socket = serverSocket;
        serverSocket = null;
        boundPort = -1;
        if (socket == null) {
            return;
        }
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }

    private void acceptLoop(ServerSocket socket) {
        while (!socket.isClosed()) {
            try {
                var inbound = socket.accept();
                inbound.setTcpNoDelay(true);
                ioExecutor.execute(() -> handleInboundConnection(inbound));
            } catch (SocketException exception) {
                if (!socket.isClosed()) {
                    DebugLog.log(
                            LogCategory.BLUETOOTH,
                            VirtualBluetoothRuntime.class.getName(),
                            "Bluetooth host accept failed: " + exception.getMessage()
                    );
                }
                return;
            } catch (IOException exception) {
                DebugLog.log(
                        LogCategory.BLUETOOTH,
                        VirtualBluetoothRuntime.class.getName(),
                        "Bluetooth host accept failed: " + exception.getMessage()
                );
            }
        }
    }

    private void handleInboundConnection(Socket socket) {
        try {
            var input = new DataInputStream(socket.getInputStream());
            var output = new DataOutputStream(socket.getOutputStream());
            if (input.readInt() != MAGIC) {
                throw new IOException("Unexpected Bluetooth magic.");
            }
            var requestType = input.readByte();
            switch (requestType) {
                case REQUEST_LIST_SERVICES -> handleServiceListingRequest(socket, input, output);
                case REQUEST_CONNECT -> handleConnectRequest(socket, input, output);
                default -> throw new IOException("Unsupported Bluetooth request: " + requestType);
            }
        } catch (Exception exception) {
            DebugLog.log(
                    LogCategory.BLUETOOTH,
                    VirtualBluetoothRuntime.class.getName(),
                    "Bluetooth inbound request failed: " + exception.getMessage()
            );
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    private void handleServiceListingRequest(Socket socket, DataInputStream input, DataOutputStream output) throws IOException {
        var settings = currentSettings();
        var services = hostedServices.values().stream()
                .sorted(Comparator.comparingInt(HostedService::listenId).reversed())
                .map(HostedService::serviceInfo)
                .distinct()
                .toList();
        output.writeByte(RESPONSE_OK);
        writeString(output, settings.localName());
        output.writeInt(services.size());
        for (var service : services) {
            writeServiceInfo(output, service);
        }
        output.flush();
        socket.close();
    }

    private void handleConnectRequest(Socket socket, DataInputStream input, DataOutputStream output) throws IOException {
        var requestedServiceId = readString(input);
        var requestedServiceName = readString(input);
        var requestedSeed1 = readString(input);
        var requestedSeed2 = readString(input);
        var remoteFriendlyName = readString(input);
        var hostedService = hostedServices.values().stream()
                .filter(service -> service.matches(requestedServiceId, requestedServiceName, requestedSeed1, requestedSeed2))
                .max(Comparator.comparingInt(HostedService::listenId))
                .orElse(null);
        if (hostedService == null) {
            output.writeByte(RESPONSE_ERROR);
            writeString(output, "Requested Bluetooth service is not available.");
            output.flush();
            socket.close();
            return;
        }

        output.writeByte(RESPONSE_OK);
        writeString(output, currentSettings().localName());
        writeServiceInfo(output, hostedService.serviceInfo());
        output.flush();

        var connId = nextConnectionId.getAndIncrement();
        var remoteAddress = socket.getInetAddress().getHostAddress();
        var connection = new Connection(
                connId,
                hostedService.owner(),
                socket,
                input,
                output,
                remoteAddress,
                remoteFriendlyName
        );
        hostedService.owner().attachAcceptedConnection(connection);
    }

    private static void writeServiceInfo(DataOutputStream output, ServiceInfo service) throws IOException {
        writeString(output, service.serviceId());
        writeString(output, service.serviceName());
        writeString(output, service.seed1());
        writeString(output, service.seed2());
    }

    private static ServiceInfo readServiceInfo(DataInputStream input, String address, String friendlyName) throws IOException {
        return new ServiceInfo(
                address,
                friendlyName,
                readString(input),
                readString(input),
                readString(input),
                readString(input)
        );
    }

    private static void writeString(DataOutputStream output, String value) throws IOException {
        var bytes = (value == null ? "" : value).getBytes(StandardCharsets.UTF_8);
        output.writeInt(bytes.length);
        output.write(bytes);
    }

    private static String readString(DataInputStream input) throws IOException {
        var length = input.readInt();
        if (length < 0) {
            throw new IOException("Negative string length.");
        }
        var bytes = input.readNBytes(length);
        if (bytes.length != length) {
            throw new EOFException("Unexpected end of Bluetooth stream.");
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public interface SessionCallbacks {
        default void onConnectionStatus(int connId, int status) {
        }

        default void onMemberList(int[] connIds) {
        }

        default void onStringMessage(int connId, String message) {
        }

        default void onByteMessage(int connId, byte[] message) {
        }

        default void onSignal(int connId, int signal) {
        }

        default void onResult(int messageId, int[] connIds, int[] results) {
        }
    }

    public record DeviceInfo(String address, String friendlyName) {
    }

    public record ServiceInfo(
            String address,
            String friendlyName,
            String serviceId,
            String serviceName,
            String seed1,
            String seed2
    ) {
        public boolean matches(String requestedServiceId, String requestedServiceName, String requestedSeed1, String requestedSeed2) {
            var seedMatches = !requestedSeed1.isBlank()
                    && Objects.equals(seed1, requestedSeed1)
                    && Objects.equals(seed2, requestedSeed2);
            var idMatches = !requestedServiceId.isBlank() && Objects.equals(serviceId, requestedServiceId);
            var nameMatches = requestedServiceName.isBlank() || Objects.equals(serviceName, requestedServiceName);
            return nameMatches && (seedMatches || idMatches);
        }
    }

    public final class SessionHandle {
        private final SessionCallbacks callbacks;
        private final CopyOnWriteArrayList<Integer> connectionOrder = new CopyOnWriteArrayList<>();
        private final ConcurrentHashMap<Integer, Connection> activeConnections = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<Integer, HostedService> listeningServices = new ConcurrentHashMap<>();
        private final AtomicInteger nextMessageId = new AtomicInteger(1);

        private volatile SessionMode mode = SessionMode.NONE;

        private SessionHandle(SessionCallbacks callbacks) {
            this.callbacks = callbacks;
        }

        public int listen(ServiceInfo service) throws IOException {
            Objects.requireNonNull(service, "service");
            ensureHostServer();
            mode = SessionMode.HOST;
            var normalizedService = normalizeLocalService(service);
            clearHostedServices(normalizedService);
            var listenId = nextListenId.getAndDecrement();
            var hostedService = new HostedService(listenId, normalizedService, this);
            listeningServices.put(listenId, hostedService);
            hostedServices.put(listenId, hostedService);
            DebugLog.log(
                    LogCategory.BLUETOOTH,
                    VirtualBluetoothRuntime.class.getName(),
                    "Advertised Bluetooth service " + hostedService.serviceInfo().serviceName()
                            + " (" + hostedService.serviceInfo().serviceId() + ")"
            );
            return listenId;
        }

        public int connect(ServiceInfo service) throws IOException {
            Objects.requireNonNull(service, "service");
            var settings = requireClientSettings();
            mode = SessionMode.CLIENT;
            var socket = openSocket(service.address(), settings.port());
            var output = new DataOutputStream(socket.getOutputStream());
            var input = new DataInputStream(socket.getInputStream());
            output.writeInt(MAGIC);
            output.writeByte(REQUEST_CONNECT);
            writeString(output, service.serviceId());
            writeString(output, service.serviceName());
            writeString(output, service.seed1());
            writeString(output, service.seed2());
            writeString(output, settings.localName());
            output.flush();

            var response = input.readByte();
            if (response != RESPONSE_OK) {
                var error = readString(input);
                socket.close();
                throw new IOException(error);
            }

            var remoteFriendlyName = readString(input);
            var remoteService = readServiceInfo(input, service.address(), remoteFriendlyName);
            var connId = nextConnectionId.getAndIncrement();
            var connection = new Connection(
                    connId,
                    this,
                    socket,
                    input,
                    output,
                    remoteService.address(),
                    remoteFriendlyName
            );
            attachConnection(connection);
            callbacks.onConnectionStatus(connId, CONN_OPENED);
            callbacks.onMemberList(activeConnectionIds());
            return connId;
        }

        public boolean close(int connId) {
            var listeningService = listeningServices.remove(connId);
            if (listeningService != null) {
                hostedServices.remove(connId, listeningService);
                DebugLog.log(
                        LogCategory.BLUETOOTH,
                        VirtualBluetoothRuntime.class.getName(),
                        "Stopped advertising Bluetooth service " + listeningService.serviceInfo().serviceName()
                );
                return true;
            }

            var connection = resolveConnection(connId);
            if (connection == null) {
                return false;
            }
            connection.closeLocally();
            return true;
        }

        public int sendString(int[] connIds, String message) {
            return send(connIds, connection -> connection.sendString(message));
        }

        public int sendBytes(int[] connIds, byte[] message) {
            return send(connIds, connection -> connection.sendBytes(message));
        }

        public int sendSignal(int[] connIds, int signal) {
            return send(connIds, connection -> connection.sendSignal(signal));
        }

        public String getRemoteAddress(int connId) {
            var connection = resolveConnection(connId);
            return connection == null ? "" : connection.remoteAddress();
        }

        private int send(int[] connIds, ConnectionWriter writer) {
            if (connIds == null) {
                throw new NullPointerException("connIds");
            }
            var requested = connIds.clone();
            var results = new int[requested.length];
            var resolvedTargets = new ArrayList<Integer>(requested.length);
            for (int index = 0; index < requested.length; index++) {
                if (requested[index] < 0) {
                    // Some games keep fixed-size connID arrays and leave unused slots as -1.
                    // Treat those placeholders as ignored rather than hard send failures.
                    results[index] = SUCCESS;
                    continue;
                }
                var connection = resolveConnection(requested[index]);
                if (connection == null) {
                    results[index] = ERROR_NO_CONNECTION;
                    continue;
                }
                resolvedTargets.add(connection.connId());
                results[index] = writer.write(connection) ? SUCCESS : ERROR_NO_CONNECTION;
            }
            var messageId = nextMessageId.getAndIncrement();
            DebugLog.log(
                    LogCategory.BLUETOOTH,
                    VirtualBluetoothRuntime.class.getName(),
                    "Bluetooth send messageId=" + messageId
                            + " requested=" + Arrays.toString(requested)
                            + " resolved=" + resolvedTargets
                            + " results=" + Arrays.toString(results)
            );
            callbacks.onResult(messageId, requested, results);
            return messageId;
        }

        private void attachAcceptedConnection(Connection connection) {
            attachConnection(connection);
            callbacks.onConnectionStatus(connection.connId(), CONN_OPENED);
            callbacks.onMemberList(activeConnectionIds());
        }

        private void attachConnection(Connection connection) {
            activeConnections.put(connection.connId(), connection);
            connectionOrder.add(connection.connId());
            connection.startReader();
        }

        private void onConnectionClosed(Connection connection, int status) {
            if (activeConnections.remove(connection.connId(), connection)) {
                connectionOrder.remove(Integer.valueOf(connection.connId()));
                callbacks.onConnectionStatus(connection.connId(), status);
                callbacks.onMemberList(activeConnectionIds());
            }
        }

        private void onStringMessage(int connId, String message) {
            callbacks.onStringMessage(connId, message);
        }

        private void onByteMessage(int connId, byte[] message) {
            callbacks.onByteMessage(connId, message);
        }

        private void onSignal(int connId, int signal) {
            callbacks.onSignal(connId, signal);
        }

        private int[] activeConnectionIds() {
            var ids = new int[connectionOrder.size()];
            for (int index = 0; index < connectionOrder.size(); index++) {
                ids[index] = connectionOrder.get(index);
            }
            return ids;
        }

        private Connection resolveConnection(int requestedConnId) {
            var direct = activeConnections.get(requestedConnId);
            if (direct != null) {
                return direct;
            }
            if (mode != SessionMode.HOST) {
                return null;
            }
            if (requestedConnId < 0 || requestedConnId >= connectionOrder.size()) {
                return null;
            }
            return activeConnections.get(connectionOrder.get(requestedConnId));
        }

        private void clearHostedServices(ServiceInfo normalizedService) {
            for (var entry : listeningServices.entrySet()) {
                var existing = entry.getValue();
                if (!existing.serviceInfo().equals(normalizedService)) {
                    continue;
                }
                listeningServices.remove(entry.getKey(), existing);
                hostedServices.remove(entry.getKey(), existing);
            }
            hostedServices.entrySet().removeIf(entry -> entry.getValue().serviceInfo().equals(normalizedService));
        }
    }

    private ServiceInfo normalizeLocalService(ServiceInfo service) {
        var settings = currentSettings();
        return new ServiceInfo(
                "127.0.0.1",
                settings.localName(),
                emptyToBlank(service.serviceId()),
                emptyToBlank(service.serviceName()),
                emptyToBlank(service.seed1()),
                emptyToBlank(service.seed2())
        );
    }

    private static String emptyToBlank(String value) {
        return value == null ? "" : value;
    }

    private record HostedService(int listenId, ServiceInfo serviceInfo, SessionHandle owner) {
        private boolean matches(String requestedServiceId, String requestedServiceName, String requestedSeed1, String requestedSeed2) {
            return serviceInfo.matches(
                    emptyToBlank(requestedServiceId),
                    emptyToBlank(requestedServiceName),
                    emptyToBlank(requestedSeed1),
                    emptyToBlank(requestedSeed2)
            );
        }
    }

    private final class Connection {
        private final int connId;
        private final SessionHandle owner;
        private final Socket socket;
        private final DataInputStream input;
        private final DataOutputStream output;
        private final String remoteAddress;
        private final String remoteFriendlyName;
        private final AtomicBoolean closed = new AtomicBoolean();
        private final Object writeLock = new Object();

        private Connection(
                int connId,
                SessionHandle owner,
                Socket socket,
                DataInputStream input,
                DataOutputStream output,
                String remoteAddress,
                String remoteFriendlyName
        ) {
            this.connId = connId;
            this.owner = owner;
            this.socket = socket;
            this.input = input;
            this.output = output;
            this.remoteAddress = remoteAddress == null ? "" : remoteAddress;
            this.remoteFriendlyName = remoteFriendlyName == null ? "" : remoteFriendlyName;
        }

        private int connId() {
            return connId;
        }

        private String remoteAddress() {
            return remoteAddress;
        }

        private void startReader() {
            ioExecutor.execute(this::readLoop);
        }

        private boolean sendString(String message) {
            return writeFrame(FRAME_STRING, output -> writeString(output, message));
        }

        private boolean sendBytes(byte[] message) {
            Objects.requireNonNull(message, "message");
            return writeFrame(FRAME_BYTES, output -> {
                output.writeInt(message.length);
                output.write(message);
            });
        }

        private boolean sendSignal(int signal) {
            return writeFrame(FRAME_SIGNAL, output -> output.writeInt(signal));
        }

        private boolean writeFrame(byte type, FrameWriter writer) {
            synchronized (writeLock) {
                if (closed.get()) {
                    return false;
                }
                try {
                    output.writeByte(type);
                    writer.write(output);
                    output.flush();
                    return true;
                } catch (IOException exception) {
                    closeWithStatus(CONN_FAILED);
                    return false;
                }
            }
        }

        private void readLoop() {
            try {
                while (!closed.get()) {
                    var frameType = input.readByte();
                    switch (frameType) {
                        case FRAME_STRING -> owner.onStringMessage(connId, readString(input));
                        case FRAME_BYTES -> owner.onByteMessage(connId, input.readNBytes(input.readInt()));
                        case FRAME_SIGNAL -> owner.onSignal(connId, input.readInt());
                        case FRAME_CLOSE -> {
                            closeWithStatus(CONN_CLOSED);
                            return;
                        }
                        default -> throw new IOException("Unknown Bluetooth frame: " + frameType);
                    }
                }
            } catch (EOFException | SocketException ignored) {
                closeWithStatus(CONN_CLOSED);
            } catch (IOException exception) {
                DebugLog.log(
                        LogCategory.BLUETOOTH,
                        VirtualBluetoothRuntime.class.getName(),
                        "Bluetooth connection " + connId + " to " + remoteFriendlyName
                                + " (" + remoteAddress + ") failed: " + exception.getMessage()
                );
                closeWithStatus(CONN_FAILED);
            }
        }

        private void closeLocally() {
            synchronized (writeLock) {
                if (closed.get()) {
                    return;
                }
                try {
                    output.writeByte(FRAME_CLOSE);
                    output.flush();
                } catch (IOException ignored) {
                }
            }
            closeWithStatus(CONN_CLOSED);
        }

        private void closeWithStatus(int status) {
            if (!closed.compareAndSet(false, true)) {
                return;
            }
            try {
                socket.close();
            } catch (IOException ignored) {
            }
            owner.onConnectionClosed(this, status);
        }
    }

    @FunctionalInterface
    private interface ConnectionWriter {
        boolean write(Connection connection);
    }

    @FunctionalInterface
    private interface FrameWriter {
        void write(DataOutputStream output) throws IOException;
    }

    private enum SessionMode {
        NONE,
        HOST,
        CLIENT
    }

    private record RuntimeSettings(
            LaunchConfig.BluetoothBackend backend,
            LaunchConfig.BluetoothRole role,
            String localName,
            String remoteHost,
            int port
    ) {
    }

    private record ServiceListing(String hostName, List<ServiceInfo> services) {
    }
}
