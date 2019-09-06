package com.autochips.android.backcar.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.autochips.android.backcar.service.BackcarService;


public class BootCompletedReceiver extends BroadcastReceiver {
    private static final String TAG = "BootCompletedReceiver";

    @Override
    public void onReceive( Context context, Intent intent) {
        String action = intent.getAction();
        Log.v(TAG, " onReceiver = " + action);

        if ("android.intent.action.START_ARM1_BACKCAR".equals(action)) {
            Log.v(TAG, "- start service");
            if (null != context){
                Intent ServiceIntent = new Intent(context, BackcarService.class);
                context.startService(ServiceIntent);
            }
        }
    }
}
