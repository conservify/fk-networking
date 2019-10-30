package org.conservify.networking;

import android.support.annotation.NonNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import okio.ForwardingSource;
import okio.Okio;
import okio.Source;

public class ProgressAwareResponseBody extends ResponseBody {
    private final ResponseBody responseBody;
    private final WebTransferListener listener;
    private final String tag;
    private final Headers headers;
    private BufferedSource bufferedSource;
    private long lastProgress = 0;

    ProgressAwareResponseBody(String tag, Headers headers, ResponseBody responseBody, WebTransferListener listener) {
        this.responseBody = responseBody;
        this.listener = listener;
        this.tag = tag;
        this.headers = headers;
    }

    @Override
    public MediaType contentType() {
        return responseBody.contentType();
    }

    @Override
    public long contentLength() {
        return responseBody.contentLength();
    }

    @Override
    public BufferedSource source() {
        if (bufferedSource == null) {
            bufferedSource = Okio.buffer(source(responseBody.source()));
        }
        return bufferedSource;
    }

    private Source source(Source source) {
        final Map<String, String> headers = new HashMap<>();

        listener.onProgress(tag, headers, 0, contentLength());

        return new ForwardingSource(source) {
            long totalBytesRead = 0;
            long contentLength = contentLength();

            @Override public long read(@NonNull Buffer sink, long byteCount) throws IOException {
                long bytesRead = super.read(sink, byteCount);

                totalBytesRead += bytesRead != -1 ? bytesRead : 0;

                if (lastProgress == 0 || totalBytesRead == contentLength || System.currentTimeMillis() - lastProgress > 500) {
                    lastProgress = System.currentTimeMillis();
                    listener.onProgress(tag, headers, totalBytesRead, contentLength);
                }

                return bytesRead;
            }
        };
    }
}
