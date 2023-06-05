package com.github.aakumykov.okhttp_file_downloader.exceptions;

public abstract class OkHttpFileDownloaderException extends Exception {
    public OkHttpFileDownloaderException(String message) {
        super(message);
    }
}
