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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;

import com.github.aakumykov.app.databinding.ActivityDemoBinding;
import com.github.aakumykov.okhttp_file_downloader.OkHttpFileDownloader;
import com.github.aakumykov.okhttp_file_downloader.ProgressCallback;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

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
    private final AtomicBoolean mDownloadingIsActive = new AtomicBoolean(false);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBinding = ActivityDemoBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        if (null != savedInstanceState)
            mBinding.urlInput.setText(savedInstanceState.getString(KEY_URL));

        mBinding.clearInputButton.setOnClickListener(v -> clearInputField());
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
            mOkHttpFileDownloader.cancelDownloading();
    }

    private void clearInputField() {
        mBinding.urlInput.setText("");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCompositeDisposable.dispose();
    }



    private void downloadImage() {

        if (mDownloadingIsActive.get()) {
            Toast.makeText(this, "Скачивание уже идёт", Toast.LENGTH_SHORT).show();
            return;
        }


        final String imageUrl = mBinding.urlInput.getText().toString();
        if (TextUtils.isEmpty(imageUrl.trim())) {
            showError(R.string.ERROR_there_is_no_image_url);
            return;
        }

        Observable.create(new ObservableOnSubscribe<Double>() {
            @Override
            public void subscribe(ObservableEmitter<Double> emitter) throws Exception {

                mDownloadingIsActive.set(true);

                final File targetFile = new File(getCacheDir(), "downloaded.file");

                mOkHttpFileDownloader = OkHttpFileDownloader.create(targetFile);

                mOkHttpFileDownloader.setProgressCallback(new ProgressCallback() {
                    @Override
                    public void onProgress(long bytes, long total, float percent) {
                        Log.d(TAG, "onProgress() called with: bytes = [" + bytes + "], total = [" + total + "], percent = [" + percent + "]");
                        mBinding.progressBar.setProgress((int) (percent));
                        mBinding.loadedBytesView.setText(ByteSizeConverter.humanReadableByteCountSI(bytes));
                    }

                    @Override
                    public void onComplete() {
                        emitter.onComplete();
                    }
                });

                mOkHttpFileDownloader.download(imageUrl);
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
//                                        mBinding.progressBar.setProgress((int) (aDouble * 100.0));
                                    }

                                    @Override
                                    public void onError(Throwable e) {
                                        displayErrorState(e);
                                    }

                                    @Override
                                    public void onComplete() {
                                        mDownloadingIsActive.set(false);
                                        mBinding.getRoot().postDelayed(() -> displayIdleState(), 1000);
                                    }
                                });
    }

    private void displayIdleState() {
        hideError();
        hideProgressWidgets();
        hideImage();
    }

    private void displayBusyState() {
        hideError();
        showProgressWidgets();
        hideImage();
    }

    private void displaySuccessState(File imageFile) {
        hideError();
        hideProgressWidgets();
        showImage(imageFile);
    }

    private void displayErrorState(Throwable t) {
        displayErrorState(t.getMessage());
        Log.e(TAG, t.getMessage(), t);
    }

    private void displayErrorState(String errorMsg) {
        showError(errorMsg);
        hideProgressWidgets();
        hideImage();
    }


    private void show(View view) {
        view.setVisibility(View.VISIBLE);
    }
    private void hide(View view) {
        view.setVisibility(View.GONE);
    }

    private void showProgressWidgets() {
        show(mBinding.progressBar);
        show(mBinding.loadedBytesView);
    }
    private void hideProgressWidgets() {
        mBinding.progressBar.setProgress(0);
        mBinding.loadedBytesView.setText("");
        hide(mBinding.progressBar);
        hide(mBinding.loadedBytesView);
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