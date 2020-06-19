package org.conservify.data;

public class ReadOptions {
    private int batchSize;
    private boolean base64EncodeData = true;

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public boolean isBase64EncodeData() {
        return base64EncodeData;
    }

    public void setBase64EncodeData(boolean base64EncodeData) {
        this.base64EncodeData = base64EncodeData;
    }
}
