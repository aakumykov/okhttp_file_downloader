package com.github.aakumykov.app;

public class DownloadingProgress {

    public final long loadedBytes;
    public final long totalBytes;
    public final float percent;

    public DownloadingProgress(long loadedBytes, long totalBytes, float percent) {
        this.loadedBytes = loadedBytes;
        this.totalBytes = totalBytes;
        this.percent = percent;
    }
}
