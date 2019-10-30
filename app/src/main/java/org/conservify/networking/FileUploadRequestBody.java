package org.conservify.networking;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.internal.Util;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;

public class FileUploadRequestBody extends RequestBody {
    private final String taskId;
    private final File file;
    private final String contentType;
    private final WebTransferListener uploadListener;

    public FileUploadRequestBody(String taskId, File file, String contentType, WebTransferListener uploadListener) {
        this.taskId = taskId;
        this.file = file;
        this.contentType = contentType;
        this.uploadListener = uploadListener;
    }

    @Override
    public long contentLength() throws IOException {
        return file.length();
    }

    @Nullable
    @Override
    public MediaType contentType() {
        return MediaType.parse(contentType);
    }

    @Override
    public void writeTo(@NotNull BufferedSink bufferedSink) throws IOException {
        Map<String, String> headers = new HashMap<String, String>();

        Source source = null;
        try {
            source = Okio.source(this.file);

            long copied = 0;
            long read = 0;

            uploadListener.onProgress(taskId, headers, 0, file.length());

            while ((read = source.read(bufferedSink.getBuffer(), 4096)) != -1) {
                copied += read;

                bufferedSink.flush();

                uploadListener.onProgress(taskId, headers, copied, file.length());
            }

            uploadListener.onProgress(taskId, headers, copied, file.length());
        }
        finally {
            Util.closeQuietly(source);
        }
    }
}
