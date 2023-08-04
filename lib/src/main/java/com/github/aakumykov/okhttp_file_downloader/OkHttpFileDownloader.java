package com.github.aakumykov.okhttp_file_downloader;

import androidx.annotation.Nullable;

import com.github.aakumykov.okhttp_file_downloader.exceptions.BadResponseException;
import com.github.aakumykov.okhttp_file_downloader.exceptions.EmptyBodyException;

import java.io.File;
import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class OkHttpFileDownloader implements AutoCloseable {

    private static final String TAG = OkHttpFileDownloader.class.getSimpleName();
    private static final String TEMP_PREFIX = TAG+"_";
    private static final String TEMP_SUFFIX = "_"+TAG;

    private final OkHttpClient mOkHttpClient;
    private final OkHttpFileWriter mOkHttpFileWriter;

    @Nullable private Response mResponse;
    @Nullable private ResponseBody mResponseBody;


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

        final Request request = new Request.Builder().url(url).build();
        final Response response = mOkHttpClient.newCall(request).execute();

        if (!response.isSuccessful())
            throw new BadResponseException(response, url);

        final ResponseBody responseBody = response.body();

        if (null == responseBody)
            throw new EmptyBodyException();

        mOkHttpFileWriter.write(responseBody);
    }


    public void interruptDownloading() {
        if (null != mResponse)
            mResponse.close();
    }


    @Override
    public void close() throws Exception {
        mOkHttpFileWriter.close();
    }
}
