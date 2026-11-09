package remexa.audio.smaf.ma5;

/**
 * MA5 PCM generator before the output filter, pan and mix gain.
 */
public final class MA5PcmVoice {
    private final MA5PcmVoiceProgram program;
    private final int[] wave;
    private final int end;
    private final int loop;
    private final int sampleRate;
    private final int lfoStep;
    private final int totalLevel;
    private final long sustainLevel;
    private int stage = 2;
    private long level;
    private long attack, decay, sustain, release;
    private long position;
    private int baseAdvance, advance, gain;
    private boolean held;
    private int heldOutput;
    private int modulation;
    private boolean pedal;
    private int lfoPhase, lfoIndex;
    private long frame, serviceTick, serviceEndFrame, nextLfoFrame;
    private boolean controlsChanged = true;

    public MA5PcmVoice(MA5PcmVoiceProgram program, int[] wave, int sampleRate) {
        if (wave.length == 0 || program.endPoint() < 0 || sampleRate < 1000)
            throw new IllegalArgumentException("Invalid MA5 PCM wave/rate");
        this.program = program;
        this.wave = wave;
        this.sampleRate = sampleRate;
        end = Math.max(1, Math.min(program.endPoint(), wave.length));
        loop = Math.clamp(program.loopPoint(), 0, end);
        lfoStep = MA5FmTables.lfoStep(sampleRate, program.lfo());
        totalLevel = MA5FmTables.totalLevel(program.totalLevel());
        sustainLevel = MA5FmTables.sustainLevel(program.sustainLevel());
    }

    public void setFrequencyRegister(int register) {
        long product = Integer.toUnsignedLong((int)(0x5dc00000L / sampleRate * Integer.toUnsignedLong(register)));
        baseAdvance = (int)(product >> 15);
        int block = Math.clamp(Math.getExponent((double)Integer.toUnsignedLong(register)) - 11, 0, 7);
        int fnum = (int)(Integer.toUnsignedLong(register) >> (block + 1)) - 1024;
        int offset = ((block << 1) | ((fnum >> 9) & 1)) >> 2;
        attack = MA5FmTables.attack(rateIndex(program.attackRate(), offset), sampleRate);
        decay = MA5FmTables.decay(rateIndex(program.decayRate(), offset), sampleRate);
        sustain = MA5FmTables.decay(rateIndex(program.sustainRate(), offset), sampleRate);
        release = MA5FmTables.decay(rateIndex(program.releaseRate(), offset), sampleRate);
        controlsChanged = true;
    }

    public void setModulation(int value) {
        int clamped = Math.clamp(value, 0, 127);
        modulation = clamped == 0 ? 0 : (clamped >> 5) + 1;
        controlsChanged = true;
    }

    public void hold(boolean value) { pedal = value && program.sustain(); }

    public void keyOff() { if (stage != 0 && !program.ignoreKeyOff()) stage = 1; }

    public void keyOn() { stage = 2; held = false; }
    public void keyOn(boolean force) { if (force) level = 0; keyOn(); }
    public void stop() { stage = 0; level = 0; }

    public void reuseSlot(MA5PcmVoice previous) {
        level = previous.level;
        lfoPhase = previous.lfoPhase;
        lfoIndex = previous.lfoIndex;
        frame = previous.frame;
        serviceTick = previous.serviceTick;
        serviceEndFrame = previous.serviceEndFrame;
        nextLfoFrame = previous.nextLfoFrame;
    }

    public boolean isFinished() { return stage == 0; }

    public int nextSample() {
        if (frame >= nextLfoFrame) {
            lfoIndex = lfoPhase >>> 20;
            if (frame >= serviceEndFrame) serviceEndFrame = ++serviceTick * sampleRate / 1000;
            nextLfoFrame = Math.min(serviceEndFrame, frame + 32);
            lfoPhase += lfoStep * (int)(nextLfoFrame - frame);
            controlsChanged = true;
        }
        if (controlsChanged) {
            advance = baseAdvance;
            if (program.vibratoEnabled() && modulation != 0) {
                int depth = Math.min(3, program.vibratoDepth() + modulation - 1);
                advance = (int)(Integer.toUnsignedLong(baseAdvance) * MA5FmTables.PM[depth][lfoIndex] >> 20);
            }
            gain = program.amplitudeModEnabled()
                    ? totalLevel * MA5FmTables.AM[program.amplitudeModDepth()][lfoIndex] >> 15 : totalLevel;
            controlsChanged = false;
        }
        frame++;
        if (stage == 0) return 0;
        switch (stage) {
            case 1 -> { level = level * (pedal ? sustain : release) >> 30; if (level == 0) stage = 0; }
            case 2 -> { position = 0; stage = 3; }
            case 3 -> { level = (level + attack) & 0xffffffffL; if (level >= 0x80000000L) { level = 0x80000000L; stage = 4; } }
            case 4 -> { level = level * decay >> 30; if (level == 0) stage = 0; else if (level <= sustainLevel) stage = 5; }
            case 5 -> { level = level * sustain >> 30; if (level == 0) stage = 0; }
            default -> { }
        }
        int envelopeGain = (int)(level >> 16) * gain >> 15;
        if (held) return envelopeGain * heldOutput >> 15;
        int source = (int)Math.min(position >>> 16, end - 1);
        int next = source + 1;
        if (next >= end) next = loop < end ? loop : source;
        int sample = wave[source] + (int)(((long)wave[next] - wave[source]) * (position & 0xffff) >> 16);
        int result = sample * envelopeGain >> 15;
        position += Integer.toUnsignedLong(advance);
        long endQ16 = (long)end << 16;
        if (position >= endQ16) {
            if (loop >= end) held = true;
            else {
                position += ((long)loop << 16) - endQ16;
                position %= endQ16;
            }
        }
        heldOutput = result;
        return result;
    }

    private static int rateIndex(int rate, int offset) { return rate == 0 ? 0 : Math.min(63, (rate << 2) + offset); }
}
