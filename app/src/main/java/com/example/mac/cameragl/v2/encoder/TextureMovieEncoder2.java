package com.example.mac.cameragl.v2.encoder;

import android.opengl.EGLContext;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.File;
import java.lang.ref.WeakReference;

public class TextureMovieEncoder2 implements Runnable {

    static final int MSG_START_RECORDING = 0;
    static final int MSG_STOP_RECORDING = 1;
    static final int MSG_FRAME_AVAILABLE = 2;
    static final int MSG_SET_TEXTURE_ID = 3;
    static final int MSG_UPDATE_SHARED_CONTEXT = 4;
    static final int MSG_QUIT = 5;
    private static String TAG = "TextureMovieEncoder2";

    private Object mReadyFence = new Object();      // guards ready/running
    private EncoderHandler mHandler;
    private boolean mReady;
    private boolean mRunning;

    @Override
    public void run() {
        Looper.prepare();
        synchronized (mReadyFence) {
            mHandler = new TextureMovieEncoder2.EncoderHandler(this);
            mReady = true;
            mReadyFence.notify();
        }
        Looper.loop();

        Log.d(TAG, "Encoder thread exiting");
        synchronized (mReadyFence) {
            mReady = mRunning = false;
            mHandler = null;
        }
    }

    private void handleStartRecording(TextureMovieEncoder2.EncoderConfig config) {
        Log.d(TAG, "handleStartRecording " + config);
    }

    private void handleStopRecording() {
        Log.d(TAG, "handleStopRecording");
    }

    private void handleFrameAvailable(float[] transform, long timestampNanos) {

    }

    public void handleSetTexture(int id) {

    }

    private void handleUpdateSharedContext(EGLContext newSharedContext) {

    }

    public static class EncoderConfig {
        final File mOutputFile;
        final int mWidth;
        final int mHeight;
        final int mBitRate;
        final EGLContext mEglContext;

        public EncoderConfig(File outputFile, int width, int height, int bitRate,
                             EGLContext sharedEglContext) {
            mOutputFile = outputFile;
            mWidth = width;
            mHeight = height;
            mBitRate = bitRate;
            mEglContext = sharedEglContext;
        }

        @Override
        public String toString() {
            return "EncoderConfig: " + mWidth + "x" + mHeight + " @" + mBitRate +
                    " to '" + mOutputFile.toString() + "' ctxt=" + mEglContext;
        }
    }

    private static class EncoderHandler extends Handler {

        private WeakReference<TextureMovieEncoder2> mWeakEncoder;

        public EncoderHandler(TextureMovieEncoder2 encoder) {
            mWeakEncoder = new WeakReference<>(encoder);
        }

        @Override
        public void handleMessage(Message inputMessage) {
            int what = inputMessage.what;
            Object obj = inputMessage.obj;

            TextureMovieEncoder2 encoder = mWeakEncoder.get();
            if (encoder == null) {
                Log.w(TAG, "EncoderHandler.handleMessage: encoder is null");
                return;
            }
            switch (what) {
                case MSG_START_RECORDING:
                    encoder.handleStartRecording((TextureMovieEncoder2.EncoderConfig) obj);
                    break;
                case MSG_STOP_RECORDING:
                    encoder.handleStopRecording();
                    break;
                case MSG_FRAME_AVAILABLE:
                    long timestamp = (((long) inputMessage.arg1) << 32) |
                            (((long) inputMessage.arg2) & 0xffffffffL);
                    encoder.handleFrameAvailable((float[]) obj, timestamp);
                    break;
                case MSG_SET_TEXTURE_ID:
                    encoder.handleSetTexture(inputMessage.arg1);
                    break;
                case MSG_UPDATE_SHARED_CONTEXT:
                    encoder.handleUpdateSharedContext((EGLContext) inputMessage.obj);
                    break;
                case MSG_QUIT:
                    Looper.myLooper().quit();
                    break;
                default:
                    throw new RuntimeException("Unhandled msg what=" + what);
            }
        }
    }

}
