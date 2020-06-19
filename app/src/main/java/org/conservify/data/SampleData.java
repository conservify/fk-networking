package org.conservify.data;

import com.google.protobuf.ByteString;

import org.conservify.fieldkit.data.pb.FkData;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class SampleData {
    public String write() {
        try {
            File file = File.createTempFile("fksample", null);

            FileOutputStream fos = new FileOutputStream(file);
            for (int i = 0; i < 100; ++i) {
                FkData.DataRecord record = FkData.DataRecord.newBuilder()
                        .setMetadata(FkData.Metadata.newBuilder().setDeviceId(ByteString.EMPTY).setFirmware(FkData.Firmware.newBuilder().setBuild("build").setHash("hash")))
                        .setIdentity(FkData.Identity.newBuilder().setName("Fake Station"))
                        .build();
                record.writeDelimitedTo(fos);
            }
            fos.close();

            return file.getPath();
        }
        catch (IOException error) {
            throw new RuntimeException(error);
        }
    }
}
