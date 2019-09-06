package com.autochips.android.backcar.view;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.ErrorCallback;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class FlyVideoView extends SurfaceView implements SurfaceHolder.Callback {

    private String TAG = "FlyVideoView";
    private Camera mCamera = null;
    private SurfaceHolder surfaceHolder = null;
    private Context mContext;
    private int videoWidth = -1;
    private int videoHeight = -1;
//    private boolean mPreviewing;
//    private FlyaudioApplication mApplication = null;
//    public static boolean mMediaServerDied = false;
    private final ErrorCallback mErrorCallback = new ErrorCallback() {

        @Override
        public void onError(int error, Camera camera) {
            // TODO Auto-generated method stub
//            if (error == Camera.CAMERA_ERROR_SERVER_DIED) {
//                mMediaServerDied = true;
//                Log.d(TAG, time()+"---------meidaserverdied");
//            }
        }
    };

    public FlyVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // TODO Auto-generated constructor stub
//        this.setBackgroundColor(Color.BLACK);
        mContext = context;
    }

    public void initFlyVideoView() {
    	Log.d(TAG, "---------initFlyVideoView");
        surfaceHolder = this.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        surfaceHolder.setFormat(PixelFormat.TRANSLUCENT);
        // /* 设置分辨率 */
        if(videoWidth >=480 && videoHeight >=320){
        	Log.d("JumpPage","set fix size video width = "+videoWidth+",height="+videoHeight);
        	surfaceHolder.setFixedSize(videoWidth, videoHeight);
        }
        Log.d("JumpPage","initFlyVideoView done");
    }
    
    public void setCamera(Camera camera){
    	mCamera = camera;
    }
  
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // TODO Auto-generated method stub
        // 当进入这个Activity时，会执行
        Log.d(TAG, "---------surfaceCreated");
        try {
            if (mCamera != null) {
                mCamera.setPreviewDisplay(holder);
            }
        } catch (IOException exception) {
            Log.e(TAG, "IOException caused by setPreviewDisplay()", exception);
        }
    }
    
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
            int height) {
        // TODO Auto-generated method stub
    	requestLayout();
//    	invalidate();
    	try{
	    	if(mCamera != null) {
                mCamera.startPreview();
            }
    	}catch(Exception exception){
    		Log.d(TAG,"startPreview failed,camera is not ready");
    	}
        Log.d(TAG, "---------surfaceChanged");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // TODO Auto-generated method stub
        // 退出这个Activity时，会执行
//        this.setBackgroundColor(Color.RED);
        // Surface will be destroyed when we return, so stop the preview.
        if (mCamera != null) {
            mCamera.stopPreview();
        }
        Log.d(TAG, "----------surfaceDestroyed");
    }

    /**
     * This class represents the condition that we cannot open the camera
     * hardware successfully. For example, another process is using the camera.
     */
    public class CameraHardwareException extends Exception {
        public CameraHardwareException(Throwable t) {
            super(t);
        }
    }
    
    public void setVideoSize(int width,int height){
    	this.videoWidth = width;
    	this.videoHeight = height;
    }

}
