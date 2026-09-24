package remexa.audio.spatial;

import java.util.WeakHashMap;

/** App-owned scene. Its monitor only protects small control snapshots, never audio rendering. */
public final class Audio3DScene {
    public record Vector(double x, double y, double z) {
        public static final Vector ZERO = new Vector(0, 0, 0);
        public Vector plus(Vector v) { return new Vector(x + v.x, y + v.y, z + v.z); }
        public Vector minus(Vector v) { return new Vector(x - v.x, y - v.y, z - v.z); }
        public Vector times(double n) { return new Vector(x * n, y * n, z * n); }
        public double dot(Vector v) { return x * v.x + y * v.y + z * v.z; }
        public double length() { return Math.sqrt(dot(this)); }
        public Vector unit() { double n = length(); return n == 0 ? ZERO : times(1 / n); }
        public Vector cross(Vector v) { return new Vector(y * v.z - z * v.y, z * v.x - x * v.z, x * v.y - y * v.x); }
        public int[] integers() { return new int[]{(int) x, (int) y, (int) z}; }
    }

    public record Listener(Vector position, Vector velocity, Vector right, Vector up, Vector back) {
        public Vector toWorld(Vector v) { return right.times(v.x).plus(up.times(v.y)).plus(back.times(v.z)); }
        public Vector toLocal(Vector v) { return new Vector(v.dot(right), v.dot(up), v.dot(back)); }
    }

    private final WeakHashMap<Audio3DSource, Boolean> sources = new WeakHashMap<>();
    private Listener pending = new Listener(Vector.ZERO, Vector.ZERO,
            new Vector(1, 0, 0), new Vector(0, 1, 0), new Vector(0, 0, 1));
    private Listener applied = pending;
    private boolean deferred;
    private int preset = 7; // None, in the Vodafone preset order.
    private int decayMillis;
    private int outputDevice = 2;

    public synchronized Audio3DSource createSource() {
        Audio3DSource source = new Audio3DSource(this);
        sources.put(source, Boolean.TRUE);
        return source;
    }

    public synchronized void setListener(Vector position, Vector velocity, Vector front, Vector up) {
        pending = new Listener(position, velocity, front.cross(up).unit(), up.unit(), front.unit().times(-1));
        if (!deferred) applied = pending;
    }

    public synchronized void setReverb(int preset, int decayMillis) {
        this.preset = preset;
        this.decayMillis = decayMillis;
    }

    public synchronized void setOutputDevice(int device) { outputDevice = device; }

    public synchronized void setDeferred(boolean value) {
        if (deferred && !value) commit();
        deferred = value;
    }

    public synchronized void commit() {
        applied = pending;
        for (Audio3DSource source : sources.keySet()) source.commit();
    }

    boolean deferred() { return deferred; }
    Listener pendingListener() { return pending; }

    synchronized Audio3DSource.Snapshot snapshot(Audio3DSource source) {
        return new Audio3DSource.Snapshot(source.applied, applied, source.mode, source.minDistance,
                source.maxDistance, source.rolloff, source.reverbLevel, preset, decayMillis, outputDevice);
    }
}
