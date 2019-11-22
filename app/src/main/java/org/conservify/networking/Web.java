package org.conservify.networking;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
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

    private okhttp3.Request buildDownloadRequest(WebTransfer transfer) {
        Headers headers = Headers.of(transfer.getHeaders());

        if (getMethod(transfer.getMethodOrDefault()) == Request.Method.GET) {
            return new okhttp3.Request.Builder()
                    .headers(headers)
                    .url(transfer.getUrl())
                    .build();
        }

        RequestBody requestbody = RequestBody.create(new byte[0]);

        return new okhttp3.Request.Builder()
                .method(transfer.getMethod(), requestbody)
                .headers(headers)
                .url(transfer.getUrl())
                .build();
    }

    public String download(final WebTransfer transfer) {
        final String id = transfer.getId();

        Log.e(TAG, "[networking] " + id + " download: " + transfer.getMethodOrDefault() + " " + transfer.getUrl() + " to " + transfer.getPath());

        okhttp3.Request request = buildDownloadRequest(transfer);

        okClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Log.e(TAG, "[networking] " + id + " failure", e);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull okhttp3.Response response) {
                Log.i(TAG, "[networking] " + id + " done");

                Map<String, String> headers = new HashMap<String, String>();
                Headers responseHeaders = response.headers();
                for (int i = 0, size = responseHeaders.size(); i < size; i++) {
                    headers.put(responseHeaders.name(i), responseHeaders.value(i));
                }

                String contentType = response.headers().get("Content-Type");

                ResponseBody responseBody = new ProgressAwareResponseBody(id, response.headers(), response.body(), downloadListener);
                BufferedSource bufferedSource = responseBody.source();
                BufferedSink sink = null;

                try {
                    sink = Okio.buffer(Okio.sink(new File(transfer.getPath())));
                    sink.writeAll(Okio.source(responseBody.byteStream()));

                    downloadListener.onComplete(id, headers, contentType, null, response.code());
                }
                catch (IOException e) {
                    Log.e(TAG, "[networking] " + id + " failure", e);
                    downloadListener.onError(id, e.getMessage());
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

        Log.e(TAG, "[networking] " + id + " upload: " + transfer.getMethodOrDefault() + " " + transfer.getUrl() + " to " + transfer.getPath());

        Headers headers = Headers.of(transfer.getHeaders());
        String contentType = headers.get("Content-Type");
        RequestBody requestBody = new FileUploadRequestBody(id, new File(transfer.getPath()), contentType, uploadListener);

        okhttp3.Request request = new okhttp3.Request.Builder()
                .method(transfer.getMethodOrDefault(), requestBody)
                .url(transfer.getUrl())
                .headers(headers)
                .build();

        okClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Log.e(TAG, "[networking] " + id + " failure", e);
                downloadListener.onError(id, e.getMessage());
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull okhttp3.Response response) {
                Log.i(TAG, "[networking] " + id + " done");

                Map<String, String> headers = new HashMap<String, String>();
                Headers responseHeaders = response.headers();
                for (int i = 0, size = responseHeaders.size(); i < size; i++) {
                    headers.put(responseHeaders.name(i), responseHeaders.value(i));
                }

                String contentType = responseHeaders.get("content-type");
                downloadListener.onComplete(id, headers, contentType, null, response.code());
            }
        });

        return id;
    }

    public String json(final WebTransfer transfer) {
        final String id = transfer.getId();

        Log.e(TAG, "[networking] " + id + " json: " + transfer.getMethodOrDefault() + " " + transfer.getUrl());

        String requestBody = transfer.getBody();

        VerboseJsonObjectRequest jsonObjectRequest = new VerboseJsonObjectRequest(getMethod(transfer.getMethodOrDefault()), transfer.getUrl(), transfer.getHeaders(), requestBody, new Response.Listener<VerboseJsonObject>() {
            @Override
            public void onResponse(VerboseJsonObject response) {
                Log.i(TAG, "[networking] " + id + " done");

                String contentType = response.getHeaders().get("content-type");
                String body = response.getObject() != null ? response.getObject().toString() : null;
                downloadListener.onComplete(id, response.getHeaders(), contentType, body, response.getStatusCode());
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG,"[networking] " + id + " failure", error);
                downloadListener.onError(id, error.getMessage());
            }
        });

        addToRequestQueue(jsonObjectRequest);

        return id;
    }

    public String binary(final WebTransfer transfer) {
        final String id = transfer.getId();

        Log.e(TAG, "[networking] " + id + " binary: " + transfer.getMethodOrDefault() + " " + transfer.getUrl());

        byte[] requestBody = null;

        if (transfer.isBase64DecodeRequestBody()) {
            requestBody = Base64.decode(transfer.getBody(), 0);
        }

        BinaryRequest binaryRequest = new BinaryRequest(getMethod(transfer.getMethodOrDefault()), transfer.getUrl(), transfer.getHeaders(), requestBody, new Response.Listener<BinaryResponse>() {
            @Override
            public void onResponse(BinaryResponse response) {
                Log.i(TAG, "[networking] " + id + " done");

                String contentType = response.getHeaders().get("content-type");

                Object body = response.getData();
                if (transfer.isBase64EncodeResponseBody()) {
                    body = Base64.encodeToString(response.getData(), 0);
                }

                downloadListener.onComplete(transfer.getId(), response.getHeaders(), contentType, body, response.getStatusCode());

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG,"[networking] " + id + " failure", error);
                downloadListener.onError(id, error.getMessage());
            }
        });

        addToRequestQueue(binaryRequest);

        return id;
    }

    private int getMethod(String method) {
        switch (method.toUpperCase()) {
            case "GET": return Request.Method.GET;
            case "POST": return Request.Method.POST;
            case "DELETE": return Request.Method.DELETE;
            case "HEAD": return Request.Method.HEAD;
            case "PATCH": return Request.Method.PATCH;
        }
        throw new RuntimeException("Unknown Method: " + method);
    }
}
