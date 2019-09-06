package com.autochips.android.backcar.view;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

import android.util.Log;
import android.util.Size;

import android.view.SurfaceHolder;
import android.view.SurfaceView;

import android.hardware.Camera;
import android.hardware.Camera.ErrorCallback;
import android.hardware.Camera.PreviewCallback;
public class CameraPreview {
///    private static Thread mThread = null;
///    private static boolean mNeedStop = false;

    private static final String TAG             = "CameraPreview";

    private static final boolean AUTOFOUCUS     = true;

    private Camera camera                       = null;
    private SurfaceHolder surfaceHolder         = null;
    private SurfaceView surfaceView             = null;
    //private PreviewCallback previewCallback     = null;
    //private List<Camera.Size> mVideoSizes       = null;
    private List<Camera.Size> mPreviewSizes     = null;
    private Camera.Parameters parameters        = null;

    private int previewWidth    = 720;
    private int previewHeight   = 480;

    private boolean isPreviewed = false;
    private boolean isInited = false;


    public CameraPreview() {

        Log.d(TAG, "CameraPreview construct entry");
        Log.d(TAG, "CameraPreview construct end");
    }

    public boolean initCamera() {
        Log.d(TAG, "initCamera() entry");

        if (isInited) {
            Log.d(TAG, "already init");
            return true;
        }

        isInited = true;
        try {
            int cameraCount = 0;
            cameraCount = Camera.getNumberOfCameras();
            Log.d(TAG, "[initCamera]The facing of the camera is same to the screen");
            //0 is recommended when mipi camera unmounted , 1 is recommended when mipi camera mounted
            if (0 == cameraCount) {//
                //mtkflyaudio
                camera = Camera.open(0);
               // camera = Camera.open(-10);
            } else {
                   //mtkflyaudio
                camera = Camera.open(1);
               // camera = Camera.open(-10);
            }

            parameters = camera.getParameters();
            Camera.Parameters pm = getCusParameters();
            camera.setParameters(pm);
            camera.setErrorCallback(new CameraErrorCallback());
            if (null != surfaceHolder) {
                setPreviewDisplay(surfaceHolder);
            }
        } catch (Exception e) {
            Log.i(TAG, "InitCamera fail!");
        }

///        mNeedStop = false;
///
///      if (null == mThread)
///        {
///            mThread = new Thread(new SignalStatusRunnable());
///            mThread.start();
///        }

        return true;
    }

    public void deInitCamera() {
        Log.d(TAG, "deInitCamera entry");
///     mNeedStop = true;

        if (!isInited) {
            Log.d(TAG, "already deinit");
            return ;
        }
        isInited = false;
        if (null != camera) {
            Log.d(TAG, "camera not null, deinit camera");
            camera.setErrorCallback(null);
            camera.setPreviewCallback(null);
            //camera.stopPreview();
            camera.release();
            camera = null;
        }

///        if (null != mThread) {
///            mThread.interrupt();
///        }

        isPreviewed = false;
        Log.d(TAG, "deInitCamera end");
    }

