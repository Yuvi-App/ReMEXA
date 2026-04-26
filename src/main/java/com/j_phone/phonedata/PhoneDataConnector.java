package com.j_phone.phonedata;

import java.io.IOException;
import java.util.NoSuchElementException;

public class PhoneDataConnector {
    private static final DataEnumeration EMPTY_ENUMERATION = new EmptyDataEnumeration();
    private static final AddressBook EMPTY_ADDRESS_BOOK = new EmptyAddressBook();
    private static final ReceivedMailBox EMPTY_RECEIVED_MAIL_BOX = new EmptyReceivedMailBox();
    private static final SentMailBox EMPTY_SENT_MAIL_BOX = new EmptySentMailBox();

    public PhoneDataConnector () {
        remexa.probes.SdkStubSupport.log("com.j_phone.phonedata.PhoneDataConnector", "PhoneDataConnector");
    }

    public static com.j_phone.phonedata.PhoneData openPhoneData (java.lang.String name, int index) throws java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.j_phone.phonedata.PhoneDataConnector", "openPhoneData", name, index);
        if (name == null) {
            throw new NullPointerException("PhoneDataConnector.openPhoneData: name is null");
        }
        if ("AddressBook".equalsIgnoreCase(name)) {
            return EMPTY_ADDRESS_BOOK;
        }
        if ("ReceivedMailBox".equalsIgnoreCase(name)) {
            return EMPTY_RECEIVED_MAIL_BOX;
        }
        if ("SentMailBox".equalsIgnoreCase(name)) {
            return EMPTY_SENT_MAIL_BOX;
        }
        throw new IOException("PhoneDataConnector.openPhoneData: unknown phone data type: " + name);
    }

    public static int getElementCount (java.lang.String name, int index) {
        remexa.probes.SdkStubSupport.log("com.j_phone.phonedata.PhoneDataConnector", "getElementCount", name, index);
        return 0;
    }

    public static int getRestCount (java.lang.String name, int index) throws java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.j_phone.phonedata.PhoneDataConnector", "getRestCount", name, index);
        return 0;
    }

    static AddressData newEmptyAddressData() {
        return new EmptyAddressData();
    }

    static MailData newEmptyMailData() {
        return new EmptyMailData();
    }

    private static abstract class EmptyPhoneData implements PhoneData {
        private final String listType;

        EmptyPhoneData(String listType) {
            this.listType = listType;
        }

        @Override
        public final void close() {
        }

        @Override
        public final String getListType() {
            return listType;
        }

        @Override
        public final DataEnumeration elements(int position, int max, int sortType) {
            return EMPTY_ENUMERATION;
        }

        @Override
        public void createElement(DataElement element) throws IOException {
            throw new IOException(listType + " is read-only in the emulator.");
        }

        @Override
        public void delete(DataElement element) throws IOException {
            throw new IOException(listType + " is read-only in the emulator.");
        }

        @Override
        public void importElementRawData(byte[] data) throws IOException {
            throw new IOException(listType + " import is not supported in the emulator.");
        }

        @Override
        public byte[] exportElementRawData(DataElement exportElement) {
            return new byte[0];
        }

        @Override
        public int getListMaxCount() {
            return 0;
        }
    }

    private static final class EmptyAddressBook extends EmptyPhoneData implements AddressBook {
        EmptyAddressBook() {
            super("AddressBook");
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

    private static final class EmptyReceivedMailBox extends EmptyPhoneData implements ReceivedMailBox {
        EmptyReceivedMailBox() {
            super("ReceivedMailBox");
        }

        @Override
        public int getUnReadMailCount() {
            return 0;
        }
    }

    private static final class EmptySentMailBox extends EmptyPhoneData implements SentMailBox {
        EmptySentMailBox() {
            super("SentMailBox");
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

    private static abstract class EmptyDataElement implements DataElement {
        private final String typeName;

        EmptyDataElement(String typeName) {
            this.typeName = typeName;
        }

        @Override
        public final String getType() {
            return typeName;
        }

        @Override
        public int getElementCount(int id) {
            return 0;
        }

        @Override
        public int getDataType(int id) {
            return 0;
        }

        @Override
        public String getString(int id, int index) {
            return null;
        }

        @Override
        public Integer getInt(int id, int index) {
            return null;
        }

        @Override
        public java.util.Date getDate(int id, int index) {
            return null;
        }

        @Override
        public Boolean getBoolean(int id, int index) {
            return null;
        }

        @Override
        public void setString(int id, int index, String value) {
        }

        @Override
        public void setInt(int id, int index, Integer value) {
        }

        @Override
        public void setBoolean(int id, int index, Boolean value) {
        }

        @Override
        public boolean isListElement() {
            return false;
        }
    }

    private static final class EmptyAddressData extends EmptyDataElement implements AddressData {
        EmptyAddressData() {
            super("AddressData");
        }

        @Override
        public DataElement createClone() {
            return new EmptyAddressData();
        }
    }

    private static final class EmptyMailData extends EmptyDataElement implements MailData {
        EmptyMailData() {
            super("MailData");
        }

        @Override
        public DataElement createClone() {
            return new EmptyMailData();
        }

        @Override
        public boolean isUnRead() {
            return false;
        }

        @Override
        public boolean hasRemainder() {
            return false;
        }

        @Override
        public int hasSendState() {
            return SEND_STATE_NO_MESSAGE;
        }

        @Override
        public int getAttachedFileCount() {
            return 0;
        }

        @Override
        public String getAttachedFileName(int index) {
            return null;
        }

        @Override
        public byte[] getAttachedFileData(int index) {
            return new byte[0];
        }

        @Override
        public void setState(int state) {
        }

        @Override
        public int setAttachedFile(String pathname) throws IOException {
            throw new IOException("MailData attachments are not supported in the emulator.");
        }

        @Override
        public int setAttachedData(byte[] data, String attachedFileName, int fileType) throws IOException {
            throw new IOException("MailData attachments are not supported in the emulator.");
        }

        @Override
        public void removeAttachedFile(int index) {
        }

        @Override
        public void setConfirm(int confirm) {
        }

        @Override
        public void setPriority(int priority) {
        }
    }
}
