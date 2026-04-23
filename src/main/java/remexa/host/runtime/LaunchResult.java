package remexa.host.runtime;

import javax.microedition.midlet.MIDlet;
import remexa.host.jad.JadDescriptor;
import remexa.host.profile.LaunchProfile;

public record LaunchResult(
        JadDescriptor descriptor,
        LaunchProfile launchProfile,
        String jarPath,
        String entryClass,
        ClassLoader classLoader,
        Object appInstance,
        MIDlet midlet
) {
}
