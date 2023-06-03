package com.github.aakumykov.okhttp_file_downloader;

import com.github.aakumykov.okhttp_file_downloader.exceptions.BadResponseException;
import com.github.aakumykov.okhttp_file_downloader.exceptions.EmptyBodyException;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class OkHttpFileDownloader implements AutoCloseable {

    private final OkHttpClient mOkHttpClient;
    private final OkHttpFileWriter mOkHttpFileWriter;


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
