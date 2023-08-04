package com.github.aakumykov.app;

public class DownloadingProgress {

    private final static Object syncObject = new Object();
    public static int counter = 0;

    public final long loadedBytes;
    public final long totalBytes;
    public final float percent;

    public DownloadingProgress(long loadedBytes, long totalBytes, float percent) {
        this.loadedBytes = loadedBytes;
        this.totalBytes = totalBytes;
        this.percent = percent;

        synchronized (syncObject) {
            counter++;
        }
    }

    public static void resetCounter() {
        synchronized (syncObject) {
            counter = 0;
        }
    }
}
