package org.conservify.networking;

import com.android.volley.NetworkResponse;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;

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

    public static Response<BinaryResponse> fromNetworkResponse(NetworkResponse response) {
        BinaryResponse br = new BinaryResponse(response.data, response.statusCode, response.headers);
        return Response.success(br, HttpHeaderParser.parseCacheHeaders(response));
    }
}
