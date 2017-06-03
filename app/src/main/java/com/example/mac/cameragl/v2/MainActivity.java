package com.example.mac.cameragl.v2;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.affectiva.android.affdex.sdk.Frame;
import com.affectiva.android.affdex.sdk.detector.Face;
import com.example.mac.cameragl.R;
import com.example.mac.cameragl.trash.AspectFrameLayout;
import com.example.mac.cameragl.trash.TextureMovieEncoder;

import java.io.ByteArrayOutputStream;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SurfaceTexture.OnFrameAvailableListener {

    private static String TAG = "MainActivity";
    private static TextureMovieEncoder sVideoEncoder = new TextureMovieEncoder();
    final float epsilon = .01f;
    AsyncFrameDetector asyncDetector;
    float lastTimestamp = -1f;
    long firstFrameTime = -1;
    private AspectFrameLayout mAspectFrameLayout;
    private GLSurfaceView mCameraPreview;
    private CameraPreviewSurfaceRenderer mRenderer;
    private CameraHandler2 mCameraHandler;
    private CameraHelper mCameraHelper;

    static Frame createFrameFromData(byte[] frameData, int width, int height, Frame.ROTATE rotation) {
        Frame.ByteArrayFrame frame = new Frame.ByteArrayFrame(frameData, width, height, Frame.COLOR_FORMAT.YUV_NV21);
        frame.setTargetRotation(rotation);
        return frame;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViews();
        initViews();
        asyncDetector = new AsyncFrameDetector(this);
        asyncDetector.setOnDetectorEventListener(new AsyncFrameDetector.OnDetectorEventListener() {
            @Override
            public void onImageResults(List<Face> faces, Frame image, float timeStamp) {
                if(faces.size() > 0){
                    float smile = faces.get(0).expressions.getSmile();
                    Log.d(TAG, "smile: " + smile);
                }
            }

            @Override
            public void onDetectorStarted() {

            }
        });
    }

    private void findViews() {
        mAspectFrameLayout = (AspectFrameLayout) findViewById(R.id.camera_preview_container);
        mCameraPreview = (GLSurfaceView) findViewById(R.id.camera_preview);
    }

    private void initViews() {
        mCameraHelper = new CameraHelper();
        mCameraHandler = new CameraHandler2(this);
        mRenderer = new CameraPreviewSurfaceRenderer(mCameraHandler);
        mCameraPreview.setEGLContextClientVersion(2);
        mCameraPreview.setRenderer(mRenderer);
        mCameraPreview.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCameraHelper.openCamera(getWindowManager());
        asyncDetector.reset();
        final int width = mCameraHelper.getCameraPreviewWidth();
        final int height = mCameraHelper.getCameraPreviewHeight();
        mAspectFrameLayout.setAspectRatio((double) width / height);
        mCameraPreview.onResume();
        mCameraPreview.queueEvent(new Runnable() {
            @Override
            public void run() {
                mRenderer.setCameraPreviewSize(width, height);
            }
        });
        asyncDetector.start();
    }

    public void handleSetSurfaceTexture(SurfaceTexture surfaceTexture) {
        mCameraHelper.setPreviewCallback(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                float timestamp = 0;
                long currentTime = SystemClock.elapsedRealtime();
                if (firstFrameTime == -1) {
                    firstFrameTime = currentTime;
                } else {
                    timestamp = (currentTime - firstFrameTime) / 1000f;
                }
                if (timestamp > (lastTimestamp + epsilon)) {
                    lastTimestamp = timestamp;
                    int width = mCameraHelper.getCameraPreviewWidth();
                    int height = mCameraHelper.getCameraPreviewHeight();
                    Frame.ROTATE rotation = mCameraHelper.computeFrameRotation(mCameraHelper.getPreviewRotation());
                    asyncDetector.process(createFrameFromData(data, height, width, Frame.ROTATE.BY_90_CCW), timestamp);
                }
            }
        });
        surfaceTexture.setOnFrameAvailableListener(this);
        mCameraHelper.handleSetSurfaceTexture(surfaceTexture);
    }

    Bitmap getBitmap(byte[] data, int width, int height, int previewFormat) {
        YuvImage yuv = new YuvImage(data, previewFormat, width, height, null);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuv.compressToJpeg(new Rect(0, 0, width, height), 50, out);

        byte[] bytes = out.toByteArray();
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        mCameraPreview.requestRender();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCameraHelper.releaseCamera();
        mCameraPreview.queueEvent(new Runnable() {
            @Override
            public void run() {
                mRenderer.notifyPausing();
            }
        });
        mCameraPreview.onPause();
        asyncDetector.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCameraHandler.invalidateHandler();
    }
}
