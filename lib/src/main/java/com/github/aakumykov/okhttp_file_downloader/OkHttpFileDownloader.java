package com.github.aakumykov.okhttp_file_downloader;

import android.content.Context;

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
    private static final String TEMP_PREFIX = TAG+"_downloaded";
    private static final String TEMP_SUFFIX = ".file";

    private final OkHttpClient mOkHttpClient;
    private final OkHttpFileWriter mOkHttpFileWriter;

    @Nullable private Call mCall;


    public static OkHttpFileDownloader createDefault(Context context) throws IOException {
        return create(tempFileName(context));
    }

    public static OkHttpFileDownloader create(@NonNull String targetFilePath) throws IOException {
        return create(new File(targetFilePath));
    }

    public static OkHttpFileDownloader create(@NonNull File targetFile) throws IOException {
        final OkHttpClient okHttpClient = new OkHttpClient();
        final OkHttpFileWriter okHttpFileWriter = new OkHttpFileWriter(targetFile);
        return new OkHttpFileDownloader(okHttpClient, okHttpFileWriter);
    }

    private static String tempFileName(Context context) {
        return new File(context.getCacheDir(), TEMP_PREFIX + TEMP_SUFFIX)
                .getAbsolutePath();
    }

    public static File downloadFile(String sourceURL)
            throws IOException, BadResponseException, EmptyBodyException
    {
        final File tempFile = File.createTempFile(TEMP_PREFIX, TEMP_SUFFIX);
        downloadFileTo(sourceURL, tempFile, null);
        return tempFile;
    }


    public static OkHttpFileDownloader downloadFileTo(String sourceURL, File targetFile, @Nullable ProgressCallback progressCallback)
            throws IOException, BadResponseException, EmptyBodyException
    {
        final OkHttpClient okHttpClient = new OkHttpClient();
        final OkHttpFileWriter okHttpFileWriter = new OkHttpFileWriter(targetFile);

        if (null != progressCallback)
            okHttpFileWriter.setProgressCallback(progressCallback);

        OkHttpFileDownloader okHttpFileDownloader = new OkHttpFileDownloader(okHttpClient, okHttpFileWriter);

        okHttpFileDownloader.download(sourceURL);

        return okHttpFileDownloader;
    }


    public OkHttpFileDownloader(OkHttpClient okHttpClient, OkHttpFileWriter okHttpFileWriter) {
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
