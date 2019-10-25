package org.conservify.networking;

import java.util.Map;

public class WebFileTransfer {
    private final String url;
    private final String path;
    private final Map<String, String> headers;

    public String getUrl() {
        return url;
    }

    public String getPath() {
        return path;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public WebFileTransfer(String url, String path, Map<String, String> headers) {
        this.url = url;
        this.path = path;
        this.headers = headers;
    }
}
