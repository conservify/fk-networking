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
    private final byte[] requestBody;

    public BinaryRequest(int method, String url, Map<String, String> headers, byte[] requestBody, Response.Listener<BinaryResponse> listener, Response.ErrorListener errorListener) {
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
    protected Response<BinaryResponse> parseNetworkResponse(NetworkResponse response) {
        return BinaryResponse.fromNetworkResponse(response);
    }

    @Override
    protected void deliverResponse(BinaryResponse response) {
        listener.onResponse(response);
    }

    @Override
    public byte[] getBody() {
        return requestBody;
    }
}
