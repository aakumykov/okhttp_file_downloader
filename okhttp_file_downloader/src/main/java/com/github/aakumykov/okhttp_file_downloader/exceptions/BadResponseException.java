package com.github.aakumykov.okhttp_file_downloader.exceptions;

import okhttp3.Response;

public class BadResponseException extends OkHttpFileDownloaderException {

    private final String mRequestURL;

    public BadResponseException(Response response, String requestUrl) {
        super(response.code()+": "+response.message());
        mRequestURL = requestUrl;
    }

    public String getRequestURL() {
        return mRequestURL;
    }
}
