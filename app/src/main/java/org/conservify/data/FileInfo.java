package org.conservify.data;

public class FileInfo {
    private final String file;
    private final long size;

    public String getFile() {
        return file;
    }

    public long getSize() {
        return size;
    }

    public FileInfo(String file, long size) {
        this.file = file;
        this.size = size;
    }
}
