package com.autochips.android.backcar.view;

import android.util.AttributeSet;
import android.content.Context;

import android.util.Log;

import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
//import android.view.ViewGroup.LayoutParams;

import android.hardware.Camera;
import android.hardware.Camera.Parameters;

public class CameraSurfaceView extends SurfaceView implements Callback {

    private static final String TAG = "CameraSurfaceView";

    private SurfaceHolder surfaceHolder;

    private int mPreviewWidth    = 0;
    private int mPreviewHeight   = 0;

    private CameraPreview mCameraPreview = null;

    public CameraSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getHolder().addCallback(this);
        getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.i(TAG, "surfaceCreated start");
        surfaceHolder = holder;
        try {
            mCameraPreview.setPreviewDisplay(surfaceHolder);
            mCameraPreview.startPreview();
        } catch (Exception e) {
            Log.d(TAG, "startPreview fail!");
        }

        Log.i(TAG, "surfaceCreated end");
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        Log.d(TAG, "surfaceChanged start, w is " + w + ", h is " + h);
        surfaceHolder = holder;
        try {
            mCameraPreview.setPreviewDisplay(surfaceHolder);
        } catch (Exception e) {
            Log.d(TAG, "open  camera setPreviewDisplay fail!");
        }

        Log.d(TAG, "surfaceChanged end");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "surfaceDestoryed start");
        surfaceHolder   = null;
        try {
            mCameraPreview.setPreviewDisplay(surfaceHolder);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Log.d(TAG, "surfaceDestroyed end");
    }

    public void setCameraPreview(CameraPreview cameraPreview){
        mCameraPreview = cameraPreview;
    }
}

