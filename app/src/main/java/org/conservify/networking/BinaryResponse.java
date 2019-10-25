package org.conservify.networking;

import java.util.Map;

public class BinaryResponse {
    private final byte[] data;
    private final int statusCode;
    private final Map<String, String> headers;

    public byte[] getData() {
        return data;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public BinaryResponse(byte[] data, int statusCode, Map<String, String> headers) {
        this.data = data;
        this.statusCode = statusCode;
        this.headers = headers;
    }
}
