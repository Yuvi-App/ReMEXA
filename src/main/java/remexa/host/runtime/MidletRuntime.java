package remexa.host.runtime;

import java.util.Map;
import java.util.WeakHashMap;
import javax.microedition.lcdui.Display;
import javax.microedition.midlet.MIDlet;
import remexa.host.jad.JadDescriptor;
import remexa.probes.DebugLog;
import remexa.probes.LogCategory;

public final class MidletRuntime {
    private static final ThreadLocal<JadDescriptor> CURRENT_JAD = new ThreadLocal<>();
    private static final Map<MIDlet, JadDescriptor> DESCRIPTORS = new WeakHashMap<>();
    private static final Map<MIDlet, Display> DISPLAYS = new WeakHashMap<>();

    private MidletRuntime() {
    }

    public static void beginInstantiation(JadDescriptor descriptor) {
        CURRENT_JAD.set(descriptor);
    }

    public static void endInstantiation() {
        CURRENT_JAD.remove();
    }

    public static void attach(MIDlet midlet) {
        var descriptor = CURRENT_JAD.get();
        if (descriptor != null) {
            DESCRIPTORS.put(midlet, descriptor);
            DebugLog.log(LogCategory.MIDLET, MidletRuntime.class.getName(), "Attached MIDlet to " + descriptor.title());
        }
    }

    public static String getAppProperty(MIDlet midlet, String key) {
        var descriptor = DESCRIPTORS.get(midlet);
        if (descriptor == null) {
            return null;
        }
        return descriptor.properties().get(key);
    }

    public static Display getDisplay(MIDlet midlet) {
        return DISPLAYS.computeIfAbsent(midlet, ignored -> new Display(midlet));
    }
}
