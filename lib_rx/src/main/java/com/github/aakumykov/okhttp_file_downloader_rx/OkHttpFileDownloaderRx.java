package com.github.aakumykov.okhttp_file_downloader_rx;

import androidx.annotation.NonNull;

import com.github.aakumykov.okhttp_file_downloader.OkHttpFileDownloader;
import com.github.aakumykov.okhttp_file_downloader.ProgressCallback;

import java.io.File;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class OkHttpFileDownloaderRx {

    public static Observable<DownloadingProgress> download(@NonNull final String sourceUrl,
                                                    @NonNull final File targetFile) {
        return Observable.create(new ObservableOnSubscribe<DownloadingProgress>() {
            @Override
            public void subscribe(ObservableEmitter<DownloadingProgress> emitter) throws Exception {

                final OkHttpFileDownloader okHttpFileDownloader = OkHttpFileDownloader.create();

                okHttpFileDownloader.setProgressCallback(new ProgressCallback() {
                    @Override
                    public void onProgress(long bytes, long total, float percent) {
                        emitter.onNext(new DownloadingProgress(bytes, total, percent));
                    }

                    @Override
                    public void onComplete() {
                        emitter.onComplete();
                    }
                });

                okHttpFileDownloader.download(sourceUrl, targetFile);
            }
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }
}
