package org.conservify.data;

public class FileAnalysis {
    private final String path;
    private long records;
    private long size;

    public String getPath() {
        return path;
    }

    public long getRecords() {
        return records;
    }

    public long getSize() {
        return size;
    }

    public FileAnalysis(String path, long records, long size) {
        this.path = path;
        this.records = records;
        this.size = size;
    }
}
