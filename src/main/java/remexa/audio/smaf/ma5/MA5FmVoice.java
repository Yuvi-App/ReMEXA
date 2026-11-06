package remexa.audio.smaf.ma5;

public final class MA5FmVoice {
    private final MA5VoiceProgram program;
    private final Operator[] operators;
    private final int sampleRate;
    private final int lfoStep;
    private int modulation;
    private int lfoPhase;
    private int lfoIndex;
    private long frame;
    private long serviceTick;
    private long serviceEndFrame;
    private long nextLfoFrame;
    private boolean controlsChanged = true;

    public MA5FmVoice(MA5VoiceProgram program, int sampleRate) {
        if (!supports(program) || sampleRate < 1000) throw new IllegalArgumentException("Unsupported MA5 FM voice/rate");
        this.program = program;
        this.sampleRate = sampleRate;
        this.lfoStep = MA5FmTables.lfoStep(sampleRate, program.lfo());
        MA5VoiceProgram.Operator[] definitions = program.operators();
        operators = new Operator[definitions.length];
        for (int i = 0; i < definitions.length; i++) operators[i] = new Operator(definitions[i]);
    }

    /** Wave-RAM oscillator shapes 15/23/31 retain the existing fallback path. */
    public static boolean supports(MA5VoiceProgram program) {
        for (MA5VoiceProgram.Operator op : program.operators()) if (!MA5FmTables.supported(op.waveShape())) return false;
        return true;
    }

    public void setFrequency(double frequency) {
        double register = Math.max(0, frequency) * 1048576.0 / 48000.0;
        int block = Math.clamp(Math.getExponent(register) - 9, 0, 7);
        int fnum = Math.clamp((int)(register / (1 << block)), 0, 1023);
        setFrequencyRegister(Math.clamp(block + 1 - program.basicOctave(), 0, 7), fnum);
    }

    // A native register boundary, also used by the isolated DLL vector tests.
    void setFrequencyRegister(int block, int fnum) {
        int base = (int)(((long)fnum << block) * 0xbb80000L / sampleRate);
        int keycode = (block << 1) | (fnum >> 9);
        for (Operator op : operators) op.frequency(base, keycode, block, fnum);
        controlsChanged = true;
    }

    /** Native 7-bit modulation selects off, then four discrete vibrato levels. */
    public void setModulation(int value) {
        int clamped = Math.clamp(value, 0, 127);
        modulation = clamped == 0 ? 0 : (clamped >> 5) + 1;
        controlsChanged = true;
    }

    public void keyOff() {
        for (Operator op : operators) if (op.stage != 0 && !op.definition.ignoreKeyOff()) op.stage = 1;
    }

    public boolean isFinished() {
        int carriers = MA5FmTables.CARRIERS[program.algorithm()];
        for (int i = 0; i < operators.length; i++) if ((carriers & (1 << i)) != 0 && operators[i].stage != 0) return false;
        return true;
    }

    public int nextSample() {
        if (frame >= nextLfoFrame) {
            lfoIndex = lfoPhase >>> 20;
            if (frame >= serviceEndFrame) serviceEndFrame = ++serviceTick * sampleRate / 1000;
            // Native 0x1000ead0 subdivides each driver service block into
            // at most 32 samples, refreshing AM/PM before every subdivision.
            nextLfoFrame = Math.min(serviceEndFrame, frame + 32);
            lfoPhase += lfoStep * (int)(nextLfoFrame - frame);
            controlsChanged = true;
        }
        if (controlsChanged) {
            for (Operator op : operators) op.modulate(lfoIndex);
            controlsChanged = false;
        }
        int a, b, c, result;
        switch (program.algorithm()) {
            case 0 -> { a = operators[0].sample(0, true); result = operators[1].sample(a, false); }
            case 1 -> { a = operators[0].sample(0, true); result = a + operators[1].sample(0, false); }
            case 2 -> {
                a = operators[0].sample(0, true); b = operators[1].sample(0, false);
                c = operators[2].sample(0, true); result = a + b + c + operators[3].sample(0, false);
            }
            case 3 -> {
                a = operators[0].sample(0, true); b = operators[1].sample(0, false);
                c = operators[2].sample(b, false); result = operators[3].sample(a + c, false);
            }
            case 4 -> {
                a = operators[0].sample(0, true); b = operators[1].sample(a, false);
                c = operators[2].sample(b, false); result = operators[3].sample(c, false);
            }
            case 5 -> {
                a = operators[0].sample(0, true); b = operators[1].sample(a, false);
                c = operators[2].sample(0, true); result = b + operators[3].sample(c, false);
            }
            case 6 -> {
                a = operators[0].sample(0, true); b = operators[1].sample(0, false);
                c = operators[2].sample(b, false); result = a + operators[3].sample(c, false);
            }
            case 7 -> {
                a = operators[0].sample(0, true); b = operators[1].sample(0, false);
                c = operators[2].sample(b, false); result = a + c + operators[3].sample(0, false);
            }
            default -> throw new IllegalStateException("Invalid FM algorithm");
        }
        frame++;
        return result;
    }

