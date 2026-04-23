package remexa.host.runtime;

import remexa.host.profile.AppProfile;
import remexa.probes.DebugLog;
import remexa.probes.LogCategory;

public final class SystemPropertyProfile {
    private SystemPropertyProfile() {
    }

    public static void apply(AppProfile profile) {
        for (var entry : profile.systemProperties().entrySet()) {
            System.setProperty(entry.getKey(), entry.getValue());
        }
        DebugLog.log(
                LogCategory.HOST,
                SystemPropertyProfile.class.getName(),
                "Applied system property profile: " + profile.displayName()
        );
    }
}
