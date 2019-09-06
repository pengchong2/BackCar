package com.autochips.android.backcar.ui;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import com.autochips.android.backcar.FlyaudioBackCarApplication;
import com.autochips.android.backcar.R;
import com.autochips.android.backcar.SkinResource;
import com.autochips.android.backcar.service.BackcarService;
import com.autochips.android.backcar.view.AutoFitTextureView;
import com.autochips.android.backcar.view.Camera2Preview;

import java.lang.reflect.Method;

import cn.flyaudio.sdk.InitListener;
import cn.flyaudio.sdk.listener.SystemListener;
import cn.flyaudio.sdk.manager.FlySystemManager;

public class BackCarActivity extends Activity implements SystemListener{

    private static final String TAG = "BackCarActivity";
    private static final String ACTION_FINISH_ACTIVITY = "autochips.intent.action.FINISH_ACTIVITY";

    public static final int REMOVE_VIEW = 0;

    public Handler mHandler = null;
    public Camera2Preview mCamera2Preview = null;

    private WindowManager mWM = null;
    private CameraManager mCM = null;
    private View mLayoutView = null;
    private AutoFitTextureView mTextureView = null;
    private BackcarService mBackcarService = null;

    private boolean mIsViewAdded = false;
    private boolean mBroadcatAdded = false;

    private ImageView leftRadar;
    private ImageView rightRadar;
    private ImageView leftCenterRadar;
    private ImageView rightCenterRadar;


