package javax.microedition.midlet;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import javax.microedition.io.ConnectionNotFoundException;
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
        MidletRuntime.notifyDestroyed(this);
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

    public final boolean platformRequest(String url) throws ConnectionNotFoundException {
        SdkStubSupport.log(getClass().getName(), "platformRequest", url);
        if (url == null) {
            throw new NullPointerException("url");
        }
        if (url.isEmpty()) {
            return false;
        }
        try {
            if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                throw new ConnectionNotFoundException("Platform browsing is not supported: " + url);
            }
            Desktop.getDesktop().browse(new URI(url));
            return false;
        } catch (IllegalArgumentException | IOException | SecurityException | URISyntaxException exception) {
            throw new ConnectionNotFoundException("Platform cannot handle URL: " + url, exception);
        }
    }
}
