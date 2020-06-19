package org.conservify.data;

import android.content.Context;

import org.conservify.data.pb.PbFile;

import java.util.concurrent.atomic.AtomicLong;

public class FileSystem {
    private final AtomicLong tokens = new AtomicLong();
    private final Context context;
    private final RecordListener listener;

    public FileSystem(Context context, RecordListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public FileSystem(RecordListener listener) {
        this.context = null;
        this.listener = listener;
    }

    public PbFile open(String path) {
        return new PbFile(listener, path);
    }

    public String newToken() {
        return "cfyfs-" + tokens.incrementAndGet();
    }
}
