package org.conservify.networking;

import java.util.Map;

public interface WebTransferListener {
    void onProgress(String task, Map<String, String> headers, long bytes, long total);
    void onComplete(String task, Map<String, String> headers, String contentType, Object body, int statusCode);
    void onError(String task, String message);
}
