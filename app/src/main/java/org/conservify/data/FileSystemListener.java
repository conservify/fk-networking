package org.conservify.data;

import java.util.List;

public interface FileSystemListener {
    void onFileInfo(String path, String task, FileInfo info);
    void onFileRecords(String path, String task, long position, long size, List<Object> records);
    void onFileError(String path, String task, String message);
}
