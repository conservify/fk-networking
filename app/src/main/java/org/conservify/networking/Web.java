package org.conservify.networking;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;

public class Web {
    private static final String TAG = "JS";

    private final Context context;
    private final WebTransferListener uploadListener;
    private final WebTransferListener downloadListener;
    private final RequestQueue requestQueue;
    private final OkHttpClient okClient;

    public Web(Context context, WebTransferListener uploadListener, WebTransferListener downloadListener) {
        if (uploadListener == null) throw new IllegalArgumentException();
        if (downloadListener == null) throw new IllegalArgumentException();

        this.context = context;
        this.uploadListener = uploadListener;
        this.downloadListener = downloadListener;
        this.requestQueue = Volley.newRequestQueue(this.context);
        this.okClient = new OkHttpClient();
    }

    public <T> void addToRequestQueue(Request<T> req) {
        requestQueue.add(req);
    }

    public String download(final WebTransfer transfer) {
        final String id = transfer.getId();

        Log.e(TAG, "[networking] download: " + transfer.getUrl() + " to " + transfer.getPath());

        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(transfer.getUrl())
                .build();

        okClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Log.i(TAG, "failure", e);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull okhttp3.Response response) {
                Map<String, String> headers = new HashMap<String, String>();
                Headers responseHeaders = response.headers();
                for (int i = 0, size = responseHeaders.size(); i < size; i++) {
                    headers.put(responseHeaders.name(i), responseHeaders.value(i));
                }

                downloadListener.onStarted(id, headers);

                ResponseBody responseBody = new ProgressAwareResponseBody(id, response.headers(), response.body(), downloadListener);
                BufferedSource bufferedSource = responseBody.source();
                BufferedSink sink = null;

                try {
                    sink = Okio.buffer(Okio.sink(new File(transfer.getPath())));
                    sink.writeAll(Okio.source(responseBody.byteStream()));

                    downloadListener.onComplete(id, headers, null, null, response.code());
                }
                catch (IOException e) {
                    Log.e(TAG, "error", e);
                    downloadListener.onError(id);
                }
                finally {
                    if (sink != null) {
                        try {
                            sink.close();
                        } catch (IOException e) {
                            Log.e(TAG, "error", e);
                        }
                    }
                }
            }
        });

        return id;
    }

    public String upload(final WebTransfer transfer) {
        final String id = transfer.getId();

        Log.e(TAG, "[networking] upload: " + transfer.getUrl() + " to " + transfer.getPath());
        return id;
    }

    public String json(final WebTransfer transfer) {
        final String id = transfer.getId();

        Log.e(TAG, "[networking] json: " + transfer.getUrl());

        String requestBody = transfer.getBody();

        VerboseJsonObjectRequest jsonObjectRequest = new VerboseJsonObjectRequest(Request.Method.GET, transfer.getUrl(), transfer.getHeaders(), requestBody, new Response.Listener<VerboseJsonObject>() {
            @Override
            public void onResponse(VerboseJsonObject response) {
                String contentType = response.getHeaders().get("content-type");
                downloadListener.onStarted(id, response.getHeaders());
                downloadListener.onComplete(id, response.getHeaders(), contentType, response.getObject().toString(), response.getStatusCode());
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG,"Error", error);
                downloadListener.onError(id);
            }
        });

        addToRequestQueue(jsonObjectRequest);

        return id;
    }

    public String binary(final WebTransfer transfer) {
        final String id = transfer.getId();

        Log.e(TAG, "[networking] binary: " + transfer.getUrl());

        byte[] requestBody = null;

        if (transfer.isBase64DecodeRequestBody()) {
            requestBody = Base64.decode(transfer.getBody(), 0);
        }

        BinaryRequest binaryRequest = new BinaryRequest(Request.Method.GET, transfer.getUrl(), transfer.getHeaders(), requestBody, new Response.Listener<BinaryResponse>() {
            @Override
            public void onResponse(BinaryResponse response) {
                String contentType = response.getHeaders().get("content-type");

                Object body = response.getData();
                if (transfer.isBase64EncodeResponseBody()) {
                    body = Base64.encodeToString(response.getData(), 0);
                }

                downloadListener.onStarted(id, response.getHeaders());
                downloadListener.onComplete(transfer.getId(), response.getHeaders(), contentType, body, response.getStatusCode());

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG,"Error", error);
                downloadListener.onError(id);
            }
        });

        addToRequestQueue(binaryRequest);

        return id;
    }
}
