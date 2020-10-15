package org.conservify.networking;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

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
    private final Boolean verboseErrors = false;

    public Web(Context context, WebTransferListener uploadListener, WebTransferListener downloadListener) {
        if (uploadListener == null) throw new IllegalArgumentException();
        if (downloadListener == null) throw new IllegalArgumentException();

        this.context = context;
        this.uploadListener = uploadListener;
        this.downloadListener = downloadListener;
        this.requestQueue = Volley.newRequestQueue(this.context);
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

        final OkHttpClient okClient = new OkHttpClient.Builder()
                .connectTimeout(transfer.getConnectionTimeout(), TimeUnit.SECONDS)
                .readTimeout(transfer.getDefaultTimeout(), TimeUnit.SECONDS)
                .writeTimeout(transfer.getDefaultTimeout(), TimeUnit.SECONDS)
                .build();

        okClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                if (verboseErrors) {
                    Log.e(TAG, "[networking] " + id + " failure", e);
                } else {
                    Log.e(TAG, "[networking] " + id + " failure: " + e.getMessage());
                }
                downloadListener.onError(id, e.getMessage());
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull okhttp3.Response response) {
                Headers responseHeaders = response.headers() != null ? response.headers() : Headers.of();
                ResponseBody responseBody = new ProgressAwareResponseBody(id, responseHeaders, response.body(), downloadListener);
                BufferedSource bufferedSource = responseBody.source();
                BufferedSink sink = null;

                try {
                    sink = Okio.buffer(Okio.sink(new File(transfer.getPath())));
                    sink.writeAll(Okio.source(responseBody.byteStream()));
                    sink.flush();

                    Map<String, String> headers = new HashMap<String, String>();
                    for (int i = 0, size = responseHeaders.size(); i < size; i++) {
                        headers.put(responseHeaders.name(i), responseHeaders.value(i));
                    }
                    String contentType = responseHeaders.get("Content-Type");
                    downloadListener.onComplete(id, headers, contentType, null, response.code());

                    Log.i(TAG, "[networking] " + id + " done");
                }
                catch (IOException e) {
                    if (verboseErrors) {
                        Log.e(TAG, "[networking] " + id + " failure", e);
                    }
                    else {
                        Log.e(TAG, "[networking] " + id + " failure: " + e.getMessage());
                    }
                    downloadListener.onError(id, e.getMessage());
                }
                finally {
                    Log.e(TAG, "[networking] " + id + " finally");
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

        Log.e(TAG, "[networking] " + id + " upload: " + transfer.getMethodOrDefault() + " " + transfer.getPath() + " to " + transfer.getUrl());

        Headers headers = Headers.of(transfer.getHeaders());
        String contentType = headers.get("Content-Type");
        RequestBody requestBody = new FileUploadRequestBody(id, new File(transfer.getPath()), contentType, uploadListener);

        okhttp3.Request request = new okhttp3.Request.Builder()
                .method(transfer.getMethodOrDefault(), requestBody)
                .url(transfer.getUrl())
                .headers(headers)
                .build();

        final OkHttpClient okClient = new OkHttpClient.Builder()
                .connectTimeout(transfer.getConnectionTimeout(), TimeUnit.SECONDS)
                .readTimeout(transfer.getDefaultTimeout(), TimeUnit.SECONDS)
                .writeTimeout(transfer.getDefaultTimeout(), TimeUnit.SECONDS)
                .build();

        okClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                if (verboseErrors ) {
                    Log.e(TAG, "[networking] " + id + " failure", e);
                }
                else {
                    Log.e(TAG, "[networking] " + id + " failure: " + e.getMessage());
                }
                uploadListener.onError(id, e.getMessage());
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull okhttp3.Response response) {
                Headers responseHeaders = response.headers() != null ? response.headers() : Headers.of();

                String body = null;
                try {
                    body = response.body().string();
                } catch (IOException e) {
                    Log.e(TAG, "error", e);
                }

                String contentType = responseHeaders.get("content-type");
                Map<String, String> headers = new HashMap<String, String>();
                for (int i = 0, size = responseHeaders.size(); i < size; i++) {
                    headers.put(responseHeaders.name(i), responseHeaders.value(i));
                }
                uploadListener.onComplete(id, headers, contentType, body, response.code());

                Log.i(TAG, "[networking] " + id + " done");
            }
        });

        return id;
    }

    public String json(final WebTransfer transfer) {
        final String id = transfer.getId();

        Log.e(TAG, "[networking] " + id + " json: " + transfer.getMethodOrDefault() + " " + transfer.getUrl());

        String requestBody = transfer.getBody();

        final VerboseJsonObjectRequest jsonObjectRequest = new VerboseJsonObjectRequest(getMethod(transfer.getMethodOrDefault()), transfer.getUrl(), transfer.getHeaders(), requestBody, new Response.Listener<VerboseJsonObject>() {
            @Override
            public void onResponse(VerboseJsonObject response) {
                invokeListener(transfer, response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (error.networkResponse != null) {
                    VerboseJsonObject response = VerboseJsonObject.fromNetworkResponse(error.networkResponse).result;
                    if (response != null) {
                        invokeListener(transfer, response);
                        return;
                    }
                }

                if (verboseErrors ) {
                    Log.e(TAG, "[networking] " + id + " failure", error);
                }
                else {
                    Log.e(TAG, "[networking] " + id + " failure: " + error.getMessage());
                }
                downloadListener.onError(id, error.getMessage());
            }
        });

        addToRequestQueue(jsonObjectRequest);

        return id;
    }

    private void invokeListener(final WebTransfer transfer, VerboseJsonObject response) {
        Log.i(TAG, "[networking] " + transfer.getId() + " done");
        String contentType = response.getHeaders().get("content-type");
        String body = response.getObject() != null ? response.getObject().toString() : null;
        downloadListener.onComplete(transfer.getId(), response.getHeaders(), contentType, body, response.getStatusCode());
    }

    public String binary(final WebTransfer transfer) {
        final String id = transfer.getId();

        Log.e(TAG, "[networking] " + id + " binary: " + transfer.getMethodOrDefault() + " " + transfer.getUrl());

        byte[] requestBody = null;

        if (transfer.isBase64DecodeRequestBody()) {
            requestBody = Base64.decode(transfer.getBody(), 0);
        }
        else if (transfer.getBody() != null) {
            requestBody = transfer.getBody().getBytes(Charset.forName("UTF-8"));
        }

        BinaryRequest binaryRequest = new BinaryRequest(getMethod(transfer.getMethodOrDefault()), transfer.getUrl(), transfer.getHeaders(), requestBody, new Response.Listener<BinaryResponse>() {
            @Override
            public void onResponse(BinaryResponse response) {
                invokeListener(transfer, response);

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (error.networkResponse != null) {
                    BinaryResponse response = BinaryResponse.fromNetworkResponse(error.networkResponse).result;
                    if (response != null) {
                        invokeListener(transfer, response);
                        return;
                    }
                }

                if (verboseErrors) {
                    Log.e(TAG,"[networking] " + id + " failure", error);
                }
                else {
                    Log.e(TAG,"[networking] " + id + " failure: " + error.getMessage());
                }
                downloadListener.onError(id, error.getMessage());
            }
        });

        addToRequestQueue(binaryRequest);

        return id;
    }

    private void invokeListener(final WebTransfer transfer, BinaryResponse response) {
        Log.i(TAG, "[networking] " + transfer.getId() + " done");

        String contentType = response.getHeaders().get("content-type");

        Object body = null;
        if (transfer.isBase64EncodeResponseBody()) {
            body = Base64.encodeToString(response.getData(), 0);
        } else {
            body = new String(response.getData(), Charset.forName("UTF-8"));
        }

        downloadListener.onComplete(transfer.getId(), response.getHeaders(), contentType, body, response.getStatusCode());
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
