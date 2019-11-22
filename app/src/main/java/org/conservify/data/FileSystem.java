package org.conservify.data;

import android.content.Context;

public class FileSystem {
    private final Context context;
    private final DataListener listener;

    public FileSystem(Context context, DataListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public DataFile openData(String path) {
        return new DataFile(listener, path);
    }

    public MetaFile openMeta(String path) {
        return new MetaFile(listener, path);
    }
}
