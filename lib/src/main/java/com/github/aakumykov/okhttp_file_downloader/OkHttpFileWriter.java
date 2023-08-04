package com.github.aakumykov.okhttp_file_downloader;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import okhttp3.ResponseBody;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;

public class OkHttpFileWriter implements AutoCloseable {

    private final BufferedSink mBufferedSink;
    @Nullable private ProgressCallback mProgressCallback;


    public OkHttpFileWriter(File outputFile) throws FileNotFoundException {
        mBufferedSink = Okio.buffer(Okio.sink(outputFile));
    }


    public void setProgressCallback(@Nullable ProgressCallback progressCallback) {
        this.mProgressCallback = progressCallback;
    }

    public void write(ResponseBody responseBody) throws IOException {

        try (BufferedSource bufferedSource = responseBody.source()) {

            final byte[] dataBuffer = new byte[1024 * 1024];
            final long totalBytes = responseBody.contentLength();
            long loadedBytes = 0;
            int readedBytes;

            while ((readedBytes = bufferedSource.read(dataBuffer)) != -1) {
                loadedBytes += readedBytes;
                mBufferedSink.write(dataBuffer, 0, readedBytes);

                if (null != mProgressCallback)
                    mProgressCallback.onProgress(
                            loadedBytes,
                            totalBytes,
                            Math.round(1f * loadedBytes / totalBytes * 100.0));
            }

            mBufferedSink.close();
        }
    }


    @Override
    public void close() throws Exception {
        if (mBufferedSink.isOpen())
            mBufferedSink.close();
    }
}
