package com.github.aakumykov.okhttp_file_downloader_demo;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.github.aakumykov.okhttp_file_downloader.OkHttpFileDownloader;
import com.github.aakumykov.okhttp_file_downloader.ProgressCallback;
import com.gitlab.aakumykov.exception_utils_module.ExceptionUtils;

import java.io.File;
import java.io.IOException;

public class FileDownloadingWorker extends Worker {

    public static final String TAG = FileDownloadingWorker.class.getSimpleName();

    public static final String SOURCE_URL = "SOURCE_URL";
    public static final String PROGRESS = "PROGRESS";
    public static final String DOWNLOADED_FILE_PATH = "DOWNLOADED_FILE_PATH";
    public static final String ERROR_MESSAGE = "ERROR_MESSAGE";

    private static final String TEMP_FILE_PREFIX = "downloaded_";
    private static final String TEMP_FILE_SUFFIX = ".file";

    private final Data.Builder mProgressDataBuilder;


    public FileDownloadingWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        mProgressDataBuilder = new Data.Builder();
    }


    @NonNull @Override
    public Result doWork() {

        try {
            final File targetFile = File.createTempFile(TEMP_FILE_PREFIX, TEMP_FILE_SUFFIX);
            OkHttpFileDownloader okHttpFileDownloader = OkHttpFileDownloader.create(targetFile);
            okHttpFileDownloader.setProgressCallback(new ProgressCallback() {
                @Override
                public void onProgress(long bytes, long total, float percent) {
                    setProgressAsync(progressData(percent));
                }

                @Override
                public void onComplete() {

                }
            });
            return Result.success(successData(targetFile));
        }
        catch (IOException e) {
            Log.e(TAG, ExceptionUtils.getErrorMessage(e), e);
            return Result.failure(errorData(e));
        }
    }

    private Data progressData(final float percent) {
        return mProgressDataBuilder
                .putFloat(PROGRESS, percent)
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
}
