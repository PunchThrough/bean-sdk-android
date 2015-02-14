package com.punchthrough.bean.sdk.internal.intelhex;

public class Line {

    private int byteCount;
    private int address;
    private LineRecordType recordType;
    private byte[] data;
    private int checksum;

    public int getByteCount() {
        return byteCount;
    }

    public void setByteCount(int byteCount) {
        this.byteCount = byteCount;
    }

    public int getAddress() {
        return address;
    }

    public void setAddress(int address) {
        this.address = address;
    }

    public LineRecordType getRecordType() {
        return recordType;
    }

    public void setRecordType(LineRecordType recordType) {
        this.recordType = recordType;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public int getChecksum() {
        return checksum;
    }

    public void setChecksum(int checksum) {
        this.checksum = checksum;
    }

}
