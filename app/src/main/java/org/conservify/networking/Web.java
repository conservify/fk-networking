package org.conservify.networking;

import android.content.Context;

import java.util.Map;

public class Web {
    private final Context context;
    private final WebTransferListener uploadListener;
    private final WebTransferListener downloadListener;

    public Web(Context context, WebTransferListener uploadListener, WebTransferListener downloadListener) {
        this.context = context;
        this.uploadListener = uploadListener;
        this.downloadListener = downloadListener;
    }

    public String download(WebFileTransfer transfer) {
        return null;
    }

    public String upload(WebFileTransfer transfer) {
        return null;
    }

    public String json(WebFileTransfer transfer) {
        return null;
    }
}
