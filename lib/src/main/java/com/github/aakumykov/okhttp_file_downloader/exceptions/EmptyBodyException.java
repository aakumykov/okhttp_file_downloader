package com.github.aakumykov.okhttp_file_downloader.exceptions;

public class EmptyBodyException extends OkHttpFileDownloaderException {
    public EmptyBodyException() {
        super("Response body is empty");
    }
}
