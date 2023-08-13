package com.github.aakumykov.okhttp_file_downloader_demo;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.github.aakumykov.okhttp_file_downloader.OkHttpFileDownloader;
import com.github.aakumykov.okhttp_file_downloader.ProgressCallback;
import com.github.aakumykov.okhttp_file_downloader.exceptions.BadResponseException;
import com.github.aakumykov.okhttp_file_downloader.exceptions.EmptyBodyException;
import com.gitlab.aakumykov.exception_utils_module.ExceptionUtils;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class FileDownloadingWorker extends Worker {

    public static final String TAG = FileDownloadingWorker.class.getSimpleName();

    public static final String SOURCE_URL = "SOURCE_URL";
    public static final String PROGRESS = "PROGRESS";
    public static final String LOADED_BYTES = "LOADED_BYTES";
    public static final String TOTAL_BYTES = "TOTAL_BYTES";
    public static final String DOWNLOADED_FILE_PATH = "DOWNLOADED_FILE_PATH";
    public static final String ERROR_MESSAGE = "ERROR_MESSAGE";

    private static final String TEMP_FILE_PREFIX = "downloaded_";
    private static final String TEMP_FILE_SUFFIX = ".file";

    private final Data.Builder mProgressDataBuilder;
    @Nullable private Timer mTimer;
    private long mLoadedBytes = 0L;
    private long mTotalBytes = 0L;
    private float mProgressPercent = 0f;


    public FileDownloadingWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        mProgressDataBuilder = new Data.Builder();
    }


    @NonNull @Override
    public Result doWork() {

        final String sourceURL = getInputData().getString(SOURCE_URL);

        if (null == sourceURL) {
            final String errorMsg = "Source url is null";
            return Result.failure(errorData(new IllegalArgumentException(errorMsg)));
        }

        try {
            mTimer = new Timer();
            mTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    setProgressAsync(progressData(mProgressPercent,mLoadedBytes,mTotalBytes));
                }
            }, 0, 1000);

            final File targetFile = File.createTempFile(TEMP_FILE_PREFIX, TEMP_FILE_SUFFIX);

            OkHttpFileDownloader okHttpFileDownloader = OkHttpFileDownloader.create();

            okHttpFileDownloader.setProgressCallback(new ProgressCallback() {
                @Override
                public void onProgress(long bytes, long total, float percent) {
                    mProgressPercent = percent;
                    mLoadedBytes = bytes;
                    mTotalBytes = total;
                }

                @Override
                public void onComplete() {

                }
            });

            okHttpFileDownloader.download(sourceURL);

            return Result.success(successData(targetFile));
        }
        catch (IOException | BadResponseException | EmptyBodyException e) {
            Log.e(TAG, ExceptionUtils.getErrorMessage(e), e);
            return Result.failure(errorData(e));
        }
    }

    private Data progressData(final float percent, final long loadedBytes, final long totalBytes) {
        Log.d(TAG, "progressData() called with: percent = [" + percent + "], loadedBytes = [" + loadedBytes + "], totalBytes = [" + totalBytes + "]");
        return mProgressDataBuilder
                .putFloat(PROGRESS, percent)
                .putLong(LOADED_BYTES, loadedBytes)
                .putLong(TOTAL_BYTES, totalBytes)
                .build();
    }

    private Data successData(final File targetFile) {
        return new Data.Builder()
                .putString(DOWNLOADED_FILE_PATH, targetFile.getAbsolutePath())
                .build();
    }

    private Data errorData(final Exception e) {
        return new Data.Builder()
                .putString(ERROR_MESSAGE, ExceptionUtils.getErrorMessage(e))
                .build();
    }

    @Override
    public void onStopped() {
        super.onStopped();
        if (null != mTimer)
            mTimer.purge();
    }
}
