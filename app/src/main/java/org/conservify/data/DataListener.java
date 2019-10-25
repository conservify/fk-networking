package org.conservify.data;

import java.util.List;

public interface DataListener {
    void onFileInfo(String path, FileInfo info);
    void onFileRecords(String path, List<FileRecord> records);
    void onFileAnalysis(String path, FileAnalysis analysis);
}
