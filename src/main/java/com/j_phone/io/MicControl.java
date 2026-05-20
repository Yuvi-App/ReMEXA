package com.j_phone.io;

public class MicControl {
    public static final int MIC = 1;
    public static final int PITCH_SCANNING = 2;

    private static final int MAX_VOLUME = 5;
    private static final int MAX_ECHO_LEVEL = 2;
    private static final int SCAN_INTERVAL_MS = 50;
    private static final float SAMPLE_RATE = 16000.0f;
    private static final int SAMPLE_BYTES = 2;
    private static final int MIN_PITCH_HZ = 70;
    private static final int MAX_PITCH_HZ = 1000;
    private static final com.j_phone.io.MicControl INSTANCE = new com.j_phone.io.MicControl();

    private static volatile com.j_phone.io.MicControlListener listener;

    private final Object scanLock = new Object();
    private volatile boolean enabled = true;
    private volatile boolean scanning;
    private int volume = MAX_VOLUME;
    private int echoLevel = 1;
    private javax.sound.sampled.TargetDataLine line;
    private Thread scanThread;

    public static com.j_phone.io.MicControl getInstance () throws java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.j_phone.io.MicControl", "getInstance");
        return INSTANCE;
    }

    public int getState () {
        remexa.probes.SdkStubSupport.log("com.j_phone.io.MicControl", "getState");
        return scanning ? PITCH_SCANNING : (enabled ? MIC : 0);
    }

    public void enable () throws java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.j_phone.io.MicControl", "enable");
        enabled = true;
        fireEvent(com.j_phone.io.MicControlListener.SWITCH_ON);
    }

    public void disable () {
        remexa.probes.SdkStubSupport.log("com.j_phone.io.MicControl", "disable");
        stopPitchScan();
        enabled = false;
        fireEvent(com.j_phone.io.MicControlListener.SWITCH_OFF);
    }

    public int getMaxVolume () {
        remexa.probes.SdkStubSupport.log("com.j_phone.io.MicControl", "getMaxVolume");
        return MAX_VOLUME;
    }

    public int getVolume () {
        remexa.probes.SdkStubSupport.log("com.j_phone.io.MicControl", "getVolume");
        return volume;
    }

    public void setVolume (int volume) {
        remexa.probes.SdkStubSupport.log("com.j_phone.io.MicControl", "setVolume", volume);
        this.volume = clamp(volume, 0, MAX_VOLUME);
        fireEvent(com.j_phone.io.MicControlListener.VOLUME_CHANGED);
    }

    public int getEchoMaxLevel () {
        remexa.probes.SdkStubSupport.log("com.j_phone.io.MicControl", "getEchoMaxLevel");
        return MAX_ECHO_LEVEL;
    }

    public int getEchoLevel () {
        remexa.probes.SdkStubSupport.log("com.j_phone.io.MicControl", "getEchoLevel");
        return echoLevel;
    }

    public void setEchoLevel (int level) {
        remexa.probes.SdkStubSupport.log("com.j_phone.io.MicControl", "setEchoLevel", level);
        echoLevel = clamp(level, 0, MAX_ECHO_LEVEL);
        fireEvent(com.j_phone.io.MicControlListener.ECHO_CHANGED);
    }

    public int getScanInterval () {
        remexa.probes.SdkStubSupport.log("com.j_phone.io.MicControl", "getScanInterval");
        return SCAN_INTERVAL_MS;
    }

    public void startPitchScan (com.j_phone.io.PitchScanData scandata) throws java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.j_phone.io.MicControl", "startPitchScan", scandata);
        if (scandata == null) {
            throw new NullPointerException("MicControl.startPitchScan: scandata is null.");
        }

        stopPitchScan();
        enabled = true;
        scandata.reset();
        synchronized (scanLock) {
            scanning = true;
            scanThread = new Thread(() -> scanLoop(scandata), "ReMEXA-MicControl");
            scanThread.setDaemon(true);
            scanThread.start();
        }
        fireEvent(com.j_phone.io.MicControlListener.PITCHSCAN_START);
    }

    public void stopPitchScan () {
        remexa.probes.SdkStubSupport.log("com.j_phone.io.MicControl", "stopPitchScan");
        Thread threadToInterrupt;
        synchronized (scanLock) {
            if (!scanning) {
                return;
            }
            scanning = false;
            closeLine();
            threadToInterrupt = scanThread;
            scanThread = null;
        }
        if (threadToInterrupt != null) {
            threadToInterrupt.interrupt();
        }
        fireEvent(com.j_phone.io.MicControlListener.PITCHSCAN_STOP);
    }

    public static void setMicControlListener (com.j_phone.io.MicControlListener listener) {
        remexa.probes.SdkStubSupport.log("com.j_phone.io.MicControl", "setMicControlListener", listener);
        com.j_phone.io.MicControl.listener = listener;
    }

    private void scanLoop(com.j_phone.io.PitchScanData scandata) {
        javax.sound.sampled.TargetDataLine activeLine = null;
        try {
            activeLine = openLine();
            synchronized (scanLock) {
                if (!scanning) {
                    return;
                }
                line = activeLine;
            }
            activeLine.start();
            byte[] buffer = new byte[Math.round(SAMPLE_RATE * SCAN_INTERVAL_MS / 1000.0f) * SAMPLE_BYTES];
            while (scanning) {
                int read = activeLine.read(buffer, 0, buffer.length);
                if (read <= 0) {
                    continue;
                }
                appendAnalysis(scandata, analyze(buffer, read));
            }
        } catch (Exception exception) {
            remexa.probes.DebugLog.log(
                    remexa.probes.LogCategory.IO,
                    MicControl.class.getName(),
                    "Microphone capture unavailable; using silent pitch scan fallback: " + exception.getMessage()
            );
            silentScanLoop(scandata);
        } finally {
            if (activeLine != null) {
                activeLine.close();
            }
            synchronized (scanLock) {
                if (line == activeLine) {
                    line = null;
                }
                if (Thread.currentThread() == scanThread) {
                    scanThread = null;
                }
                scanning = false;
            }
        }
    }

    private void silentScanLoop(com.j_phone.io.PitchScanData scandata) {
        while (scanning) {
            appendAnalysis(scandata, new Analysis(-1, 0));
            try {
                Thread.sleep(SCAN_INTERVAL_MS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private javax.sound.sampled.TargetDataLine openLine() throws javax.sound.sampled.LineUnavailableException {
        var format = new javax.sound.sampled.AudioFormat(
                SAMPLE_RATE,
                16,
                1,
                true,
                false
        );
        var info = new javax.sound.sampled.DataLine.Info(javax.sound.sampled.TargetDataLine.class, format);
        var target = (javax.sound.sampled.TargetDataLine) javax.sound.sampled.AudioSystem.getLine(info);
        target.open(format, Math.round(SAMPLE_RATE * 0.25f) * SAMPLE_BYTES);
        return target;
    }

    private void appendAnalysis(com.j_phone.io.PitchScanData scandata, Analysis analysis) {
        if (scandata.appendSample(analysis.pitch(), analysis.voice())) {
            fireEvent(com.j_phone.io.MicControlListener.PITCHSCANDATA_OVERFLOW);
        }
    }

    private Analysis analyze(byte[] buffer, int length) {
        int sampleCount = length / SAMPLE_BYTES;
        if (sampleCount < 32) {
            return new Analysis(-1, 0);
        }

        double[] samples = new double[sampleCount];
        double mean = 0.0;
        for (int i = 0; i < sampleCount; i++) {
            int low = buffer[i * 2] & 0xFF;
            int high = buffer[i * 2 + 1];
            short sample = (short) ((high << 8) | low);
            samples[i] = sample;
            mean += sample;
        }
        mean /= sampleCount;

        double energy = 0.0;
        for (int i = 0; i < sampleCount; i++) {
            samples[i] -= mean;
            energy += samples[i] * samples[i];
        }
        double rms = Math.sqrt(energy / sampleCount) / 32768.0;
        int voice = rmsToVoice(rms);
        if (voice == 0) {
            return new Analysis(-1, 0);
        }

        int pitch = estimatePitch(samples);
        return new Analysis(pitch, voice);
    }

    private int estimatePitch(double[] samples) {
        int minLag = Math.max(1, Math.round(SAMPLE_RATE / MAX_PITCH_HZ));
        int maxLag = Math.min(samples.length - 2, Math.round(SAMPLE_RATE / MIN_PITCH_HZ));
        double bestCorrelation = 0.0;
        int bestLag = -1;

        for (int lag = minLag; lag <= maxLag; lag++) {
            double correlation = 0.0;
            double energyA = 0.0;
            double energyB = 0.0;
            for (int i = lag; i < samples.length; i++) {
                double a = samples[i];
                double b = samples[i - lag];
                correlation += a * b;
                energyA += a * a;
                energyB += b * b;
            }
            if (energyA == 0.0 || energyB == 0.0) {
                continue;
            }
            double normalized = correlation / Math.sqrt(energyA * energyB);
            if (normalized > bestCorrelation) {
                bestCorrelation = normalized;
                bestLag = lag;
            }
        }

        if (bestLag <= 0 || bestCorrelation < 0.45) {
            return -1;
        }
        double frequency = SAMPLE_RATE / bestLag;
        return (int) Math.round(frequency * 16.0);
    }

    private int rmsToVoice(double rms) {
        if (rms < 0.012) {
            return 0;
        }
        double gain = 0.65 + (volume / (double) MAX_VOLUME);
        return clamp((int) Math.round(rms * 100.0 * gain), 0, 15);
    }

    private void closeLine() {
        if (line == null) {
            return;
        }
        try {
            line.stop();
        } catch (RuntimeException ignored) {
        }
        try {
            line.close();
        } catch (RuntimeException ignored) {
        }
        line = null;
    }

    private static void fireEvent(int event) {
        var current = listener;
        if (current == null) {
            return;
        }
        try {
            current.eventOccurred(event, System.currentTimeMillis());
        } catch (RuntimeException exception) {
            remexa.probes.DebugLog.log(
                    remexa.probes.LogCategory.IO,
                    MicControl.class.getName(),
                    "MicControl listener failed for event " + event + ": " + exception.getMessage()
            );
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private record Analysis(int pitch, int voice) {
    }
}
