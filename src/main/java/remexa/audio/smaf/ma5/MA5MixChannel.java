package remexa.audio.smaf.ma5;

public final class MA5MixChannel {
    private final int step;
    private final Control volume = new Control();
    private final Control left = new Control();
    private final Control right = new Control();
    private int volumeLevel, leftLevel, rightLevel;

    public MA5MixChannel(int sampleRate) {
        step = switch (sampleRate) { case 22050 -> 0x22d47; case 44100 -> 0x116a3; case 48000 -> 0x10000; default -> 0x18000; };
        reset(100, 127, 64);
    }

    public void reset(int value, int expression, int pan) {
        volume.set(MA5MixTables.channelIndex(value, expression), false);
        left.set(MA5MixTables.panIndex(pan, false), false);
        right.set(MA5MixTables.panIndex(pan, true), false);
    }

    public void volume(int value, int expression) { volume.set(MA5MixTables.channelIndex(value, expression), true); }
    public void pan(int value) { left.set(MA5MixTables.panIndex(value, false), true); right.set(MA5MixTables.panIndex(value, true), true); }

    /** The hardware samples these buffers at each 32-sample/1-ms subdivision. */
    public void latch() { volumeLevel=volume.sample(); leftLevel=left.sample(); rightLevel=right.sample(); }
    public void advance(int frames) { volume.advance(frames); left.advance(frames); right.advance(frames); }
    public int volumeLevel() { return volumeLevel; }
    public int panLevel(boolean rightChannel) { return rightChannel ? rightLevel : leftLevel; }

    private final class Control {
        private int current, target;
        private void set(int value, boolean interpolate) { target=value; if (!interpolate) current=value; }
        private int sample() {
            int previous=current;
            advance(1);
            return MA5MixTables.LEVEL[(current == target ? current : previous) >>> 16];
        }
        private void advance(int frames) {
            long delta=(long)step*frames;
            if (current < target) current=(int)Math.min(target,current+delta);
            else if (current > target) current=(int)Math.max(target,current-delta);
        }
    }
}