    public void setPreviewDisplay(SurfaceHolder surfaceHolder) {
        Log.d(TAG, "setPreviewDisplay()");
        this.surfaceHolder = surfaceHolder;
        if(null != camera) {
            Log.d(TAG, "camera not null, set previewDisplay to camera");
            try{
                this.camera.setPreviewDisplay(surfaceHolder);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void startPreview() {
        Log.d(TAG, "startPreview() entry");
        if(null != camera) {
            try {
                //camera.setPreviewCallback(previewCallback);
                camera.setPreviewDisplay(surfaceHolder);
            } catch(Exception e) {
                e.printStackTrace();
            }
            //If already preview, than do not need preview again
            if (!isPreviewed) {
                this.camera.startPreview();
                isPreviewed = true;
                if (AUTOFOUCUS) {
                    this.camera.cancelAutoFocus();
                }
            } else {
                Log.d(TAG, "Camera is previewing");
            }
        }

        Log.d(TAG, "startPreview() end");
    }

    public void stopPreview() {
        Log.d(TAG, "stopPreview() entry");
        Log.d(TAG, Log.getStackTraceString(new Throwable()));
        if(null != camera) {
            //If already stoped, than do not need stop preview
            if (isPreviewed) {
                this.camera.stopPreview();
                isPreviewed = false;
            } else {
                Log.d(TAG, "Camera is not preview");
            }
        }

        Log.d(TAG, "stopPreview() end");
    }

    public Camera.Parameters getCusParameters() {
        Log.d(TAG, "getCusParameters() entry, get custom parameters");
        Camera.Parameters cp = parameters;

        if (null == cp) {
            return null;
        }

        //Get support preview size and set
        mPreviewSizes = cp.getSupportedPreviewSizes();
        Collections.sort(mPreviewSizes, new CameraSizeComparator());
        for (int i = 0; i < mPreviewSizes.size(); i++) {
            Log.d(TAG, (i+1) + ". Camera.Preview size width = " + mPreviewSizes.get(i).width + ", height = " + mPreviewSizes.get(i).height);
        }
        if (!checkPreviewSizeValid(previewWidth, previewHeight)) {
            previewWidth   = mPreviewSizes.get(0).width;
            previewHeight  = mPreviewSizes.get(0).height;
        }
        Log.d(TAG,"previewWidth = "+previewWidth+" previewHeight = "+previewHeight);
        cp.setPreviewSize(previewWidth, previewHeight);

        //cp.setVideoChannel(1);
		//Log.e(TAG, "Richard set video channel\n");

        if (AUTOFOUCUS) {
            cp.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }

        return cp;
    }

    public class CameraSizeComparator implements Comparator<Camera.Size> {
        public int compare(Camera.Size lhs, Camera.Size rhs) {
            if(lhs.width == rhs.width) {
                return 0;
            }
            else if(lhs.width < rhs.width) {
                return 1;
            }else {
                return -1;
            }
        }
    }

    public boolean checkPreviewSizeValid(int x, int y) {
        Log.d(TAG, "checkPreviewSizeValid() entry");
        if (null == camera)
            return false;

        if((null == mPreviewSizes) || (0 == mPreviewSizes.size()))
            return false;

        for (int i = 0, n = mPreviewSizes.size() ; i < n; i++) {
            if ((mPreviewSizes.get(i).width == x) && (mPreviewSizes.get(i).height == y))
                return true;
        }

        return false;
    }

    public Size getPreviewSize() {
        Log.d(TAG, "getPreviewSize() entry");
        if ((0 == previewWidth) || (0 == previewHeight)) {
            return null;
        }

        Size size = new Size(previewWidth, previewHeight);
        return size;
    }

    public Camera.Size getPreviewSuitableSize() {
        Log.d(TAG, "getPreviewSuitableSize() entry");
        if (null == camera)
            return null;

        if((null == mPreviewSizes) || (0 == mPreviewSizes.size()))
            return null;

        Collections.sort(mPreviewSizes, new CameraSizeComparator());
        return mPreviewSizes.get(0);
    }

    public class CameraErrorCallback implements ErrorCallback {
        private static final String TAG = "CameraErrorCallback";

        @Override
        public void onError(int error, Camera camera) {
            Log.e(TAG, "onError got camera error callback. error = " + error);
            stopPreview();
        }
    }

///        public class SignalStatusRunnable implements Runnable {
///
///        @Override
///        public void run() {
///            while (true) {
///                if (!mNeedStop) {
///                    try {
///                        if(camera!= null){
///                            parameters = camera.getParameters();
///                            int status =parameters.getSensorSignalStatus();
///                            Log.d(TAG, "backcar get signal status = " + status);
///                        }
///                        Thread.sleep(300);
///                    } catch (InterruptedException e) {
///                        Log.e(TAG, "wake up block thread");
///                        mNeedStop = true;
///                    } catch (Exception e) {
///                        Log.e(TAG, "sleep Exception");
///                    }
///                } else {
///                    Log.d(TAG, "SignalStatusRunnable exit");
///                    return;
///                }
///            }
///        }
///    }
}
