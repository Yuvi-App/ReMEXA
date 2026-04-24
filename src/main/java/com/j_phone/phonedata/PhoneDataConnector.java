package com.j_phone.phonedata;

import java.io.IOException;
import java.util.NoSuchElementException;

public class PhoneDataConnector {
    private static final AddressBook EMPTY_ADDRESS_BOOK = new EmptyAddressBook();

    public PhoneDataConnector () {
        remexa.probes.SdkStubSupport.log("com.j_phone.phonedata.PhoneDataConnector", "PhoneDataConnector");
    }


    public static com.j_phone.phonedata.PhoneData openPhoneData (java.lang.String name, int index) throws java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.j_phone.phonedata.PhoneDataConnector", "openPhoneData", name, index);
        if ("AddressBook".equalsIgnoreCase(name)) {
            return EMPTY_ADDRESS_BOOK;
        }
        return null;
    }

    public static int getElementCount (java.lang.String name, int index) {
        remexa.probes.SdkStubSupport.log("com.j_phone.phonedata.PhoneDataConnector", "getElementCount", name, index);
        if ("AddressBook".equalsIgnoreCase(name)) {
            return 0;
        }
        return 0;
    }

    public static int getRestCount (java.lang.String name, int index) throws java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.j_phone.phonedata.PhoneDataConnector", "getRestCount", name, index);
        if ("AddressBook".equalsIgnoreCase(name)) {
            return 0;
        }
        return 0;
    }

    private static final class EmptyAddressBook implements AddressBook {
        private static final DataEnumeration EMPTY_ENUMERATION = new EmptyDataEnumeration();

        @Override
        public void close() {
        }

        @Override
        public String getListType() {
            return "AddressBook";
        }

        @Override
        public DataEnumeration elements(int position, int max, int sortType) {
            return EMPTY_ENUMERATION;
        }

        @Override
        public void createElement(DataElement element) throws IOException {
            throw new IOException("AddressBook is read-only in the emulator.");
        }

        @Override
        public void delete(DataElement element) throws IOException {
            throw new IOException("AddressBook is read-only in the emulator.");
        }

        @Override
        public void importElementRawData(byte[] data) throws IOException {
            throw new IOException("AddressBook import is not supported in the emulator.");
        }

        @Override
        public byte[] exportElementRawData(DataElement exportElement) {
            return new byte[0];
        }

        @Override
        public int getListMaxCount() {
            return 0;
        }

        @Override
        public int[] getGroupNoList() {
            return new int[0];
        }

        @Override
        public String getGroupName(int groupNo) {
            return "";
        }

        @Override
        public int getPhoneNumberMaxCount() {
            return 0;
        }

        @Override
        public int getMailAddressMaxCount() {
            return 0;
        }

        @Override
        public DataEnumeration elements(int type, String searchString, int from, int max) {
            return EMPTY_ENUMERATION;
        }
    }

    private static final class EmptyDataEnumeration implements DataEnumeration {
        @Override
        public int getCount() {
            return 0;
        }

        @Override
        public boolean hasMoreElements() {
            return false;
        }

        @Override
        public Object nextElement() {
            throw new NoSuchElementException("No phone data elements available.");
        }
    }
}
