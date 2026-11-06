package remexa.audio.smaf.ma5;

public final class MA5OutputStage {
    private final int sampleRate;
    private final int[] direct = new int[64], cross = new int[64];
    private final int[] historyLeft = new int[64], historyRight = new int[64];
    private int cursor;
    private boolean bypass = true;
    private int filterShift, bypassShift;
    private int attenuation, fadeStep;
    private int left, right;

    public MA5OutputStage(int sampleRate) { this.sampleRate=sampleRate; }

    public void reset() {
        java.util.Arrays.fill(historyLeft,0); java.util.Arrays.fill(historyRight,0);
        cursor=0; attenuation=fadeStep=0;
    }

    public void setKernel(short[] directKernel, short[] crossKernel) {
        resample(directKernel,direct); resample(crossKernel,cross); bypass=false;
    }

    public void setBypass(boolean value, int filteredShift, int unfilteredShift) {
        bypass=value; filterShift=filteredShift & 3; bypassShift=unfilteredShift & 3;
    }

    public void setFade(int indexQ14, int increment) { attenuation=Math.clamp(indexQ14,0,192<<14); fadeStep=increment; }

    public void process(int mixedLeft, int mixedRight) {
        int l=Math.clamp(mixedLeft,-32767,32767), r=Math.clamp(mixedRight,-32767,32767);
        historyLeft[cursor]=l; historyRight[cursor]=r;
        if (bypass) { l >>= bypassShift; r >>= bypassShift; }
        else {
            int sumL=0,sumR=0;
            for (int i=0;i<64;i++) {
                int index=(cursor+i)&63;
                sumL += historyLeft[index]*direct[i] >> 15;
                sumL += historyRight[index]*cross[i] >> 15;
                sumR += historyRight[index]*direct[i] >> 15;
                sumR += historyLeft[index]*cross[i] >> 15;
            }
            l=Math.clamp(sumL,-32768,32767) >> filterShift;
            r=Math.clamp(sumR,-32768,32767) >> filterShift;
        }
        cursor=(cursor-1)&63;
        int gain=MA5MixTables.ATTENUATION[attenuation >>> 14];
        left=l*gain >> 15; right=r*gain >> 15;
        attenuation=Math.clamp(attenuation+fadeStep,0,192<<14);
        if (attenuation == 0 && fadeStep < 0) fadeStep=0;
    }

    public int left() { return left; }
    public int right() { return right; }

    private void resample(short[] source, int[] output) {
        if (source.length != 64) throw new IllegalArgumentException("Expected 64 FIR taps");
        output[0]=source[0];
        double step=48000.0/sampleRate, position=step;
        for (int i=1;i<64;i++,position+=step) {
            if (sampleRate == 48000) { output[i]=source[i]; continue; }
            int index=(int)position;
            output[i]=index+1>=64 ? 0 : Math.clamp((int)(source[index]+(position-index)*(source[index+1]-source[index])+.5),-32768,32767);
        }
    }
}
