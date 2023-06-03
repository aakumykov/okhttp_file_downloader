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

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;

import com.github.aakumykov.app.databinding.ActivityDemoBinding;
import com.github.aakumykov.okhttp_file_downloader.OkHttpFileDownloader;

import java.io.File;

import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleObserver;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class DemoActivity extends AppCompatActivity {

    private static final String TAG = DemoActivity.class.getSimpleName();
    private ActivityDemoBinding mBinding;
    private ClipboardManager mClipboardManager;
    private final CompositeDisposable mCompositeDisposable = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBinding = ActivityDemoBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        mBinding.downloadButton.setOnClickListener(v -> downloadImage());

        mBinding.clipboardButton.setOnClickListener(v -> {
            final String text = clipboardText().toString();
            if (!TextUtils.isEmpty(text))
                mBinding.urlInput.setText(text);
        });
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

        Single.create(new SingleOnSubscribe<File>() {
            @Override
            public void subscribe(SingleEmitter<File> emitter) throws Exception {
                final File targetFile = File.createTempFile("image_", "_file", getCacheDir());
                OkHttpFileDownloader.downloadFileTo(imageUrl, targetFile);
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
                });

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