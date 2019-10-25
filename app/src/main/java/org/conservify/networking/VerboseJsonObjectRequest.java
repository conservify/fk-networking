package org.conservify.networking;

import android.support.annotation.Nullable;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.Map;

public class VerboseJsonObjectRequest extends Request<VerboseJsonObject> {
    private final Map<String, String> headers;
    @Nullable
    private final JSONObject jsonRequest;
    private final Response.Listener<VerboseJsonObject> listener;

    public VerboseJsonObjectRequest(int method, String url, Map<String, String> headers, @Nullable JSONObject jsonRequest, Response.Listener<VerboseJsonObject> listener, @Nullable Response.ErrorListener errorListener) {
        super(method, url, errorListener);
        this.headers = headers;
        this.jsonRequest = jsonRequest;
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
        try {
            String jsonString = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
            JSONObject object = new JSONObject(jsonString);
            VerboseJsonObject vjo = new VerboseJsonObject(object, response.statusCode, response.headers);
            return Response.success(vjo, HttpHeaderParser.parseCacheHeaders(response));
        } catch (UnsupportedEncodingException e) {
            return Response.error(new ParseError(e));
        } catch (JSONException je) {
            return Response.error(new ParseError(je));
        }
    }
}



