package org.conservify.data.pb;

import android.os.AsyncTask;
import android.util.Log;

import com.google.protobuf.CodedInputStream;

import org.conservify.data.FileInfo;
import org.conservify.data.FileSystemListener;
import org.conservify.data.ReadOptions;
import org.conservify.fieldkit.data.pb.FkData;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PbFile {
    private static final String TAG = "JS";

    private final FileSystemListener listener;
    private final String path;

    public PbFile(FileSystemListener listener, String path) {
        this.listener = listener;
        this.path = path;
    }

    public boolean readRecord(long record, ReadOptions options) {
        return false;
    }

    private class ReadInfoTask extends AsyncTask<PbFile, Void, Boolean> {
        private final String token;
        private final PbFile file;

        private ReadInfoTask(String token, PbFile file) {
            this.token = token;
            this.file = file;
        }

        @Override
        protected Boolean doInBackground(PbFile... pbFiles) {
            try {
                FileInputStream fis = new FileInputStream(this.file.path);

                try {
                    /*
                    while (true) {
                        FkData.DataRecord record = FkData.DataRecord.parseDelimitedFrom(fis);
                        if (record == null) {
                            break;
                        }
                    }
                    */
                    File javaFile = new File(this.file.path);
                    long size = javaFile.getTotalSpace();
                    FileInfo info = new FileInfo(this.file.path, size);
                    this.file.listener.onFileInfo(this.file.path, this.token, info);
                }
                finally {
                    fis.close();
                }

                return true;
            } catch (IOException e) {
                Log.e(TAG, "error", e);
                this.file.listener.onFileError(this.file.path, this.token, e.getMessage());
                return false;
            }
        }
    }

    public boolean readInfo(String token) {
        ReadInfoTask task = new ReadInfoTask(token,this);
        task.execute(this);
        return true;
    }

    private class ReadRecordsTask extends AsyncTask<PbFile, Void, Boolean> {
        private final String token;
        private final PbFile file;
        private final ReadOptions options;

        private ReadRecordsTask(String token, PbFile file, ReadOptions options) {
            this.token = token;
            this.file = file;
            this.options = options;
        }

        @Override
        protected Boolean doInBackground(PbFile... pbFiles) {
            try {
                FileInputStream fis = new FileInputStream(file.path);

                long size = fis.getChannel().size();

                try {
                    List<Object> records = new ArrayList<>();
                    while (true) {
                        FkData.DataRecord record = FkData.DataRecord.parseDelimitedFrom(fis);
                        if (record == null) {
                            break;
                        }
                        records.add(record);

                        if (records.size() == options.getBatchSize()) {
                            file.listener.onFileRecords(file.path, this.token, fis.getChannel().position(), size, records);
                            records.clear();
                        }
                    }

                    if (records.size() > 0) {
                        file.listener.onFileRecords(file.path, this.token, fis.getChannel().position(), size, records);
                        records.clear();
                    }

                    file.listener.onFileRecords(file.path, this.token, fis.getChannel().position(), size,null);
                } finally {
                    fis.close();
                }

                return true;
            } catch (IOException e) {
                Log.e(TAG, "error", e);
                this.file.listener.onFileError(this.file.path, this.token, e.getMessage());
                return false;
            }
        }
    }

    public boolean readDataRecords(String token, ReadOptions options) {
        ReadRecordsTask task = new ReadRecordsTask(token, this, options);
        task.execute(this);
        return true;
    }
    private class ReadDelimitedTask extends AsyncTask<PbFile, Void, Boolean> {
        private final String token;
        private final PbFile file;
        private final ReadOptions options;

        private ReadDelimitedTask(String token, PbFile file, ReadOptions options) {
            this.token = token;
            this.file = file;
            this.options = options;
        }

        @Override
        protected Boolean doInBackground(PbFile... pbFiles) {
            try {
                FileInputStream fis = new FileInputStream(file.path);

                long size = fis.getChannel().size();

                try {
                    List<Object> records = new ArrayList<>();
                    while (true) {
                        int firstByte = fis.read();
                        if (firstByte == -1) {
                            break;
                        }

                        int recordSize = CodedInputStream.readRawVarint32(firstByte, fis);
                        byte[] data = new byte[recordSize];
                        int bytesRead = fis.read(data);

                        records.add(data);

                        if (records.size() == options.getBatchSize()) {
                            file.listener.onFileRecords(file.path, this.token, fis.getChannel().position(), size, records);
                            records.clear();
                        }
                    }

                    if (records.size() > 0) {
                        file.listener.onFileRecords(file.path, this.token, fis.getChannel().position(), size, records);
                        records.clear();
                    }

                    file.listener.onFileRecords(file.path, this.token, fis.getChannel().position(), size,null);
                } finally {
                    fis.close();
                }

                return true;
            } catch (IOException e) {
                Log.e(TAG, "error", e);
                this.file.listener.onFileError(this.file.path, this.token, e.getMessage());
                return false;
            }
        }
    }

    public boolean readDelimited(String token, ReadOptions options) {
        ReadDelimitedTask task = new ReadDelimitedTask(token, this, options);
        task.execute(this);
        return true;
    }
}
