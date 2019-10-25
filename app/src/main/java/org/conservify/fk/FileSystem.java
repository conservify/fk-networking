package org.conservify.fk;

import android.content.Context;

public class FileSystem {
    private final Context context;

    public FileSystem(Context context) {
        this.context = context;
    }

    public DataFile openData() {
        return null;
    }

    public MetaFile openMeta() {
        return null;
    }
}
