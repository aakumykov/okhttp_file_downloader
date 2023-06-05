package com.github.aakumykov.okhttp_file_downloader;

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


    public static File downloadFile(String sourceURL)
            throws IOException, BadResponseException, EmptyBodyException
    {
        final File tempFile = File.createTempFile(TEMP_PREFIX, TEMP_SUFFIX);
        downloadFileTo(sourceURL, tempFile);
        return tempFile;
    }


    public static void downloadFileTo(String sourceURL, File targetFile)
            throws IOException, BadResponseException, EmptyBodyException
    {
        final OkHttpClient okHttpClient = new OkHttpClient();
        final OkHttpFileWriter okHttpFileWriter = new OkHttpFileWriter(targetFile);

        new OkHttpFileDownloader(okHttpClient, okHttpFileWriter).download(sourceURL);
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


    @Override
    public void close() throws Exception {
        mOkHttpFileWriter.close();
    }
}
