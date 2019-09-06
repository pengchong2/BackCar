/*
 * Copyright 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.autochips.android.backcar.view;

import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;

import com.autochips.android.backcar.service.BackcarService;
import com.autochips.android.backcar.ui.FlyBackcarUI;
import com.mediatek.serviceMonitor.CameraServiceDetector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import android.os.Process;

public class Camera2Preview {
    /**
     * Tag for the {@link Log}.
     */
    private static final String TAG = "Camera2Preview";

    /**
     * Camera state: Showing camera preview.
     */
    private static final int STATE_PREVIEW = 0;
    private boolean bWaitPreview = false;
    /**
     * {@link TextureView.SurfaceTextureListener} handles several lifecycle events on a
     * {@link TextureView}.
     */
    private final TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            Log.i(TAG, "width: " + width + " height: " + height);
            setUpCameraOutputs(width, height);
            configureTransform(width, height);
            //openCamera();
            mbSurfaceAvailable=true;
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
            Log.i(TAG, "width: " + width + " height: " + height);
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            mbSurfaceAvailable=false;
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {
            //Log.i(TAG, "onSurfaceTextureUpdated");
        }


    };

    /**
     * ID of the current {@link CameraDevice}.
     */
    private String mCameraId;

    private boolean mbSurfaceAvailable;
    private boolean mbExit;
    /**
     * An {@link AutoFitTextureView} for camera preview.
     */
    private AutoFitTextureView mTextureView;

    /**
     * An {@link CameraManager} for camera service.
     */
    private CameraManager mCameraManager;

    /**
     * Rotation of the current {@link mRotation}.
     */
    private int mRotation;
    private int trueRotation;
    //private Context mContext;
    /**
     * A {@link CameraCaptureSession } for camera preview.
     */
    private CameraCaptureSession mCaptureSession;

    /**
     * A reference to the opened {@link CameraDevice}.
     */
    private CameraDevice mCameraDevice;

    /**
     * The {@link Size} of camera preview.
     */
    private Size mPreviewSize;

    /**
     * {@link CameraDevice.StateCallback} is called when {@link CameraDevice} changes its state.
     */
    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            Log.i(TAG, "onOpened mCameraOpenCloseLock before release");
            // This method is called when the camera is opened.  We start camera preview here.
            while(mbSurfaceAvailable==false &&mbExit==false)
                try {
                    Thread.sleep(10);
                    Log.d(TAG, "wait surface available");
                } catch (Exception e) {
                    Log.e(TAG, "sleep Exception");
                }
            //FlyBackcarUI.getInstance().requestHalControlBackLight(500);
            bWaitPreview = true;
            mCameraDevice = cameraDevice;
            if(!mbExit)
            {
                createCameraPreviewSession();
            }
            mCameraOpenCloseLock.release();
            Log.i(TAG, "onOpened mCameraOpenCloseLock after release");
            FlyBackcarUI.getInstance().setmStep(2);
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            Log.i(TAG, "onDisconnected mCameraOpenCloseLock release");
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            Log.e(TAG, "onError mCameraOpenCloseLock release");
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            Log.e(TAG, "onError camera close");
            mCameraDevice = null;

            if (null != BackcarService.getInstance()) {
                BackcarService.getInstance().cameraError();
            }
        }

        @Override
        public void onClosed(@NonNull CameraDevice camera) {

        }
    };

    /**
     * An additional thread for running tasks that shouldn't block the UI.
     */
    private HandlerThread mBackgroundThread;

    /**
     * A {@link Handler} for running tasks in the background.
     */
    private Handler mBackgroundHandler;


    /**
     * An {@link ImageReader} that handles still image capture.
     */
    private ImageReader mImageReader;

    /**
     * {@link CaptureRequest.Builder} for the camera preview
     */
    private CaptureRequest.Builder mPreviewRequestBuilder;

    /**
     * {@link CaptureRequest} generated by {@link #mPreviewRequestBuilder}
     */
    private CaptureRequest mPreviewRequest;

    /**
     * The current state of camera state for taking pictures.
     *
     * @see #mCaptureCallback
     */
    private int mState = STATE_PREVIEW;

    /**
     * A {@link Semaphore} to prevent the app from exiting before closing the camera.
     */
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);
    CameraServiceDetector mCameraServiceDetector = new CameraServiceDetector();

    /**
     * A {@link CameraCaptureSession.CaptureCallback} that handles events related to JPEG capture.
     */
    private CameraCaptureSession.CaptureCallback mCaptureCallback
            = new CameraCaptureSession.CaptureCallback() {

        private void process(CaptureResult result) {
            switch (mState) {
                case STATE_PREVIEW: {
                    // We have nothing to do when the camera preview is working normally.
                    if(bWaitPreview) {
                        bWaitPreview = false;
                        //FlyBackcarUI.getInstance().requestHalControlBackLight(100);
                    }
                    break;
                }
            }
        }
        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                       @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {
            process(result);
        }

    };

    /**
     * Given {@code choices} of {@code Size}s supported by a camera, chooses the smallest one whose
     * width and height are at least as large as the respective requested values, and whose aspect
     * ratio matches with the specified value.
     *
     * @param choices     The list of sizes that the camera supports for the intended output class
     * @param width       The minimum desired width
     * @param height      The minimum desired height
     * @param aspectRatio The aspect ratio
     * @return The optimal {@code Size}, or an arbitrary one if none were big enough
     */
    private static Size chooseOptimalSize(Size[] choices, int width, int height, Size aspectRatio) {
        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();

        Log.i(TAG, " w:"+w+"  ,h:"+h);
        Log.i(TAG, " width:"+width+"  ,height:"+height);
        for (Size option : choices) {
            Log.i(TAG, " choices.height:"+option.getHeight()+", choices.width:"+option.getWidth());
            if (option.getHeight() == option.getWidth() * h / w &&
                    option.getWidth() >= width && option.getHeight() >= height) {
                bigEnough.add(option);
            }
        }
        Log.i(TAG, " bigEnough.size:" + bigEnough.size());

        // Pick the smallest of those, assuming we found any
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size");
            return choices[0];  // 0->2
        }
    }

    public static Camera2Preview newInstance(AutoFitTextureView autofittextureview, CameraManager cm, int rotation) {
        return new Camera2Preview(autofittextureview, cm, rotation);
    }

    public Camera2Preview(AutoFitTextureView autofittextureview, CameraManager cm, int rotation) {
        Log.i(TAG, "Camera2Preview");
        mTextureView = autofittextureview;
        mCameraManager = cm;
        trueRotation = rotation;
        mRotation = rotation;
        mbSurfaceAvailable=false;
        mbExit=false;
        int cameraNums = mCameraServiceDetector.atc_cameraServiceDetect();
        if(cameraNums==2)	 // exist back
           mCameraId = "1";   //
        else if (cameraNums==1) //no back when NO back sensor,front will be treated as back sensor,so we forced select back..
           mCameraId = "0";
        init();
    }

    private void init() {
        Log.i(TAG, "init");
        startBackgroundThread();
        if (mTextureView.isAvailable()) {
            mbSurfaceAvailable=true;
            setUpCameraOutputs(mTextureView.getWidth(), mTextureView.getHeight());
            configureTransform(mTextureView.getWidth(), mTextureView.getHeight());
            mBackgroundHandler.post(new Runnable() {
                @Override
                public void run() {
                    openCamera();
                    Log.i(TAG, "openCamera  bk");
                }
            });

        } else {
            mBackgroundHandler.post(new Runnable() {
                @Override
                public void run() {
                    openCamera();
                    Log.i(TAG, "openCamera  bk2");
                }
            });
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    /**
     * Sets up member variables related to camera.
     *
     * @param width  The width of available size for camera preview
     * @param height The height of available size for camera preview
     */
    private void setUpCameraOutputs(int width, int height) {
        Log.i(TAG, "setUpCameraOutputs");
        StreamConfigurationMap map = null;

        CameraCharacteristics characteristics;

        CameraManager manager = mCameraManager;
        try {
/*
            for (String cameraId : manager.getCameraIdList()) {
                characteristics = manager.getCameraCharacteristics(cameraId);

                // We don't use a front facing camera in this sample.
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                //if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                if (facing != null && (facing != CameraCharacteristics.LENS_FACING_BACK)) {
                    continue;
                }

                map = characteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map == null) {
                    continue;
                }
                mCameraId = cameraId;

            }
            //TODO
            if(mCameraId=="0")    //only exist back
                mCameraId = "1";   //
            else if (mCameraId==null) //no back when NO back sensor,front will be treated as back sensor,so we forced select back..
                mCameraId = "0";

            //mCameraId = "0";

            // For still image captures, we use the largest available size.
*/
            characteristics = manager.getCameraCharacteristics(mCameraId);
            map = characteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            Size largest = Collections.max(
                    Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                    new CompareSizesByArea());

            Log.i(TAG, "largest.width:"+largest.getWidth()+" ,largest.hight:"+largest.getHeight());

            mImageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(),
                    ImageFormat.JPEG, /*maxImages*/7);
            // Danger, W.R.! Attempting to use too large a preview size could  exceed the camera
            // bus' bandwidth limitation, resulting in gorgeous previews but the storage of
            // garbage capture data.
            mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                    width, height, largest);

            Log.i(TAG, "PreviewSize.width:"+mPreviewSize.getWidth()+" ,PreviewSize.hight:"+mPreviewSize.getHeight());

            // We fit the aspect ratio of TextureView to the size of preview we picked.
            //int orientation = getResources().getConfiguration().orientation;
            //int orientation = getRequestedOrientation();
            //if(0 == trueRotation){
            //if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                //mTextureView.setAspectRatio(mPreviewSize.getWidth(), mPreviewSize.getHeight());
                //Log.i(TAG, "the screen orient is landsacpe");
            //} else {
            //  mTextureView.setAspectRatio(mPreviewSize.getHeight(), mPreviewSize.getWidth());
            // Log.i(TAG, "the screen orient is  iportrait");

            //}
            Log.i(TAG, "mCameraId: " + mCameraId);
            return;
        } catch (CameraAccessException e) {
            Log.e(TAG,"CameraAccessException:No camera!!");
            e.printStackTrace();

        } catch (NullPointerException e) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            Log.e(TAG,"IllegalArgumentException : mCameraId  = " + mCameraId);
            e.printStackTrace();
        }

    }

    /**
     * Opens the camera specified by {@link Camera2BasicFragment#mCameraId}.
     */
    private void openCamera() {
        Log.i(TAG, "openCamera");

        //setUpCameraOutputs(width, height);
        //configureTransform(width, height);
        CameraManager manager = mCameraManager;

        try {
            Log.i(TAG, "openCamera try enter going to acquire lock mCameraOpenCloseLock");
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            Log.i(TAG, "openCamera try leave mCameraOpenCloseLock get");
            manager.openCamera(mCameraId, mStateCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (InterruptedException e){
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        } catch(IllegalArgumentException e) {
             Log.e(TAG,"IllegalArgumentException: No camera!!!");
        }
    }

    /**
     * Closes the current {@link CameraDevice}.
     */
    public void closeCamera(){
        Log.i(TAG, "closeCamera");
        mTextureView.setSurfaceTextureListener(null);
        mbExit=true;
        Log.i(TAG, "set SurfaceTextureLostener null");
        try {
            mCameraOpenCloseLock.acquire();
            Log.i(TAG, "closeCamera get lock mCameraOpenCloseLock");

           // mCameraOpenCloseLock.acquire();
            if (null != mCaptureSession) {
                Log.i(TAG, "mCaptureSession.close()");
                mCaptureSession.close();
                mCaptureSession = null;
            }
            if (null != mCameraDevice) {
                Log.i(TAG, " mCameraDevice.close()" );
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (null != mImageReader) {
                Log.i(TAG, "mImageReader.close()");
                mImageReader.close();
                mImageReader = null;
            }


        } catch (InterruptedException e) {
        //catch (IllegalArgumentException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            Log.i(TAG, "closeCamera release lock mCameraOpenCloseLock");
            mCameraOpenCloseLock.release();
            Log.i(TAG, "closeCamera sucessful!");
        }
        stopBackgroundThread();
    }

    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground",Process.THREAD_PRIORITY_BACKGROUND + Process.THREAD_PRIORITY_MORE_FAVORABLE);
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        if (null != mBackgroundThread) {
            mBackgroundThread.quitSafely();
            try {
                mBackgroundThread.join();
                mBackgroundThread = null;
                mBackgroundHandler = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            Log.e(TAG, "mBackgroundThread is null ???");
        }
    }

    /**
     * Creates a new {@link CameraCaptureSession} for camera preview.
     */
    private void createCameraPreviewSession() {
        Log.i(TAG, "CreateCameraPreviewSession");
        try {
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;
            //Log.i(TAG, "createCameraPreviewSession--texture:"+texture);
            // We configure the size of default buffer to be the size of camera preview we want.
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

            // This is the output Surface we need to start preview.
            Surface surface = new Surface(texture);

            // We set up a CaptureRequest.Builder with the output Surface.
            mPreviewRequestBuilder
                    = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(surface);
            Log.i(TAG, "mCameraDevice.createCaptureSession enter");

            // Here, we create a CameraCaptureSession for camera preview.
            mCameraDevice.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            try {
                                mCameraOpenCloseLock.acquire();
                            } catch (InterruptedException e) {
                                //catch (IllegalArgumentException e) {
                                throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
                            }
                            // The camera is already closed
                            if (null == mCameraDevice) {
                                return;
                            }

                            // When the session is ready, we start displaying the preview.
                            mCaptureSession = cameraCaptureSession;
                            try {
                                // Auto focus should be continuous for camera preview.
                                //mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                //        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                // Flash is automatically enabled when necessary.
                                //mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                                //        CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

                                // Finally, we start displaying the camera preview.
                                mPreviewRequest = mPreviewRequestBuilder.build();
                                mCaptureSession.setRepeatingRequest(mPreviewRequest,
                                        mCaptureCallback, mBackgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                            mCameraOpenCloseLock.release();
                        }

                        @Override
                        public void onConfigureFailed(
                                @NonNull CameraCaptureSession cameraCaptureSession) {
                            Log.i(TAG, "createCaptureSession.StateCallback : onConfigureFailed");
                        }
                    }, null
            );
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch(NullPointerException e) {
           Log.i(TAG, "NullPointerException!!");
           e.printStackTrace();
        } catch(IllegalStateException e) {
           Log.i(TAG, "IllegalStateException!!");
           e.printStackTrace();
        }
        
        Log.i(TAG, "mCameraDevice.createCaptureSession leave");
    }

    /**
     * Configures the necessary {@link Matrix} transformation to `mTextureView`.
     * This method should be called after the camera preview size is determined in
     * setUpCameraOutputs and also the size of `mTextureView` is fixed.
     *
     * @param viewWidth  The width of `mTextureView`
     * @param viewHeight The height of `mTextureView`
     */
    private void configureTransform(int viewWidth, int viewHeight) {
        Log.i(TAG, "configureTransform");
        if (null == mTextureView || null == mPreviewSize) {
            return;
        }

        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        // Force rotate the preview orientation
        Log.i(TAG, "rotation = " + mRotation);
        mRotation = Surface.ROTATION_90;
        Log.i(TAG, "fix the rotation. rotation = " + mRotation);
        //Log.i(TAG, "viewHeight= " + viewHeight+", viewWidth="+viewWidth);
        //Log.i(TAG, "mPreviewSize.getHeight()= " + mPreviewSize.getHeight()+", mPreviewSize.getWidth()="+mPreviewSize.getWidth());

        if (Surface.ROTATION_90 == mRotation || Surface.ROTATION_270 == mRotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale1 =
                    (float) viewHeight / mPreviewSize.getHeight();
            float scale2 =  
                    (float) viewWidth / mPreviewSize.getWidth();

            Log.i(TAG, "scale1= " + scale1+", scale2="+scale2);
            
            matrix.postScale(scale1, scale2, centerX, centerY);
            matrix.postRotate(90 * (mRotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == mRotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        mTextureView.setTransform(matrix);
    }


    /**
     * Compares two {@code Size}s based on their areas.
     */
    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }
}
