package com.example.mac.cameragl.v2;

import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import com.affectiva.android.affdex.sdk.Frame;
import com.example.mac.cameragl.trash.CameraUtils;

import java.io.IOException;

public class CameraHelper {

    private static String TAG = "CameraHelper";

    private Camera mCamera;
    private int mCameraPreviewWidth;
    private int mCameraPreviewHeight;
    private int mPreviewRotation;

    public Camera getCamera() {
        return mCamera;
    }

    public int getCorrectOrientation(WindowManager manager, Camera.CameraInfo info, Camera camera) {
        int rotation = manager.getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;

        }
        int result = 0;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;
        } else {
            result = (info.orientation - degrees + 360) % 360;
        }
        return result;
    }

    public Frame.ROTATE computeFrameRotation(int rotation) {
        switch (rotation) {
            case 0:
                return Frame.ROTATE.NO_ROTATION;
            case 90:
                return Frame.ROTATE.BY_90_CW;
            case 180:
                return Frame.ROTATE.BY_180;
            case 270:
                return Frame.ROTATE.BY_90_CCW;
            default:
                return Frame.ROTATE.NO_ROTATION;
        }
    }

    private Point getScreenSize(WindowManager manager) {
        Display display = manager.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size;
    }

    public void openCamera(WindowManager manager) {
        Point size = getScreenSize(manager);
        int desiredWidth = size.x;
        int desiredHeight = size.y;
        if (mCamera != null) {
            throw new RuntimeException("camera already initialized");
        }
        Camera.CameraInfo info = new Camera.CameraInfo();
        int numCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numCameras; i++) {
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                mCamera = Camera.open(i);
                break;
            }
        }
        if (mCamera == null) {
            Log.d(TAG, "No front-facing camera found; opening default");
            mCamera = Camera.open();    // opens first back-facing camera
        }
        if (mCamera == null) {
            throw new RuntimeException("Unable to open camera");
        }
        int orientation = getCorrectOrientation(manager, info, mCamera);
        Camera.Parameters parms = mCamera.getParameters();
        parms.setRotation(orientation);
        CameraUtils.choosePreviewSize(parms, desiredWidth, desiredHeight);
        parms.setRecordingHint(true);
        mCamera.setParameters(parms);
        mCamera.setDisplayOrientation(orientation);
        int[] fpsRange = new int[2];
        Camera.Size mCameraPreviewSize = parms.getPreviewSize();
        parms.getPreviewFpsRange(fpsRange);
        mCameraPreviewWidth = mCameraPreviewSize.height;
        mCameraPreviewHeight = mCameraPreviewSize.width;
        mPreviewRotation = orientation;
    }

    public int getPreviewRotation() {
        return mPreviewRotation;
    }

    public void releaseCamera() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
            Log.d(TAG, "releaseCamera -- done");
        }
    }

    public void handleSetSurfaceTexture(SurfaceTexture surfaceTexture) {
        try {
            mCamera.setPreviewTexture(surfaceTexture);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
        mCamera.startPreview();
    }

    public void setPreviewCallback(Camera.PreviewCallback callback) {
        mCamera.setPreviewCallback(callback);
    }

    public int getCameraPreviewWidth() {
        return mCameraPreviewWidth;
    }

    public int getCameraPreviewHeight() {
        return mCameraPreviewHeight;
    }
}
