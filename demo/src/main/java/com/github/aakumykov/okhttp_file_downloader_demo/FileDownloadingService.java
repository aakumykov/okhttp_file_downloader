package com.github.aakumykov.okhttp_file_downloader_demo;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.aakumykov.okhttp_file_downloader.OkHttpFileDownloader;
import com.github.aakumykov.okhttp_file_downloader.ProgressCallback;
import com.github.aakumykov.okhttp_file_downloader.exceptions.BadResponseException;
import com.github.aakumykov.okhttp_file_downloader.exceptions.EmptyBodyException;

import java.io.File;
import java.io.IOException;

import io.reactivex.disposables.CompositeDisposable;

public class FileDownloadingService extends Service {

    private static final String TAG = FileDownloadingService.class.getSimpleName();
    private final CompositeDisposable mCompositeDisposable = new CompositeDisposable();
    @Nullable private OkHttpFileDownloader mOkHttpFileDownloader;

    public static Intent intent(Context context) {
        return new Intent(context, FileDownloadingService.class);
    }


    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate() called");

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy() called");
    }


    @Nullable @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind() called with: intent = [" + intent + "]");
        return new Binder(this);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand() called with: intent = [" + intent + "], flags = [" + flags + "], startId = [" + startId + "]");
        return super.onStartCommand(intent, flags, startId);
    }


    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnbind() called with: intent = [" + intent + "]");
        return true;
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
        Log.d(TAG, "onRebind() called with: intent = [" + intent + "]");
    }

    public void downloadFile(@NonNull final String sourceUrl, @NonNull final File targetFile)
            throws IOException, EmptyBodyException, BadResponseException
    {
        mOkHttpFileDownloader = OkHttpFileDownloader.create(targetFile);
        mOkHttpFileDownloader.download(sourceUrl);
    }


    void stopWork() {
        if (null != mOkHttpFileDownloader)
            mOkHttpFileDownloader.cancelDownloading();
    }


    public void setProgressCallback(@NonNull final ProgressCallback progressCallback) {
        if (null != mOkHttpFileDownloader)
            mOkHttpFileDownloader.setProgressCallback(progressCallback);
    }


    public static class Binder extends android.os.Binder {
        private FileDownloadingService mService;
        public Binder(FileDownloadingService service) {
            mService = service;
        }
        public FileDownloadingService getService() {
            return mService;
        }
        public void release() {
            mService = null;
        }
    }
}
