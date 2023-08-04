package com.github.aakumykov.okhttp_file_downloader;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.aakumykov.okhttp_file_downloader.exceptions.BadResponseException;
import com.github.aakumykov.okhttp_file_downloader.exceptions.EmptyBodyException;

import java.io.File;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class OkHttpFileDownloader implements AutoCloseable {

    private static final String TAG = OkHttpFileDownloader.class.getSimpleName();
    private final OkHttpClient mOkHttpClient;
    private final OkHttpFileWriter mOkHttpFileWriter;
    @Nullable private Call mCall;


    public static OkHttpFileDownloader create(@NonNull File targetFile) throws IOException {
        return createReal(targetFile, null);
    }

    /*public static OkHttpFileDownloader create(@NonNull File targetFile,
                                              @NonNull ProgressCallback progressCallback) throws IOException {
        return createReal(targetFile, progressCallback);
    }*/

    private static OkHttpFileDownloader createReal(@NonNull File targetFile,
                                                   @Nullable ProgressCallback progressCallback) throws IOException {
        final OkHttpClient okHttpClient = new OkHttpClient();
        final OkHttpFileWriter okHttpFileWriter = new OkHttpFileWriter(targetFile);
        if (null != progressCallback)
            okHttpFileWriter.setProgressCallback(progressCallback);
        return new OkHttpFileDownloader(okHttpClient, okHttpFileWriter);
    }

    public OkHttpFileDownloader(OkHttpClient okHttpClient,
                                OkHttpFileWriter okHttpFileWriter) {
        mOkHttpClient = okHttpClient;
        mOkHttpFileWriter = okHttpFileWriter;
    }


    public void download(String url) throws IOException, BadResponseException, EmptyBodyException {

        mCall = mOkHttpClient.newCall(new Request.Builder().url(url).build());

        final Response response = mCall.execute();

        if (!response.isSuccessful())
            throw new BadResponseException(response, url);

        final ResponseBody responseBody = response.body();

        if (null == responseBody)
            throw new EmptyBodyException();

        mOkHttpFileWriter.write(responseBody);
    }


    public void cancelDownloading() {
        if (null != mCall && !mCall.isCanceled())
            mCall.cancel();
    }


    @Override
    public void close() throws Exception {
        mOkHttpFileWriter.close();
    }


    public void setProgressCallback(@NonNull ProgressCallback progressCallback) {
        mOkHttpFileWriter.setProgressCallback(progressCallback);
    }
}
