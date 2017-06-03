package com.example.mac.cameragl.v2;

import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.lang.ref.WeakReference;

public class CameraHandler2 extends Handler {

    public static final int MSG_SET_SURFACE_TEXTURE = 0;
    private static String TAG = "CameraHandler";
    private WeakReference<MainActivity> mWeakHelper;

    public CameraHandler2(MainActivity activity) {
        mWeakHelper = new WeakReference<>(activity);
    }

    public void invalidateHandler() {
        mWeakHelper.clear();
    }

    @Override
    public void handleMessage(Message inputMessage) {
        int what = inputMessage.what;
        Log.d(TAG, "CameraHandler [" + this + "]: what=" + what);
        MainActivity activity = mWeakHelper.get();
        if (activity == null) {
            Log.w(TAG, "CameraHandler2.handleMessage: helper is null");
            return;
        }
        switch (what) {
            case MSG_SET_SURFACE_TEXTURE:
                activity.handleSetSurfaceTexture((SurfaceTexture) inputMessage.obj);
                break;
            default:
                throw new RuntimeException("unknown msg " + what);
        }
    }
}