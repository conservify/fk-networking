package org.conservify.data;

public class FileRecord {
    private final long record;
    private final byte[] data;

    public long getRecord() {
        return record;
    }

    public byte[] getData() {
        return data;
    }

    public FileRecord(byte[] data) {
        this.record = 0;
        this.data = data;
    }

    public FileRecord(long record, byte[] data) {
        this.record = record;
        this.data = data;
    }
}
