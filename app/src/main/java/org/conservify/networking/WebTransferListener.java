package org.conservify.networking;

import java.util.Map;

public interface WebTransferListener {
    void onStarted(String task, Map<String, String> headers);
    void onProgress(String task, long bytes, long total);
    void onComplete(String task, Map<String, String> headers, String body, int statusCode);
    void onError(String task);
}
