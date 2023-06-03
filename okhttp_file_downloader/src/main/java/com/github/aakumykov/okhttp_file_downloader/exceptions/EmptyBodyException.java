package com.github.aakumykov.okhttp_file_downloader.exceptions;

public class EmptyBodyException extends Exception {
    public EmptyBodyException() {
        super("Response body is empty");
    }
}