    private final class Operator {
        private final MA5VoiceProgram.Operator definition;
        private final short[] wave;
        private final int totalLevel;
        private final long sustainLevel;
        private final int feedbackShift;
        private int stage = 2;
        private long level;
        private long attack;
        private long decay;
        private long sustain;
        private long release;
        private int phase;
        private int baseAdvance;
        private int advance;
        private int scaledLevel;
        private int gain;
        private int previous;
        private int previous2;

        private Operator(MA5VoiceProgram.Operator definition) {
            this.definition = definition;
            this.wave = MA5FmTables.WAVES[definition.waveShape()];
            this.totalLevel = MA5FmTables.totalLevel(definition.totalLevel());
            this.sustainLevel = MA5FmTables.sustainLevel(definition.sustainLevel());
            this.feedbackShift = MA5FmTables.FEEDBACK_SHIFT[definition.feedback()];
        }

        private void frequency(int base, int keycode, int block, int fnum) {
            baseAdvance = MA5FmTables.phaseAdvance(base, definition.multi(), definition.detune(), block, fnum, sampleRate);
            int offset = definition.keyScaleRate() ? keycode : keycode >> 2;
            attack = MA5FmTables.attack(rateIndex(definition.attackRate(), offset), sampleRate);
            decay = MA5FmTables.decay(rateIndex(definition.decayRate(), offset), sampleRate);
            sustain = MA5FmTables.decay(rateIndex(definition.sustainRate(), offset), sampleRate);
            release = MA5FmTables.decay(rateIndex(definition.releaseRate(), offset), sampleRate);
            scaledLevel = totalLevel * MA5FmTables.keyScale(definition.keyScaleLevel(), block, fnum) >> 15;
        }

        private void modulate(int index) {
            advance = baseAdvance;
            if (definition.vibratoEnabled() && modulation != 0) {
                int depth = Math.min(3, definition.vibratoDepth() + modulation - 1);
                advance = (int)((Integer.toUnsignedLong(baseAdvance) * MA5FmTables.PM[depth][index]) >> 20);
            }
            gain = definition.amplitudeModEnabled()
                    ? scaledLevel * MA5FmTables.AM[definition.amplitudeModDepth()][index] >> 15 : scaledLevel;
        }

        private int sample(int input, boolean feedback) {
            if (stage == 0) return 0;
            envelope();
            if (feedback && feedbackShift != 0) input += (previous + previous2) >> feedbackShift;
            int index = (((phase >>> 20) + ((input << 14) >> 15)) >> 2) & 1023;
            int envelopeGain = (int)(level >> 16) * gain >> 15;
            int sample = wave[index] * envelopeGain >> 15;
            phase += advance;
            previous2 = previous;
            previous = sample;
            return sample;
        }

        private void envelope() {
            switch (stage) {
                case 1 -> { level = level * release >> 30; if (level == 0) stage = 0; }
                case 2 -> { phase = 0; stage = 3; }
                case 3 -> { level += attack; if (level >= 0x80000000L) { level = 0x80000000L; stage = 4; } }
                case 4 -> { level = level * decay >> 30; if (level == 0) stage = 0; else if (level <= sustainLevel) stage = 5; }
                case 5 -> { level = level * sustain >> 30; if (level == 0) stage = 0; }
                default -> { }
            }
        }
    }

    private static int rateIndex(int rate, int offset) { return rate == 0 ? 0 : Math.min(63, (rate << 2) + offset); }
}
