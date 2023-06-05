package com.github.aakumykov.okhttp_file_downloader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import okhttp3.ResponseBody;
import okio.BufferedSink;
import okio.Okio;

public class OkHttpFileWriter implements AutoCloseable {

    private final BufferedSink mBufferedSink;


    public OkHttpFileWriter(File outputFile) throws FileNotFoundException {
        mBufferedSink = Okio.buffer(Okio.sink(outputFile));
    }

    public OkHttpFileWriter(FileOutputStream fileOutputStream) {
        mBufferedSink = Okio.buffer(Okio.sink(fileOutputStream));
    }


    public void write(ResponseBody responseBody) throws IOException {
        mBufferedSink.writeAll(responseBody.source());
        mBufferedSink.close();
    }


    @Override
    public void close() throws Exception {
        if (mBufferedSink.isOpen())
            mBufferedSink.close();
    }
}
