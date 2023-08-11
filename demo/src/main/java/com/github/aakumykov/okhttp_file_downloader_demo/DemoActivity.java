package com.github.aakumykov.okhttp_file_downloader_demo;

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
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.github.aakumykov.enum_utils.EnumUtils;
import com.github.aakumykov.okhttp_file_downloader.OkHttpFileDownloader;
import com.github.aakumykov.okhttp_file_downloader.ProgressCallback;
import com.github.aakumykov.okhttp_file_downloader_demo.databinding.ActivityDemoBinding;
import com.github.abdularis.buttonprogress.DownloadButtonProgress;

import java.io.File;
import java.util.UUID;
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
    @Nullable
    private OkHttpFileDownloader mOkHttpFileDownloader;
    private final AtomicBoolean mDownloadingIsActive = new AtomicBoolean(false);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBinding = ActivityDemoBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        EditTextValuePersistingHelper editTextValuePersistingHelper = new EditTextValuePersistingHelper(this);
        mBinding.urlInput.setText(editTextValuePersistingHelper.getText(KEY_URL));
        editTextValuePersistingHelper.addFieldToPersistText(KEY_URL, mBinding.urlInput);

        mBinding.clearInputButton.setOnClickListener(v -> clearInputField());
        mBinding.downloadButton.setOnClickListener(v -> downloadFile(false));
        mBinding.downloadWithWorkerButton.setOnClickListener(v -> downloadFile(true));
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
            mOkHttpFileDownloader.stopDownloading();
    }

    private void clearInputField() {
        mBinding.urlInput.setText("");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCompositeDisposable.dispose();
    }

    private void downloadFile(boolean withWorker) {

        if (mDownloadingIsActive.get()) {
            Toast.makeText(this, "Скачивание уже идёт", Toast.LENGTH_SHORT).show();
            return;
        }

        final String sourceUrl = mBinding.urlInput.getText().toString();
        if (TextUtils.isEmpty(sourceUrl.trim())) {
            showError(R.string.ERROR_there_is_no_image_url);
            return;
        }

        if (withWorker)
            downloadWithWorker();
        else
            downloadFileNew(sourceUrl);
    }


    private void downloadWithWorker() {

        final String sourceURL = mBinding.urlInput.getText().toString();

        final Data inputData = new Data.Builder()
                .putString(FileDownloadingWorker.SOURCE_URL, sourceURL)
                .build();

        OneTimeWorkRequest oneTimeWorkRequest =
                new OneTimeWorkRequest.Builder(FileDownloadingWorker.class)
                    .setInputData(inputData)
                    .build();

        final UUID workId = oneTimeWorkRequest.getId();

        final WorkManager wm = WorkManager.getInstance(this);

        wm.enqueueUniqueWork(
                sourceURL,
                ExistingWorkPolicy.KEEP,
                oneTimeWorkRequest)
                /*.getState().observe(this, new androidx.lifecycle.Observer<Operation.State>() {
                    @Override
                    public void onChanged(Operation.State state) {
                        switch (workStateToEnum(state)) {
                            case SUCCESS:
                                break;
                            case PROGRESS:
                                break;
                            case ERROR:
                                break;
                            default:
                                EnumUtils
                        }
                    }
                })*/;

        wm.getWorkInfoByIdLiveData(workId).observe(this, new androidx.lifecycle.Observer<WorkInfo>() {
            @Override
            public void onChanged(WorkInfo workInfo) {

                if (null == workInfo) {
                    Log.d(TAG, "onChanged: NULL");
                    return;
                }

                final Data progressData = workInfo.getProgress();
                final float percent = progressData.getFloat(FileDownloadingWorker.PROGRESS, 0f);
                mBinding.progressBar.setCurrentProgress(floatPercentsToProgress(percent));

                final WorkInfo.State workInfoState = workInfo.getState();

                Log.d(TAG, "workInfoState: "+workInfoState+", percent: "+percent);

                switch (workInfoState) {
                    case ENQUEUED:
                    case BLOCKED:
                    case CANCELLED:
                    case SUCCEEDED:
                        displayIdleState();
                        break;
                    case RUNNING:
                        displayBusyState();
                        break;
                    case FAILED:
                        final Data errorData = workInfo.getOutputData();
                        final String errorMessage = errorData.getString(FileDownloadingWorker.ERROR_MESSAGE);
                        displayErrorState(errorMessage);
                        Log.e(TAG, "Ошибка скачивания файла:"+errorMessage);
                        break;
                    default:
                        EnumUtils.throwUnknownValue(workInfoState);
                }
            }
        });
    }


    private void downloadFileNew(final String sourceUrl) {

        Observable.create(new ObservableOnSubscribe<DownloadingProgress>() {
                    @Override
                    public void subscribe(ObservableEmitter<DownloadingProgress> emitter) throws Exception {

                        mDownloadingIsActive.set(true);

                        final File targetFile = new File(getCacheDir(), "downloaded.file");

                        mOkHttpFileDownloader = OkHttpFileDownloader.create();

                        mOkHttpFileDownloader.setProgressCallback(new ProgressCallback() {
                            @Override
                            public void onProgress(long bytes, long total, float percent) {
                                Log.d(TAG, "onProgress() called with: bytes = [" + bytes + "], total = [" + total + "], percent = [" + percent + "]");
                                emitter.onNext(new DownloadingProgress(bytes, total, percent));
                            }

                            @Override
                            public void onComplete() {
                                emitter.onComplete();
                            }
                        });

                        DownloadingProgress.resetCounter();

                        mOkHttpFileDownloader.download(sourceUrl);
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())

                .subscribe(new Observer<DownloadingProgress>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        mCompositeDisposable.add(d);
                        displayBusyState();
                    }

                    @Override
                    public void onNext(DownloadingProgress downloadingProgress) {
                        mBinding.progressBar.setCurrentProgress(floatPercentsToProgress(downloadingProgress.percent));
                        mBinding.loadedBytesView.setText(ByteSizeConverter.humanReadableByteCountSI(downloadingProgress.loadedBytes));
                    }

                    @Override
                    public void onError(Throwable e) {
                        mDownloadingIsActive.set(false);
                        displayErrorState(e);
                    }

                    @Override
                    public void onComplete() {
                        mDownloadingIsActive.set(false);
                        mBinding.getRoot().postDelayed(() -> displayIdleState(), 1000);

                        final int count = DownloadingProgress.counter;
                        Toast.makeText(DemoActivity.this, "count="+count, Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "count="+count);
                    }
                });
    }


    private void displayIdleState() {
        hideError();
//        hideProgressWidgets();
        changeProgressToIdle();
        hideImage();
    }

    private void changeProgressToIdle() {
        DownloadButtonProgress progressBar = mBinding.progressBar;
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

    private void displayErrorState(@NonNull Throwable t) {
        displayErrorState(t.getMessage());
        Log.e(TAG, t.getMessage(), t);
    }

    private void displayErrorState(String errorMsg) {
        showError(errorMsg);
        hideProgressWidgets();
        hideImage();
    }


    private void show(@NonNull View view) {
        view.setVisibility(View.VISIBLE);
    }
    private void hide(@NonNull View view) {
        view.setVisibility(View.GONE);
    }

    private void showProgressWidgets() {
        show(mBinding.progressBar);
        show(mBinding.loadedBytesView);
    }
    private void hideProgressWidgets() {
        mBinding.progressBar.setCurrentProgress(0);
        mBinding.loadedBytesView.setText("");
        hide(mBinding.progressBar);
        hide(mBinding.loadedBytesView);
    }

    private void showImage(@NonNull File imageFile) {
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

    private enum WorkState { PROGRESS, SUCCESS, ERROR }

    private int floatPercentsToProgress(float percent) {
        return (int) (percent * 100.0);
    }
}