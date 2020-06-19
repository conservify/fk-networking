package org.conservify;

import org.conservify.data.FileInfo;
import org.conservify.data.FileSystem;
import org.conservify.data.ReadOptions;
import org.conservify.data.RecordListener;
import org.conservify.data.SampleData;
import org.conservify.data.pb.PbFile;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.*;

public class FileSystemTest {
    @Test
    public void test_opening_file() {
        RecordListener listener = new RecordListener() {
            @Override
            public void onFileInfo(String path, String token, FileInfo info) {
            }

            @Override
            public void onFileRecords(String path, String token, long position, long size, List<Object> records) {
            }

            @Override
            public void onFileError(String path, String token, String message) {
            }
        };

        File file = new File(new SampleData().write());
        FileSystem fs = new FileSystem( listener);
        PbFile meta = fs.open(file.getAbsolutePath());
        assertTrue(meta.readInfo("token"));
        assertTrue(meta.readDataRecords("token", new ReadOptions()));
        assertTrue(meta.readDelimited("token", new ReadOptions()));
    }
}
