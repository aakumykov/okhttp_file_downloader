package com.github.aakumykov.okhttp_file_downloader;

import com.github.aakumykov.okhttp_file_downloader.exceptions.BadResponseException;
import com.github.aakumykov.okhttp_file_downloader.exceptions.EmptyBodyException;

import java.io.File;
import java.io.IOException;

import okhttp3.OkHttpClient;

public class OkHttpDownloadingHelper {

    private static final String TEMP_PREFIX = "temp_";
    private static final String TEMP_SUFFIX = "_temp";

    public File downloadFile(String sourceURL)
            throws IOException, BadResponseException, EmptyBodyException
    {
        final File tempFile = File.createTempFile(TEMP_PREFIX, TEMP_SUFFIX);
        downloadFileTo(sourceURL, tempFile);
        return tempFile;
    }

    public void downloadFileTo(String sourceURL, File targetFile)
            throws IOException, BadResponseException, EmptyBodyException
    {
        final OkHttpClient okHttpClient = new OkHttpClient();
        final OkHttpFileWriter okHttpFileWriter = new OkHttpFileWriter(targetFile);

        new OkHttpFileDownloader(okHttpClient, okHttpFileWriter).download(sourceURL);
    }
}
