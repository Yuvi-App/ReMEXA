package com.j_phone.phonedata;

public class DataElementFactory {
    public static com.j_phone.phonedata.MailData createMailData () {
        remexa.probes.SdkStubSupport.log("com.j_phone.phonedata.DataElementFactory", "createMailData");
        return PhoneDataConnector.newEmptyMailData();
    }

    public static com.j_phone.phonedata.AddressData createAddressData () {
        remexa.probes.SdkStubSupport.log("com.j_phone.phonedata.DataElementFactory", "createAddressData");
        return PhoneDataConnector.newEmptyAddressData();
    }
}
