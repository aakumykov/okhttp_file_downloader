package com.github.aakumykov.okhttp_file_downloader;

public interface ProgressCallback {
    void onProgress(double progressPercent);
}