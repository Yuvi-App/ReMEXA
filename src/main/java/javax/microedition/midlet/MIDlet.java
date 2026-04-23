package javax.microedition.midlet;

import remexa.host.runtime.MidletRuntime;
import remexa.probes.SdkStubSupport;

public abstract class MIDlet {
    protected MIDlet() {
        MidletRuntime.attach(this);
        SdkStubSupport.log(getClass().getName(), "<init>");
    }

    protected abstract void startApp() throws MIDletStateChangeException;

    protected abstract void pauseApp();

    protected abstract void destroyApp(boolean unconditional) throws MIDletStateChangeException;

    public final void notifyDestroyed() {
        SdkStubSupport.log(getClass().getName(), "notifyDestroyed");
    }

    public final void notifyPaused() {
        SdkStubSupport.log(getClass().getName(), "notifyPaused");
    }

    public final void resumeRequest() {
        SdkStubSupport.log(getClass().getName(), "resumeRequest");
    }

    public String getAppProperty(String key) {
        SdkStubSupport.log(getClass().getName(), "getAppProperty", key);
        return MidletRuntime.getAppProperty(this, key);
    }
}