    private void registerIntentFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_FINISH_ACTIVITY);
        registerReceiver(mSetActListener, filter);
    }

    private void unregisterIntentFilter() {
        unregisterReceiver(mSetActListener);
    }

    private BroadcastReceiver mSetActListener = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            final String action = intent.getAction();

            if (ACTION_FINISH_ACTIVITY.equals(action)) {
                if (mIsViewAdded) {
                    Log.d(TAG, "ACTION_FINISH_ACTIVITY");
                    disableStatusbarExpand(false);
                    collapsePnanels();
                    mWM.removeView(mLayoutView);
                    mIsViewAdded = false;
                }
                if (null != mBackcarService) {
                    BackcarService.getInstance().setActivity(null);
                }
                finish();
            }
        }
    };


    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate ");
        super.onCreate(savedInstanceState);
        setContentView(SkinResource.getSkinLayoutViewByName("backcar_main"));
        FlySystemManager.getInstance().setSystemListener(this);

        mBackcarService = BackcarService.getInstance();

        if (null != mBackcarService) {
            if (!mBackcarService.IsBackcarOn()) {
                BackcarService.getInstance().setActivity(null);
                finish();
            } else {
                BackcarService.getInstance().setActivity(this);
            }
        } else {
            registerIntentFilter();
            mBroadcatAdded = true;
        }

        mWM = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        mCM = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        LayoutInflater inflater = LayoutInflater.from(SkinResource.getSkinContext());
        mLayoutView = inflater.inflate(SkinResource.getSkinLayoutIdByName("backcar_camera_preview"), null);
        mTextureView = (AutoFitTextureView) mLayoutView.findViewById(SkinResource.getSkinResourceId("backcar_texture", "id"));

        leftRadar = (ImageView) mLayoutView.findViewById(SkinResource.getSkinResourceId("radarview_leftdown_out", "id"));
        leftCenterRadar = (ImageView)mLayoutView.findViewById(SkinResource.getSkinResourceId("radarview_leftdown_inside", "id"));
        rightRadar = (ImageView)mLayoutView.findViewById(SkinResource.getSkinResourceId("radarview_rightdown_out", "id"));
        rightCenterRadar =(ImageView) mLayoutView.findViewById(SkinResource.getSkinResourceId("radarview_rightdown_inside", "id"));


        WindowManager.LayoutParams Lp = new WindowManager.LayoutParams(WindowManager.LayoutParams.TYPE_SYSTEM_ERROR);

        Lp.flags = WindowManager.LayoutParams.FLAG_FULLSCREEN;

        Lp.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_IMMERSIVE_GESTURE_ISOLATED;

        Lp.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;



        if (!mIsViewAdded) {
            mWM.addView(mLayoutView, Lp);
            Log.d(TAG, " mIsViewAdded = true");
            mIsViewAdded = true;
        } else {
            Log.d(TAG, "onCreate : View is already added");
        }

        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case REMOVE_VIEW:
                        Log.d(TAG, "mHandler  mIsViewAdded: "  + mIsViewAdded);
                        if (mIsViewAdded){
                            disableStatusbarExpand(false);
                            collapsePnanels();
                            mWM.removeView(mLayoutView);
                            mIsViewAdded = false;
                        } else {
                            Log.d(TAG, "Handler : View is already removed");
                        }
                        break;
                    default:
                        break;
                }
                super.handleMessage(msg);
            }
        };


    }

    private void disableStatusbarExpand(boolean dis) {

        try {
            Object service = getSystemService("statusbar");
            Class<?> statusbarManager = Class.forName("android.app.StatusBarManager");
            Method method = statusbarManager.getMethod("disable", int.class);
            if (dis) {
                method.invoke(service, View.STATUS_BAR_DISABLE_EXPAND | View.STATUS_BAR_DISABLE_RECENT);
            } else {
                method.invoke(service, 0x0);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    private void collapsePnanels() {

        try {
            Object service = getSystemService("statusbar");
            Class<?> statusbarManager = Class.forName("android.app.StatusBarManager");
            Method method = statusbarManager.getMethod("collapsePanels");
            method.invoke(service);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
        disableStatusbarExpand(true);

        if (mCamera2Preview == null) {
            int rotation = mWM.getDefaultDisplay().getRotation();
            mCamera2Preview = Camera2Preview.newInstance(mTextureView, mCM, rotation);
        } else {
            Log.d(TAG, "mCamera2Preview is not null");
        }
    }

    @Override
    protected void onPause(){
        Log.v(TAG, "onPause");
        super.onPause();
        if (mBroadcatAdded){
            mBroadcatAdded = false;
            unregisterIntentFilter();
        }
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
    }

    @Override
    public void onVolumeStatus(int i, int i1, int i2) {

    }

    @Override
    public void onScreenLightLevelStatus(int i) {

    }

    @Override
    public void onCarRadar(int i, int left, int leftCenter, int rightCenter, int right) {
        Log.d(TAG, "showCarRadarmassage  left:  "+left);
        Log.d(TAG, "showCarRadarmassage  leftCenter:  "+leftCenter);
        Log.d(TAG, "showCarRadarmassage  rightCenter:  "+rightCenter);
        Log.d(TAG, "showCarRadarmassage  right:  "+right);

        if (left == FlySystemManager.RADAR_INIT){
            leftRadar.getDrawable().setLevel(0);
        }else {
            leftRadar.getDrawable().setLevel(left);
        }
        if (leftCenter == FlySystemManager.RADAR_INIT){
            leftCenterRadar.getDrawable().setLevel(0);
        }else {
            leftCenterRadar.getDrawable().setLevel(leftCenter);
        }
        if (right == FlySystemManager.RADAR_INIT){
            rightRadar.getDrawable().setLevel(0);
        }else {
            rightRadar.getDrawable().setLevel(right);
        }
        if (rightCenter == FlySystemManager.RADAR_INIT){
            rightCenterRadar.getDrawable().setLevel(0);
        }else {
            rightCenterRadar.getDrawable().setLevel(rightCenter);
        }
    }

    @Override
    public void onDefaultNaviChanged() {

    }


    @Override
    public void onVolumeChannel(int i){

    }

    @Override
    public void onDayNightMode(int i){

    }

    @Override
    public void onCurrentPageChanged(int i){

    }

    @Override
    public void onPhoneVolumeStatus(int i){

    }

    @Override
    public void onMediaVolumeStatus(int i){

    }

    @Override
    public void onScreenBrightness(int i){

    }

    @Override
    public final void onScreenBrightnessAutoStatus(int i){

    }
}

