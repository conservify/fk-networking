package org.conservify.networking;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
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

    public static Response<VerboseJsonObject> fromNetworkResponse(NetworkResponse response) {
        try {
            String jsonString = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
            JSONObject object = null;
            if (jsonString.length() > 0 ) {
                object = new JSONObject(jsonString);
            }
            VerboseJsonObject vjo = new VerboseJsonObject(object, response.statusCode, response.headers);
            return Response.success(vjo, HttpHeaderParser.parseCacheHeaders(response));
        } catch (UnsupportedEncodingException e) {
            return Response.error(new ParseError(e));
        } catch (JSONException je) {
            return Response.error(new ParseError(je));
        }

    }
}
