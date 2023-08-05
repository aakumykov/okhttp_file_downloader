package com.github.aakumykov.okhttp_file_downloader_demo;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

public class FileDownloadingService extends Service {

    private static final String TAG = FileDownloadingService.class.getSimpleName();


    public static Intent intent(Context context) {
        return new Intent(context, FileDownloadingService.class);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate() called");
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
