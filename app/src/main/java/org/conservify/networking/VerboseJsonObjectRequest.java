package org.conservify.networking;

import android.support.annotation.Nullable;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.HttpHeaderParser;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.Map;

public class VerboseJsonObjectRequest extends Request<VerboseJsonObject> {
    private static final String PROTOCOL_CHARSET = "utf-8";
    private static final String PROTOCOL_CONTENT_TYPE = String.format("application/json; charset=%s", PROTOCOL_CHARSET);

    private final Map<String, String> headers;
    @Nullable
    private final String requestBody;
    private final Response.Listener<VerboseJsonObject> listener;

    public VerboseJsonObjectRequest(int method, String url, Map<String, String> headers, @Nullable String requestBody, Response.Listener<VerboseJsonObject> listener, @Nullable Response.ErrorListener errorListener) {
        super(method, url, errorListener);
        this.headers = headers;
        this.requestBody = requestBody;
        this.listener = listener;
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        return headers != null ? headers : super.getHeaders();
    }

    @Override
    protected void deliverResponse(VerboseJsonObject response) {
        listener.onResponse(response);
    }

    @Override
    protected Response<VerboseJsonObject> parseNetworkResponse(NetworkResponse response) {
        return VerboseJsonObject.fromNetworkResponse(response);
    }

    @Override
    public byte[] getBody() {
        try {
            return requestBody == null ? null : requestBody.getBytes(PROTOCOL_CHARSET);
        } catch (UnsupportedEncodingException uee) {
            VolleyLog.wtf("Unsupported Encoding while trying to get the bytes of %s using %s", requestBody, PROTOCOL_CHARSET);
            return null;
        }
    }
}



