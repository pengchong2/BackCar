package com.autochips.android.backcar.service;

import android.app.Activity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Process;
import android.os.SystemProperties;
import android.util.Log;

import com.autochips.android.backcar.Backcar_GPIO;
import com.autochips.android.backcar.Native;
import com.autochips.android.backcar.ui.BackCarActivity;
import com.autochips.android.backcar.ui.FlyBackcarOps;
import com.autochips.android.backcar.ui.FlyBackcarUI;
import com.autochips.android.backcar.view.Camera2Preview;
import com.mediatek.camera.AtcFeatureControl;
import com.mediatek.serviceMonitor.CameraServiceDetector;
import java.lang.reflect.Method;


public class BackcarService extends Service {
    private static final String TAG = "BackcarService";

    private static final String ACTION_FINISH_ACTIVITY = "autochips.intent.action.FINISH_ACTIVITY";
    private static final String BOOT_IPO = "android.intent.action.ACTION_BOOT_IPO";
    private static final String SHUTDOWN_IPO = "android.intent.action.ACTION_SHUTDOWN_IPO";

    private static final int MSG_START = 0x0;
    private static final int MSG_STOP = 0x1;
    private static final int MSG_BACKCAR_STOPAUDIO =0x6;
    private static final int MSG_BACKCAR_START_BCEVENT_THREAD = 0x7;
    private static final int MSG_BACKCAR_CHECK_START = 0x8;

    private static final int STOPAUDIO_DELAY = 50;
    private static final int CHECK_DELAY = 500;

    private static boolean mIsBackcarOn = false;
    private static boolean mNeedStop = false;

    private static Thread mThread = null;
    private static BackcarService gInst = null;

    private volatile static Method mGetMethod = null;
    private volatile static Method mSetMethod = null;

    private CameraManager mCM = null;
    private AudioManager mAm = null;
    private Activity mActivity = null;
    private String preWatermarkStatus;

    int buffLen = 1032;
    byte[] readbuff = new byte[buffLen];   
    CameraServiceDetector mCameraServiceDetector = new CameraServiceDetector();

