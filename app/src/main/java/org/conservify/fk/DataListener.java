package org.conservify.fk;

import java.util.List;

public interface DataListener {
    void onFileInfo(FileInfo info);
    void onFileRecords(List<FileRecord> records);
}
