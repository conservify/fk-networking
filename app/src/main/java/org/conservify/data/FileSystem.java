package org.conservify.data;

import android.content.Context;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicLong;

public class FileSystem {
    private static final String TAG = "JS";

    private final AtomicLong tokens = new AtomicLong();
    private final Context context;
    private final FileSystemListener listener;

    public FileSystem(Context context, FileSystemListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public FileSystem(FileSystemListener listener) {
        this.context = null;
        this.listener = listener;
    }

    public PbFile open(String path) {
        return new PbFile(listener, path);
    }

    public String newToken() {
        return "cfyfs-" + tokens.incrementAndGet();
    }

    public boolean copyFile(String source, String destiny) {
        try {
            InputStream is = new FileInputStream(source);
            try {
                OutputStream os = new FileOutputStream(destiny);
                try {
                    byte[] buf = new byte[32768];
                    int len;
                    while ((len = is.read(buf)) > 0) {
                        os.write(buf, 0, len);
                    }
                } finally {
                    os.close();
                }
            } finally {
                is.close();
            }
            return true;
        }
        catch (IOException e) {
            Log.e(TAG, "error", e);
            return false;
        }
    }
}