    private void registerIPOBroadcastReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BOOT_IPO);
        filter.addAction(SHUTDOWN_IPO);
        registerReceiver(mIPOListener, filter);
        Log.d(TAG, "IPO");
    }

    private void unregisterIPOBroadcastReceiver() {
        unregisterReceiver(mIPOListener);
    }

    private BroadcastReceiver mIPOListener = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (BOOT_IPO.equals(action)){
                Log.d(TAG, "BOOT_IPO");
                mNeedStop = false;
                Backcar_GPIO.resetGpioFlag();
                if (false == mThread.isAlive()) {
                    mThread = new Thread(new BackCarRunnable());
                    mThread.setPriority(Thread.MAX_PRIORITY);
                    mThread.start();
                }
            } else if (SHUTDOWN_IPO.equals(action)) {
                Log.d(TAG, "SHUTDWON_IPO");
                if (mIsBackcarOn){
                    mMsgHandler.removeMessages(MSG_STOP);
                    mMsgHandler.sendEmptyMessage(MSG_STOP);
                }
                mNeedStop = true;
                if (null != mThread){
                    mThread.interrupt();
                }
            } else {
                Log.d(TAG, "Not IPO");
            }
        }
    };

    private static String getString(final String key, final String def){
        try {
            if (null == mGetMethod) {
                mGetMethod = Class.forName("android.os.SystemProperties").getMethod("get", String.class, String.class);
            }
            return (String)mGetMethod.invoke(null, key, def);
        } catch (Exception e) {
            Log.e(TAG, "Platform error: " + e.toString());
            return def;
        }
    }

    private static String setString(final String key, final String val) {
        try {
            if (null == mSetMethod) {
                mSetMethod = Class.forName("android.os.SystemProperties").getMethod("set", String.class, String.class);
            }
            return (String)mSetMethod.invoke(null, key, val);
        } catch (Exception e) {
            Log.e(TAG, "Platform error: " + e.toString());
            return val;
        }
    }



    Handler mMsgHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case MSG_BACKCAR_START_BCEVENT_THREAD:
                    Log.v(TAG,"MSG_BACKCAR_START_BCEVENT_THREAD");
                    if (null == mThread) {
                        mThread = new Thread(new BackCarRunnable());
                        mThread.start();
                        registerIPOBroadcastReceiver();
                        readJNIThread jniThread = new readJNIThread();
                        jniThread.start();
                        Log.d(TAG,"start backcar readJNIThread");
                    }
                    break;
                case MSG_START:
                    Log.v(TAG, "MSG_START");
                    if (!mIsBackcarOn) {
                       
                        mIsBackcarOn = true;
                        if(true == AtcFeatureControl.ATC_SUPPORT_CVBS_FORMAT_CHANGE){
                            // update cvbs format information
                            Log.d(TAG,"cvbs_switch: start time = ");
                            // FlyBackcarUI.getInstance().setBackcarStatus(true);
                            // FlyBackcarUI.getInstance().processBackcarEvent(0x01);
                            mCameraServiceDetector.atc_updateCameraStaticInfo();
                        }
                        // Intent intentBCActvity = new Intent(getApplicationContext(), BackCarActivity.class);
                        // intentBCActvity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                        //     Intent.FLAG_ACTIVITY_SINGLE_TOP |
                        //     Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                        // startActivity(intentBCActvity);
                        FlyBackcarUI.getInstance().setBackcarStatus(true);
                        FlyBackcarUI.getInstance().processBackcarEvent(0x01);
                        // removeMessages(MSG_BACKCAR_CHECK_START);
                        //sendEmptyMessageDelayed(MSG_BACKCAR_CHECK_START, CHECK_DELAY);

                        preWatermarkStatus = getString("runtime.watermark.config", "0");
                        setString("runtime.watermark.config", "0");

                    }
                    break;

                case MSG_STOP:
                    Log.d(TAG, "MSG_STOP" + mIsBackcarOn);
                      //FlyBackcarUI.getInstance().processBackcarEvent(0x00);
                       // FlyBackcarUI.getInstance().setBackcarStatus(false);

                    if (mIsBackcarOn) {
                        mIsBackcarOn = false;                    
                        setString("runtime.watermark.config", preWatermarkStatus);
                        FlyBackcarUI.getInstance().processBackcarEvent(0x00);
                        FlyBackcarUI.getInstance().setBackcarStatus(false);
                        /*
                        removeMessages(MSG_BACKCAR_CHECK_START);
                        removeMessages(MSG_BACKCAR_STOPAUDIO);
                        Log.d(TAG, "MSG_STOP  :sendEmptyMessageDelayed");
                        sendEmptyMessageDelayed(MSG_BACKCAR_STOPAUDIO, STOPAUDIO_DELAY);

                        Camera2Preview c2p = null;
                        if (null != (c2p = getCamera2Preview())) {
                            Log.d(TAG, "MSG_STOP  : c2p.closeCamera()");
                            c2p.closeCamera();
                        }
                        else {
                            Log.d(TAG, "Camera2Preview is null");
                        }

                        if (null != mActivity) {
                            Log.d(TAG, "MSG_STOP  :mActivity.finish()");
                            ((BackCarActivity)mActivity).mHandler.removeMessages(BackCarActivity.REMOVE_VIEW);
                            ((BackCarActivity)mActivity).mHandler.sendEmptyMessage(BackCarActivity.REMOVE_VIEW);
                            mActivity.finish();
                            mActivity = null;
                        } else {
                            Intent intent = new Intent();
                            intent.setAction(ACTION_FINISH_ACTIVITY);
                            sendBroadcast(intent);
                            Log.d(TAG,"mActivity is null!");
                        }
                        */
                    }
                    break;

                case MSG_BACKCAR_STOPAUDIO:
                    Log.d(TAG, "MSG_BACKCAR_STOPAUDIO");

                    break;

                case MSG_BACKCAR_CHECK_START:
                     /*
                     if (mIsBackcarOn && (null == mActivity)) {
                        //fix for special case (bug 237339)
                        //In some case, 3rd apk will start acticity again if it lost activity focus.
                        //our intent will be ignored by AMS.
                        Log.d(TAG,"startActivity again");
                        Intent intentBCActvity = new Intent(getApplicationContext(), BackCarActivity.class);
                        intentBCActvity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                            //Intent.FLAG_ACTIVITY_CLEAR_TASK |
                            //Intent.FLAG_ACTIVITY_CLEAR_TOP |
                            Intent.FLAG_ACTIVITY_SINGLE_TOP |
                            Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                        startActivity(intentBCActvity);
                    }
                    */
                    break;
                default:
                    break;
            }
        }
    };
    public class Arm2CommunicationRunnable implements Runnable{
        @Override
        public void run() {
            Log.i(TAG, "Current thread priority " + Process.getThreadPriority(Process.myTid()));
            Process.setThreadPriority(Process.THREAD_PRIORITY_FOREGROUND);//-2
            //Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            String CameraId;
            mCM = (CameraManager)getSystemService(Context.CAMERA_SERVICE);
            CameraCharacteristics characteristics;
            Log.i(TAG, "inform arm2 android system is ready");
            //Backcar_GPIO.takeoverfromArm2(); //audio和这里操作倒车播U盘歌曲，把U盘相机状态错误倒车退出。
            Log.i(TAG, "arm2 backcar is exit, so arm1 start backcarservice and take over the backcar"); 
            Log.d(TAG,"before camera time = "+ System.currentTimeMillis());    
            // step 2, wait for cameraservice
            // continue only when camera service is ready
            int cameraNums = mCameraServiceDetector.atc_cameraServiceDetect();
            while(0 >= cameraNums){
                try {
                    Thread.sleep(10);
                } catch (Exception e) {
                    Log.e(TAG, "sleep Exception");
                }
                cameraNums = mCameraServiceDetector.atc_cameraServiceDetect();
            }
            Log.d(TAG,"found cameras: "+ cameraNums+" time = "+ System.currentTimeMillis());
			            // step 3, start BC event thread
            //add 7s delay for preload,3s for usual load
            // try {
            //     Thread.sleep(7000);
            // } catch (Exception e) {
            //     Log.e(TAG, "sleep Exception");
            // }

            Log.d(TAG,"sleep 7s time = "+ System.currentTimeMillis());

            while("1".equals(SystemProperties.get("agreepage.state.displaying"))){
                  try{
                        Thread.sleep(100);
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                  
            }

              Log.d(TAG,"agreepage.state.displaying = "+SystemProperties.get("agreepage.state.displaying"));

            // for(int i=0;i<300;i++){
            //     Log.d(TAG,"agreepage.state.displaying = "+SystemProperties.get("agreepage.state.displaying"));

            //     if("1".equals(SystemProperties.get("agreepage.state.displaying"))){
            //         try{
            //             Thread.sleep(100);
            //         }catch(Exception e){
            //             e.printStackTrace();
            //         }
            //     }else{
            //         // try{
            //         //     Thread.sleep(3000);
            //         // }catch (Exception e){
            //         //     e.printStackTrace();
            //         // }
            //         break;
            //     }
            // }
            Log.d(TAG,"after agreepage time = "+ System.currentTimeMillis());

            mMsgHandler.sendEmptyMessage(MSG_BACKCAR_START_BCEVENT_THREAD);
            /******************** for call cameraservice connect  in advance ->atc0113 **************************/
            try {
               if (cameraNums == 2){
                  CameraId = "1";
                  Log.i(TAG,"get parameters start");
                  characteristics = mCM.getCameraCharacteristics(CameraId);

                  CameraId = "0";
                  characteristics = mCM.getCameraCharacteristics(CameraId);

                  Log.i(TAG,"get parameters end");
               }else if(cameraNums == 1){
                  characteristics = mCM.getCameraCharacteristics("0");}
           } catch (CameraAccessException e) {
                Log.e(TAG, "get info  Exception");
                e.printStackTrace();
           }
        }
    }
    public class BackCarRunnable implements Runnable {

        @Override
        public void run() {
            Log.v(TAG, "run");
            Process.setThreadPriority(Process.THREAD_PRIORITY_DISPLAY);
            while (true) {
                if (!mNeedStop) {
                    try {

                        Log.d(TAG,"before read getbackcarevent");
                        int BackcarEvent = Backcar_GPIO.GetBackcarEvent();
                    	//int BackcarEvent = getBackcarEvent();
                    	int delayTime = 300;
                        Log.i(TAG, "BackcarEvent = " + BackcarEvent);
                        int step = FlyBackcarUI.getInstance().getmStep();
                        if(BackcarEvent ==0){
                            //request start
                            if(step == -1 ||step == 4){

                            }else if(step == 1 ||step == 2){
                                continue;
                            }else if(step==3){
                                Log.d(TAG,"request start,step 3,wait 500ms");
                                Thread.sleep(300);
                            }
                            step = FlyBackcarUI.getInstance().getmStep();
                            Log.d(TAG,"request start msetp = "+step+", event = "+BackcarEvent);
                        }else if(BackcarEvent==1){
                            //request stop
                            if(step ==-1 || step==4 ){

                            }else if(step ==1){
                                Log.d(TAG,"request stop ,step 1,wait step2");
                                Thread.sleep(1000);
                                step = FlyBackcarUI.getInstance().getmStep();
                                if(step !=2){
                                    Log.d(TAG,"request stop ,step 1,wait step2,1s ,again ");
                                    Thread.sleep(500);
                                }
                                Log.d(TAG,"request stop,1.5s and step = "+FlyBackcarUI.getInstance().getmStep());
                            }else if(step==2){
                                Log.d(TAG,"request stop,step 2 ,and ok");
                                Thread.sleep(100);
                            }else if(step==3){
                                Log.d(TAG,"requeset stop ,step 3,continue");
                                continue;
                            }
                            step = FlyBackcarUI.getInstance().getmStep();
                            Log.d(TAG,"request stop msetp = "+step+", event = "+BackcarEvent);
                        }

                        if (BackcarEvent == Backcar_GPIO.BACKCAR_START) {
                            Log.d(TAG, "BackCarRunnable BACKCAR_STOP");
                            //hal do it 1 to 0,0 start ,1 stop
                            FlyBackcarUI.getInstance().setmStep(0x03);
                            mMsgHandler.removeMessages(MSG_STOP);
                            mMsgHandler.sendEmptyMessage(MSG_STOP);

                        } else if (BackcarEvent == Backcar_GPIO.BACKCAR_STOP) {
                            Log.d(TAG, "BackCarRunnable BACKCAR_START");
                            //hal do it 1 to 0.  0 start ,1 stop
                            FlyBackcarUI.getInstance().setmStep(0x01);
                            FlyBackcarOps.getInstance().requestOpsShow(0x00);
                            mMsgHandler.removeMessages(MSG_START);
                            mMsgHandler.sendEmptyMessage(MSG_START);
                            delayTime = 1000;

                        } else {
                            Log.i(TAG, "BackCarRunnable Unexpected EVENT");
                        }
                        Thread.sleep(delayTime);
                    } catch (InterruptedException e){
                        Log.e(TAG, "wake up block thread");
                        mNeedStop = true;
                    } catch (Exception e){
                        Log.e(TAG, "sleep Exception");
                    }
                } else {
                    Log.d(TAG, "BackCarRunnable exit");
                    return;
                }
            }
        }
    }

    private Camera2Preview getCamera2Preview () {
        if(null != mActivity) {
            return ((BackCarActivity)mActivity).mCamera2Preview;
        }
        return null;
    }

    public static BackcarService getInstance() {
        return (gInst);
    }

    public boolean IsBackcarOn() {
        Log.i(TAG, "mIsBackcarOn = " + mIsBackcarOn);
        return mIsBackcarOn;
    }

    public void setActivity(Activity activity){
        Log.i(TAG, "setActivity - mIsBackcarOn = " + mIsBackcarOn);
        if (null == activity){
        } else {
            if (null != mActivity && mActivity != activity) {
                mActivity.finish();
            }
        }
        mActivity = activity;
    }

    public void cameraError(){
        Log.v(TAG, "cameraError");
        mMsgHandler.removeMessages(MSG_STOP);
        mMsgHandler.sendEmptyMessage(MSG_STOP);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "onStartCommand");
        gInst = this;
        //registerIPOBroadcastReceiver();

        //******************************************************************************************
        // warning 1: not suitable condition 'if(null != intent)' for deciding if should communicate with arm2
        // warning 2: first time, backcarservice start Arm2CommunicationThread and be killed before really take control over from arm2
        //               backcar service will be restarted. this second time, it will 
        //               'sendEmptyMessage(MSG_BACKCAR_START_BCEVENT_THREAD)'  straightly. really bad situation!!
        if(null != intent){
            Log.i(TAG, "android system is ready, start BC event thread after communication with arm2 in Arm2CommunicationThread");
            Thread Arm2CommunicationThread = new Thread(new Arm2CommunicationRunnable());
            Arm2CommunicationThread.start();
            // wait for communicate end ? no!! do not block backcar_main thread
        }else{
            Log.i(TAG, "cameraservice re-started - start BC event thread");
            // step 1, empty
            // step 2, empty,?
            // step 3, start BC event thread
            mMsgHandler.sendEmptyMessage(MSG_BACKCAR_START_BCEVENT_THREAD);
        }

        //******************************************************************************************

        return START_STICKY;
    }

    @Override
    public void onDestroy(){
        Log.d(TAG, "onDestroy");
        unregisterIPOBroadcastReceiver();
        super.onDestroy();
    }

    public boolean isCameraReady(){

        int cameraNums = mCameraServiceDetector.atc_cameraServiceDetect();
        int i=200;
        while(0 >= cameraNums && i>0){
            try {
                Thread.sleep(10);
            } catch (Exception e) {
                Log.e(TAG, "sleep Exception");
            }
            cameraNums = mCameraServiceDetector.atc_cameraServiceDetect();
            --i;
        }
        Log.d(TAG,"found cameras: "+ cameraNums);

        return true;
    }

    public class readJNIThread extends Thread{
        @Override
        public void run() {
            int readLen = 0;
            while (true){
                readLen = Native._read(readbuff, buffLen);

                int lenPos = 2;

                if (readbuff != null) {
                } else {
                    continue;
                }

                int dataLen = (int) (readbuff[2] & 0xff) + 2;

                boolean status = true;
                while (status) {
                    dataLen = (int) (readbuff[lenPos] & 0xff) + 2;
                    byte[] buff = new byte[dataLen];

                    System.arraycopy(readbuff, lenPos - 2, buff, 0, dataLen);
                    // 读取JNI上的信息
                    analysisData(buff, dataLen);

                    lenPos = lenPos + dataLen;

                    if ((readLen < lenPos) || (dataLen < 9)) {

                        status = false;
                    }
                }
            }
        }
    }

    private void analysisData(byte[] data, int len) {
        // Analysis

        if (len < 9) {
            return;
        }
        // int ID = (data[3] << 24) | (data[4] << 16) | (data[5] << 8) |
        // (data[6]);
        int ID = 0;
        ID += (int) ((data[3] << 24) & 0xFF000000);
        ID += (int) ((data[4] << 16) & 0xFF0000);
        ID += (int) ((data[5] << 8) & 0xFF00);
        ID += (int) (data[6] & 0xFF);

        int type = data[7] & 0xFF;

        Log.d(TAG,"get hal to backcar data:"+bytes2HexString(data));
        //from hal to back UI only ID 7053A,and from back UI to Hal only ID 0x00
        if(ID ==0x7053a ) {
            switch (type) {
                case 0x20:
                case 0x40:
                case 0x41:
                    FlyBackcarUI.getInstance().analyseData(data,len);
                    break;
                case 0x30:
                case 0x31:
                    FlyBackcarOps.getInstance().analyseData(data,len);
                    break;
                default:
                    break;
            }
        }
    }

    public static String bytes2HexString(byte[] b) {
        String ret = "";
        for (int i = 0; i < b.length; i++) {
            String hex = Integer.toHexString(b[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            if (hex.length() == 2) {
                hex = " " + hex;
            }
            ret += hex;
        }
        return ret;
    }

}
