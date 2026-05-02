package remexa.host.input;

public enum InputProfile {
    GENERIC,
    JSKY,
    VODAFONE,
    MEXA;

    public boolean usesJPhoneKeyCodes() {
        return this == JSKY || this == VODAFONE || this == MEXA;
    }
}
