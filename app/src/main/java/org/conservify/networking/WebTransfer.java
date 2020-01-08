package org.conservify.networking;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WebTransfer {
    private String id;
    private String method;
    private String url;
    private String path;
    private Map<String, String> headers = new HashMap<String, String>();
    private String body;
    private String contentType;
    private boolean base64DecodeRequestBody;
    private boolean base64EncodeResponseBody;
    private int connectionTimeout = 10;
    private int defaultTimeout = 60*5;

    public String getId() {
        return id;
    }

    public String getTransferId() {
        return id;
    }

    public String getMethod() {
        return method;
    }

    public String getMethodOrDefault() {
        if (method == null) {
            return "GET";
        }
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

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

    public boolean isBase64DecodeRequestBody() {
        return base64DecodeRequestBody;
    }

    public void setBase64DecodeRequestBody(boolean base64DecodeRequestBody) {
        this.base64DecodeRequestBody = base64DecodeRequestBody;
    }

    public boolean isBase64EncodeResponseBody() {
        return base64EncodeResponseBody;
    }

    public void setBase64EncodeResponseBody(boolean base64EncodeResponseBody) {
        this.base64EncodeResponseBody = base64EncodeResponseBody;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public int getDefaultTimeout() {
        return defaultTimeout;
    }

    public void setDefaultTimeout(int defaultTimeout) {
        this.defaultTimeout = defaultTimeout;
    }

    public WebTransfer() {
        this.id = UUID.randomUUID().toString();
    }

    public WebTransfer(String url) {
        this.id = UUID.randomUUID().toString();
    }

    public WebTransfer(String url, String path, Map<String, String> headers) {
        this.id = UUID.randomUUID().toString();
        this.url = url;
        this.path = path;
        this.headers = headers;
    }

    public WebTransfer header(String key, String value) {
        this.headers.put(key, value);
        return this;
    }

}
