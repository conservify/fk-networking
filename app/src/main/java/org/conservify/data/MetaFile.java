package org.conservify.data;

import org.conservify.data.pb.PbFile;

public class MetaFile extends PbFile {
    public MetaFile(RecordListener listener, String path) {
        super(listener, path);
    }
}
