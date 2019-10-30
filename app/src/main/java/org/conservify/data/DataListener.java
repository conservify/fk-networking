package org.conservify.data;

import java.util.List;

public interface DataListener {
    void onFileInfo(String path, String task, FileInfo info);
    void onFileRecords(String path, String task, List<FileRecord> records);
    void onFileAnalysis(String path, String task, FileAnalysis analysis);
    void onFileError(String path, String task, String message);
}
