package org.conservify.data;

public class FileInfo {
    private final String file;
    private final int size;
    private final int firstRecord;
    private final int lastRecord;

    public String getFile() {
        return file;
    }

    public int getSize() {
        return size;
    }

    public int getFirstRecord() {
        return firstRecord;
    }

    public int getLastRecord() {
        return lastRecord;
    }

    public FileInfo(String file, int size, int firstRecord, int lastRecord) {
        this.file = file;
        this.size = size;
        this.firstRecord = firstRecord;
        this.lastRecord = lastRecord;
    }
}
