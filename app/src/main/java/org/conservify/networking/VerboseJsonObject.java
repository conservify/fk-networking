package org.conservify.networking;

import org.json.JSONObject;

import java.util.Map;

public class VerboseJsonObject {
    private final JSONObject object;
    private int statusCode;
    private Map<String, String> headers;

    public JSONObject getObject() {
        return object;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public VerboseJsonObject(JSONObject object, int statusCode, Map<String, String> headers) {
        this.object = object;
        this.statusCode = statusCode;
        this.headers = headers;
    }
}
