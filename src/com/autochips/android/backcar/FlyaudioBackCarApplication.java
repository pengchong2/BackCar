package com.autochips.android.backcar;

import android.app.Application;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;

import com.autochips.android.backcar.ui.FlyBackcarOps;
import com.autochips.android.backcar.ui.FlyBackcarUI;

import cn.flyaudio.sdk.FlySDKManager;
import cn.flyaudio.sdk.InitListener;
import cn.flyaudio.sdk.manager.FlySystemManager;


public class FlyaudioBackCarApplication extends Application implements InitListener{

    private static final String TAG = "FlyaudioBackCar";


    @Override
    public void onCreate() {
        super.onCreate();

//		SkinResource.initSkinResource(flyAudioContext, "com.flyaudio.flyaudioskinproj");
        Log.d("version","backcar 201903271921");
        SkinResource.initSkinResource(getApplicationContext(), "com.flyaudio.flyaudioskinproj");

        ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).
                inflate(R.layout.backcar_camera_preview, null);

        FlySDKManager.getInstance().initialize(this,this);

        Log.d(TAG, "FlyaudioBackCarApplication onCreate!");
        Log.d(TAG, "FlyaudioBackCarApplication 1");

        FlyBackcarUI.getInstance().init(this);
        FlyBackcarOps.getInstance().init(this);
    }

    @Override
    public void onError() {

    }

    @Override
    public void onSucceed() {
        Log.d(TAG, "init sdk sussce!");
        FlySystemManager.getInstance().registerCallBackListener();
    }

}
