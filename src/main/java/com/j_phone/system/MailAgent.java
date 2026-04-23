package com.j_phone.system;

public class MailAgent {
    public static com.j_phone.system.MailAgent getInstance () {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.MailAgent", "getInstance");
        return null;
    }

    public void setMailTransportListener (com.j_phone.system.MailTransportListener listener) {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.MailAgent", "setMailTransportListener", listener);
    }

    public void send (com.j_phone.phonedata.MailData data) throws java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.MailAgent", "send", data);
    }

    public void receiveRemainder (com.j_phone.phonedata.MailData data) throws java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.MailAgent", "receiveRemainder", data);
    }

    public int checkMailSize (com.j_phone.phonedata.MailData data) throws java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.MailAgent", "checkMailSize", data);
        return 0;
    }
}
