package com.github.aakumykov.app;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;

import com.github.aakumykov.app.databinding.ActivityDemoBinding;
import com.github.aakumykov.okhttp_file_downloader.OkHttpFileDownloader;
import com.github.aakumykov.okhttp_file_downloader.ProgressCallback;

import java.io.File;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class DemoActivity extends AppCompatActivity {

    private static final String TAG = DemoActivity.class.getSimpleName();
    private static final String KEY_URL = "URL";
    private ActivityDemoBinding mBinding;
    private ClipboardManager mClipboardManager;
    private final CompositeDisposable mCompositeDisposable = new CompositeDisposable();
    @Nullable private OkHttpFileDownloader mOkHttpFileDownloader;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBinding = ActivityDemoBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        if (null != savedInstanceState)
            mBinding.urlInput.setText(savedInstanceState.getString(KEY_URL));

        mBinding.downloadButton.setOnClickListener(v -> downloadImage());
        mBinding.cancelDownloadButton.setOnClickListener(v -> cancelDownloading());

        mBinding.clipboardButton.setOnClickListener(v -> {
            final String text = clipboardText().toString();
            if (!TextUtils.isEmpty(text))
                mBinding.urlInput.setText(text);
        });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_URL, mBinding.urlInput.getText().toString());
    }

    private void cancelDownloading() {
        if (null != mOkHttpFileDownloader)
            mOkHttpFileDownloader.interruptDownloading();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCompositeDisposable.dispose();
    }



    private void downloadImage() {

        final String imageUrl = mBinding.urlInput.getText().toString();
        if (TextUtils.isEmpty(imageUrl.trim())) {
            showError(R.string.ERROR_there_is_no_image_url);
            return;
        }

        Observable.create(new ObservableOnSubscribe<Double>() {
            @Override
            public void subscribe(ObservableEmitter<Double> emitter) throws Exception {

                final File targetFile = new File(getCacheDir(), "downloaded.file");

                mOkHttpFileDownloader = OkHttpFileDownloader.downloadFileTo(imageUrl, targetFile, new ProgressCallback() {
                    @Override
                    public void onProgress(double progressPercent) {
                        emitter.onNext(progressPercent);
                        Log.d(TAG, "onProgress: "+progressPercent);
                    }
                });
            }
        })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(new Observer<Double>() {
                                    @Override
                                    public void onSubscribe(Disposable d) {
                                        mCompositeDisposable.add(d);
                                        displayBusyState();
                                    }

                                    @Override
                                    public void onNext(Double aDouble) {
                                        mBinding.progressBar.setProgress((int) (aDouble * 100.0));
                                    }

                                    @Override
                                    public void onError(Throwable e) {
                                        displayErrorState(e);
                                    }

                                    @Override
                                    public void onComplete() {
                                        displayIdleState();
                                    }
                                });

        /*Flowable.create(new FlowableOnSubscribe<Double>() {
            @Override
            public void subscribe(FlowableEmitter<Double> emitter) throws Exception {

                final File targetFile = File.createTempFile("image_", "_file", getCacheDir());

                OkHttpFileDownloader.downloadFileTo(imageUrl, targetFile, new ProgressCallback() {
                    @Override
                    public void onProgress(double progressPercent) {
                        emitter.onNext(progressPercent);
                        Log.d(TAG, "onProgress: "+progressPercent);
                    }
                });
            }
        }, BackpressureStrategy.LATEST)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(new Subscriber<Double>() {
                                    @Override
                                    public void onSubscribe(Subscription s) {
                                        displayBusyState();
                                    }

                                    @Override
                                    public void onNext(Double aDouble) {
                                        mBinding.progressBar.setProgress((int) (aDouble * 100.0));
                                    }

                                    @Override
                                    public void onError(Throwable t) {
                                        displayErrorState(t);
                                    }

                                    @Override
                                    public void onComplete() {
                                        displayIdleState();
                                    }
                                });*/

        /*Single.create(new SingleOnSubscribe<File>() {
            @Override
            public void subscribe(SingleEmitter<File> emitter) throws Exception {

                final File targetFile = File.createTempFile("image_", "_file", getCacheDir());

                OkHttpFileDownloader.downloadFileTo(imageUrl, targetFile, new ProgressCallback() {
                    @Override
                    public void onProgress(double progressPercent) {
                        Log.d(TAG, "onProgress() called with: progress = [" + progressPercent + "]");
                    }
                });

                emitter.onSuccess(targetFile);
            }
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())

                .subscribe(new SingleObserver<File>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        mCompositeDisposable.add(d);
                        displayBusyState();
                    }

                    @Override
                    public void onSuccess(File file) {
                        displaySuccessState(file);
                    }

                    @Override
                    public void onError(Throwable e) {
                        displayErrorState(e);
                    }
                });*/

    }


    private void displayIdleState() {
        hideError();
        hideProgressBar();
        hideImage();
    }

    private void displayBusyState() {
        hideError();
        showProgressBar();
        hideImage();
    }

    private void displaySuccessState(File imageFile) {
        hideError();
        hideProgressBar();
        showImage(imageFile);
    }

    private void displayErrorState(Throwable t) {
        displayErrorState(t.getMessage());
        Log.e(TAG, t.getMessage(), t);
    }

    private void displayErrorState(String errorMsg) {
        showError(errorMsg);
        hideProgressBar();
        hideImage();
    }


    private void show(View view) {
        view.setVisibility(View.VISIBLE);
    }
    private void hide(View view) {
        view.setVisibility(View.GONE);
    }

    private void showProgressBar() {
        show(mBinding.progressBar);
    }
    private void hideProgressBar() {
        hide(mBinding.progressBar);
    }

    private void showImage(File imageFile) {
        Bitmap myBitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
        mBinding.imageView.setImageBitmap(myBitmap);
        show(mBinding.imageView);
    }
    private void hideImage() {
        hide(mBinding.imageView);
    }

    private void showError(String errorMsg) {
        mBinding.errorView.setText(errorMsg);
        show(mBinding.errorView);
    }
    private void showError(@StringRes int errorMsgRes) {
        showError(getString(errorMsgRes));
    }
    private void hideError() {
        hide(mBinding.errorView);
    }


    private CharSequence clipboardText() {

        if (null == mClipboardManager)
            mClipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

        @Nullable ClipData clipData = mClipboardManager.getPrimaryClip();
        return (null != clipData) ? clipData.getItemAt(0).getText() : "";
    }
}