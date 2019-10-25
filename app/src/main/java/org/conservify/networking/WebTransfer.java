package org.conservify.networking;

import java.util.HashMap;
import java.util.Map;

public class WebTransfer {
    private String url;
    private String path;
    private Map<String, String> headers = new HashMap<String, String>();
    private String body;
    private String contentType;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public WebTransfer() {
    }

    public WebTransfer(String url) {
    }

    public WebTransfer(String url, String path, Map<String, String> headers) {
        this.url = url;
        this.path = path;
        this.headers = headers;
    }

    public WebTransfer header(String key, String value) {
        this.headers.put(key, value);
        return this;
    }

}
