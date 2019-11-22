package org.conservify.data.pb;

import android.util.Log;

import com.google.protobuf.CodedInputStream;

import org.conservify.data.DataListener;
import org.conservify.data.FileRecord;
import org.conservify.data.ReadOptions;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PbFile {
    private static final String TAG = "JS";

    private final DataListener listener;
    private final String path;

    public PbFile(DataListener listener, String path) {
        this.listener = listener;
        this.path = path;
    }

    public boolean readRecord(long record, ReadOptions options) {
        return false;
    }

    public boolean readAll(ReadOptions options) {
        try {
            FileInputStream fis = new FileInputStream(this.path);

            List<FileRecord> records = new ArrayList<>();

            while (true) {
                int firstByte = fis.read();
                if (firstByte == -1) {
                    return true;
                }

                int recordSize = CodedInputStream.readRawVarint32(firstByte, fis);
                byte[] data = new byte[recordSize];
                int bytesRead = fis.read(data);

                if (bytesRead != recordSize) {
                    Log.e(TAG, "error reading entire record");
                    return false;
                }

                records.add(new FileRecord(data));
            }
        } catch (FileNotFoundException e) {
            Log.e(TAG, "error", e);
            return false;
        } catch (IOException e) {
            Log.e(TAG, "error", e);
            return false;
        }
    }
}
