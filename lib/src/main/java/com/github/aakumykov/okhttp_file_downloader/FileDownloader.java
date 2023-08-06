package com.github.aakumykov.okhttp_file_downloader;

import androidx.annotation.NonNull;

import com.github.aakumykov.okhttp_file_downloader.exceptions.BadResponseException;
import com.github.aakumykov.okhttp_file_downloader.exceptions.EmptyBodyException;

import java.io.File;
import java.io.IOException;

public interface FileDownloader {
    void download(@NonNull final String sourceUrl, @NonNull final File targetFile) throws EmptyBodyException, BadResponseException, IOException;
    void download(@NonNull final String sourceUrl) throws EmptyBodyException, IOException, BadResponseException;
    void setProgressCallback(ProgressCallback progressCallback);
    void stopDownloading();
}