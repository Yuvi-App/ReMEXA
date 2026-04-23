package remexa.host.runtime;

import javax.microedition.midlet.MIDlet;
import remexa.host.jad.JadDescriptor;

public record LaunchResult(
        JadDescriptor descriptor,
        String jarPath,
        String entryClass,
        ClassLoader classLoader,
        Object appInstance,
        MIDlet midlet
) {
}
