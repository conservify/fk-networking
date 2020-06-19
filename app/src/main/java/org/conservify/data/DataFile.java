package org.conservify.data;

import org.conservify.data.pb.PbFile;

public class DataFile extends PbFile {
    public DataFile(FileSystemListener listener, String path) {
        super(listener, path);
    }
}
