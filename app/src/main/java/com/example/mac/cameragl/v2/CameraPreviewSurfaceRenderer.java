package com.example.mac.cameragl.v2;

import android.graphics.SurfaceTexture;
import android.opengl.GLSurfaceView;
import android.util.Log;

import com.example.mac.cameragl.trash.gles.FullFrameRect;
import com.example.mac.cameragl.trash.gles.Texture2dProgram;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static com.example.mac.cameragl.v2.CameraHandler2.MSG_SET_SURFACE_TEXTURE;

public class CameraPreviewSurfaceRenderer implements GLSurfaceView.Renderer {

    private static String TAG = "CameraSurfaceRenderer";
    private FullFrameRect mFullFrameRect;
    private int mTextureId;
    private SurfaceTexture mSurfaceTexture;
    private CameraHandler2 mCameraHandler;
    private final float[] mSTMatrix = new float[16];
    private int mIncomingWidth;
    private int mIncomingHeight;
    private boolean mIncomingSizeUpdated;

    public CameraPreviewSurfaceRenderer(CameraHandler2 cameraHandler) {
        mCameraHandler = cameraHandler;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.d(TAG, "onSurfaceCreated");
        mFullFrameRect = new FullFrameRect(
                new Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_EXT));
        mTextureId = mFullFrameRect.createTextureObject();
        mSurfaceTexture = new SurfaceTexture(mTextureId);

        // Tell the UI thread to enable the camera preview.
        mCameraHandler.sendMessage(mCameraHandler.obtainMessage(MSG_SET_SURFACE_TEXTURE, mSurfaceTexture));
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.d(TAG, "onSurfaceChanged");
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        mSurfaceTexture.updateTexImage();
        if (mIncomingSizeUpdated) {
            mFullFrameRect.getProgram().setTexSize(mIncomingWidth, mIncomingHeight);
            mIncomingSizeUpdated = false;
        }

        // Draw the video frame.
        mSurfaceTexture.getTransformMatrix(mSTMatrix);
        mFullFrameRect.drawFrame(mTextureId, mSTMatrix);
    }

    public void setCameraPreviewSize(int width, int height) {
        Log.d(TAG, "setCameraPreviewSize");
        mIncomingWidth = width;
        mIncomingHeight = height;
        mIncomingSizeUpdated = true;
    }

    public void notifyPausing() {
        if (mSurfaceTexture != null) {
            Log.d(TAG, "renderer pausing -- releasing SurfaceTexture");
            mSurfaceTexture.release();
            mSurfaceTexture = null;
        }
        if (mFullFrameRect != null) {
            mFullFrameRect.release(false);
            mFullFrameRect = null;
        }
        mIncomingWidth = mIncomingHeight = -1;
    }
}
