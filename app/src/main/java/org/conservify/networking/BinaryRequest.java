package org.conservify.networking;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;

import java.util.Map;

public class BinaryRequest extends Request<BinaryResponse> {
    private final Map<String, String> headers;
    private final Response.Listener<BinaryResponse> listener;

    public BinaryRequest(int method, String url, Map<String, String> headers, Response.Listener<BinaryResponse> listener, Response.ErrorListener errorListener) {
        super(method, url, errorListener);
        this.headers = headers;
        this.listener = listener;
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        return headers != null ? headers : super.getHeaders();
    }

    @Override
    protected Response<BinaryResponse> parseNetworkResponse(NetworkResponse response) {
        BinaryResponse br = new BinaryResponse(response.data, response.statusCode, response.headers);
        return Response.success(br, HttpHeaderParser.parseCacheHeaders(response));
    }

    @Override
    protected void deliverResponse(BinaryResponse response) {
        listener.onResponse(response);
    }
}
