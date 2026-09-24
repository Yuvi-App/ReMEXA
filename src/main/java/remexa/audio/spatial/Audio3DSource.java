package remexa.audio.spatial;

import remexa.audio.spatial.Audio3DScene.Vector;

/** Control state shared by MMAPI and the PCM/Yamaha renderers. No player or class-loader references. */
public final class Audio3DSource {
    public record Placement(Vector position, Vector velocity, boolean relative) { }
    public record Snapshot(Placement placement, Audio3DScene.Listener listener, int mode,
                           int minDistance, int maxDistance, int rolloff, int reverbLevel,
                           int preset, int decayMillis, int outputDevice) { }

    private final Audio3DScene scene;
    private Placement pending = new Placement(Vector.ZERO, Vector.ZERO, true);
    Placement applied = pending;
    int mode;
    int minDistance = 100;
    int maxDistance = 100000;
    int rolloff = 100;
    int reverbLevel;

    Audio3DSource(Audio3DScene scene) { this.scene = scene; }
    public Snapshot snapshot() { return scene.snapshot(this); }

    public void setMode(int value) { synchronized (scene) { mode = value; } }
    public int setReverbLevel(int value) {
        synchronized (scene) { return reverbLevel = Math.max(0, Math.min(100, value)); }
    }
    public int getReverbLevel() { synchronized (scene) { return reverbLevel; } }
    public int[] getPosition() { synchronized (scene) { return pending.position.integers(); } }
    public int[] getVelocity() { synchronized (scene) { return pending.velocity.integers(); } }
    public boolean isRelative() { synchronized (scene) { return pending.relative; } }
    public int[] getRolloff() { synchronized (scene) { return new int[]{minDistance, maxDistance, rolloff}; } }

    public void setPosition(int x, int y, int z) {
        synchronized (scene) {
            pending = new Placement(new Vector(x, y, z), pending.velocity, pending.relative);
            changed();
        }
    }
    public void setVelocity(int x, int y, int z) {
        synchronized (scene) {
            pending = new Placement(pending.position, new Vector(x, y, z), pending.relative);
            changed();
        }
    }
    public void setRelative(boolean relative) {
        synchronized (scene) {
            if (pending.relative == relative) return;
            var listener = scene.pendingListener();
            Vector position = relative ? listener.toLocal(pending.position.minus(listener.position()))
                    : listener.toWorld(pending.position).plus(listener.position());
            Vector velocity = relative ? listener.toLocal(pending.velocity.minus(listener.velocity()))
                    : listener.toWorld(pending.velocity).plus(listener.velocity());
            // The native API stores integer coordinates after converting between the two frames.
            pending = new Placement(integerVector(position), integerVector(velocity), relative);
            changed();
        }
    }
    public void setRolloff(int min, int max, int factor) {
        if (min <= 0 || max < min || factor < 0) throw new IllegalArgumentException("Invalid distance rolloff");
        synchronized (scene) { minDistance = min; maxDistance = max; rolloff = factor; }
    }
    private static Vector integerVector(Vector v) { return new Vector((int) v.x(), (int) v.y(), (int) v.z()); }
    private void changed() { if (!scene.deferred()) commit(); }
    void commit() { applied = pending; }
}
