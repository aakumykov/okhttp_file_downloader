package com.github.aakumykov.okhttp_file_downloader;

public interface ProgressCallback {
    void onProgress(long bytes, long total, float percent);
    void onComplete();
}