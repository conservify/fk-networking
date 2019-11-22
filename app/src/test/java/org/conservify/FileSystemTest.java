package org.conservify;

import org.conservify.data.FileSystem;
import org.conservify.data.MetaFile;
import org.conservify.data.ReadOptions;
import org.junit.Test;

import static org.junit.Assert.*;

public class FileSystemTest {
    @Test
    public void test_opening_file() {
        FileSystem fs = new FileSystem(null, null);
        MetaFile meta = fs.openMeta("/home/jlewallen/fieldkit/stations/ancient_goose_81/20191024_153709_meta.fkpb");
        meta.readAll(new ReadOptions());
    }
}