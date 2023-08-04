package com.github.aakumykov.okhttp_file_downloader;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import okhttp3.ResponseBody;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;

public class OkHttpFileWriter implements AutoCloseable {

    private final BufferedSink mBufferedSink;
    @Nullable private ProgressCallback mProgressCallback;

    public OkHttpFileWriter(@NonNull String outputFilePath) throws IOException {
        mBufferedSink = Okio.buffer(Okio.sink(new File(outputFilePath)));
    }

    public OkHttpFileWriter(File outputFile) throws FileNotFoundException {
        mBufferedSink = Okio.buffer(Okio.sink(outputFile));
    }

    public OkHttpFileWriter(FileOutputStream fileOutputStream) {
        mBufferedSink = Okio.buffer(Okio.sink(fileOutputStream));
    }

    public void setProgressCallback(@Nullable ProgressCallback progressCallback) {
        this.mProgressCallback = progressCallback;
    }

    public void write(ResponseBody responseBody) throws IOException {

//        mBufferedSink.writeAll(responseBody.source());

        try (BufferedSource bufferedSource = responseBody.source()) {

            byte[] dataBuffer = new byte[1024];
            int readBytes;
            long totalBytes = 0;

            while ((readBytes = bufferedSource.read(dataBuffer)) != -1) {
                totalBytes += readBytes;
                mBufferedSink.write(dataBuffer, 0, readBytes);

                if (null != mProgressCallback) {
                    double progress = Math.round(1f * totalBytes / responseBody.contentLength() * 100.0) / 100.0;
                    mProgressCallback.onProgress(progress);
                }
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
