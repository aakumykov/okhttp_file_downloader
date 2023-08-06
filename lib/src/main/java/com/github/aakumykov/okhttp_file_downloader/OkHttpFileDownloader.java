package com.github.aakumykov.okhttp_file_downloader;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.aakumykov.okhttp_file_downloader.exceptions.BadResponseException;
import com.github.aakumykov.okhttp_file_downloader.exceptions.EmptyBodyException;

import org.jetbrains.annotations.Contract;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class OkHttpFileDownloader implements FileDownloader, AutoCloseable {

    private static final String TAG = OkHttpFileDownloader.class.getSimpleName();
    private static final String TEMP_FILE_PREFIX = TAG + "_";
    private static final String TEMP_FILE_SUFFIX = ".downloaded";

    @Nullable private OkHttpClient mOkHttpClient;
    @Nullable private OkHttpFileWriter mOkHttpFileWriter;
    @Nullable private Call mCall;
    /* Это поле используется как "хранилище", коллбек реально вызывается во Writer-е,
       куда передается перед вызовом его метода write().*/
    @Nullable private ProgressCallback mProgressCallback;


    @NonNull @Contract(value = " -> new", pure = true)
    public static OkHttpFileDownloader create() {
        return new OkHttpFileDownloader();
    }



    @Override
    public void download(@NonNull String sourceUrl, @NonNull File targetFile) throws
            EmptyBodyException, BadResponseException, IOException
    {
        mOkHttpClient = okHttpClient();
        mOkHttpFileWriter = okHttpFileWriter(targetFile);
        startDownloading(sourceUrl);
    }

    @Override
    public void download(@NonNull String sourceUrl) throws
            IOException, EmptyBodyException, BadResponseException {
        download(sourceUrl, tempFile());
    }

    @Override
    public void setProgressCallback(@Nullable ProgressCallback progressCallback) {
        mProgressCallback = progressCallback;
    }


    @Override
    public void stopDownloading() {
        if (null != mCall)
            mCall.cancel();
    }


    public OkHttpFileDownloader() {

    }

    // FIXME: не могу использовать этот конструктор, потому что от моей реализации зависит, будет ли вызван ProgressCallback.
    private OkHttpFileDownloader(@NonNull OkHttpClient okHttpClient, @NonNull OkHttpFileWriter okHttpFileWriter) {
        mOkHttpClient = okHttpClient;
        mOkHttpFileWriter = okHttpFileWriter;
    }


    @Override
    public void close() throws Exception {
        mOkHttpFileWriter.close();
    }


    private void startDownloading(String url) throws IOException, BadResponseException, EmptyBodyException {

        if (null == mOkHttpClient)
            throw new IllegalStateException("OkHttpClient field is null.");

        if (null == mOkHttpFileWriter)
            throw new IllegalStateException("OkHttpFileWriter field is null.");


        mCall = mOkHttpClient.newCall(new Request.Builder().url(url).build());

        final Response response = mCall.execute();
        if (!response.isSuccessful())
            throw new BadResponseException(response, url);

        final ResponseBody responseBody = response.body();
        if (null == responseBody)
            throw new EmptyBodyException();

        mOkHttpFileWriter.setProgressCallback(mProgressCallback);
        mOkHttpFileWriter.write(responseBody);
    }

    private OkHttpClient okHttpClient() {
        return new OkHttpClient();
    }

    private OkHttpFileWriter okHttpFileWriter(@NonNull final File targetFile) throws FileNotFoundException {
        return new OkHttpFileWriter(targetFile);
    }

    private File tempFile() throws IOException{
        return File.createTempFile(TEMP_FILE_PREFIX, TEMP_FILE_SUFFIX);
    }
}
