package com.autochips.android.backcar.ui;

import android.content.Context;
import android.graphics.Point;
import android.os.Handler;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.autochips.android.backcar.SkinResource;

/**
 * Created by chengzihong on 18-9-5.
 */

public class FlyBackcarOps {

    private String TAG = "ops";

    private static ImageView leftRadar=null;
    private static ImageView rightRadar=null;
    private static ImageView leftCenterRadar=null;
    private static ImageView rightCenterRadar=null;

    private static ImageView leftRadar_front=null;
    private static ImageView rightRadar_front=null;
    private static ImageView leftCenterRadar_front=null;
    private static ImageView rightCenterRadar_front=null;

    private static Button backcar_radar_back_button = null;
    private static Button backcar_radar_voice_button = null;

    private int mLevel_leftRadar=0;
    private int mLevel_rightRadar=0;
    private int mLevel_leftCenterRadar=0;
    private int mLevel_rightCenterRadar=0;

    private int mLevel_leftRadar_front=0;
    private int mLevel_rightRadar_front=0;
    private int mLevel_leftCenterRadar_front=0;
    private int mLevel_rightCenterRadar_front=0;


    private String mLayoutName = null;

    private int radarType = -1;
    private int FRONT_RADAR = 0;
    private int REAR_RADAR = 1;

    private Context mContext = null;

    private WindowManager wm;
    private WindowManager.LayoutParams params = new WindowManager.LayoutParams();

    private static FlyBackcarOps instance = null;

    private static FrameLayout mMainRadarView = null;
    private boolean mIsViewAdd = false;
    private boolean mIsShowView = false;
    private int mInitValue = 255;

    private int requestShow = -1;
    private int mColor = 0;

    private int mRadarSound = 1;
    private static String RADAR_SOUND = "RADAR_SOUND";

    FlyBackcarOps(){

    }

    public static synchronized FlyBackcarOps getInstance(){
        if(instance==null){
            instance = new FlyBackcarOps();
        }
        return instance;
    }

    public void init(Context context){
        mContext = context;

        wm = (WindowManager) mContext.getApplicationContext().getSystemService(Context.WINDOW_SERVICE);

        params.flags = WindowManager.LayoutParams.FLAG_FULLSCREEN|WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                |WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;

        params.type = 2016;

        Display display = wm.getDefaultDisplay();
        Point p  = new Point();
        display.getRealSize(p);
        params.width = p.x;
        params.height = p.y;
        //add 20180525 end

        params.x = 0;
        params.y = 0;

        params.alpha = 1;
        params.gravity = Gravity.LEFT | Gravity.TOP ;

        LayoutInflater inflater = LayoutInflater.from(SkinResource.getSkinContext());

        String backLayout = FlyBackcarUI.getInstance().getLayoutName();
        radarType = FlyBackcarUI.getInstance().getRadarType();
        if("mtk_fly_backcarvideo_radar_golf_layout".equals(backLayout)){
            mLayoutName = "mtk_fly_backcar_revevsing_radar_layout";
            if(params.width==1024){
                try {
                    mColor = Integer.parseInt(SystemProperties.get("persist.fly.colortheme",
                            "-65536"));
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
        if("mtk_fly_backcarvideo_radar_layout".equals(backLayout)){
            mLayoutName = "mtk_fly_backcar_radar_layout";
        }
        if(mLayoutName==null){
            Log.d(TAG,"no ops radar");
            return;
        }
        try {
            mMainRadarView =(FrameLayout) inflater.inflate(SkinResource.getSkinLayoutIdByName(mLayoutName), null);
        }catch (Exception e){
            e.printStackTrace();
        }
        if(mMainRadarView!=null) {
            if (radarType == 0x01 || radarType == 0x02) {
                wm.addView(mMainRadarView,params);
                initView();
                if(radarType==2){
                    mMainRadarView.setBackgroundColor(mColor);
                }
                mIsViewAdd = true;
                hideView(0);
            }
        }

    }

    private final void hideView(int whichView){
        if(mMainRadarView!=null){
            mMainRadarView.setVisibility(View.GONE);
            mIsShowView = false;
            Log.d(TAG,"ops initView "+whichView);
        }else{
            Log.d(TAG,"ops initView but null");
        }
    }

    private void initView(){
        if(mMainRadarView!=null) {
            leftRadar = (ImageView) mMainRadarView.findViewById(SkinResource.getSkinResourceId("radarrear_leftdown_out", "id"));
            leftCenterRadar = (ImageView) mMainRadarView.findViewById(SkinResource.getSkinResourceId("radarrear_leftdown_inside", "id"));
            rightRadar = (ImageView) mMainRadarView.findViewById(SkinResource.getSkinResourceId("radarrear_rightdown_out", "id"));
            rightCenterRadar = (ImageView) mMainRadarView.findViewById(SkinResource.getSkinResourceId("radarrear_rightdown_inside", "id"));

            leftRadar_front = (ImageView) mMainRadarView.findViewById(SkinResource.getSkinResourceId("radarfront_leftup_out", "id"));
            leftCenterRadar_front = (ImageView) mMainRadarView.findViewById(SkinResource.getSkinResourceId("radarfront_leftup_inside", "id"));
            rightRadar_front = (ImageView) mMainRadarView.findViewById(SkinResource.getSkinResourceId("radarfront_rightup_out", "id"));
            rightCenterRadar_front = (ImageView) mMainRadarView.findViewById(SkinResource.getSkinResourceId("radarfront_rightup_inside", "id"));
            if(radarType==2){
                backcar_radar_back_button = (Button) mMainRadarView.findViewById(SkinResource.getSkinResourceId("backcar_radar_back_button","id"));
                backcar_radar_voice_button = (Button) mMainRadarView.findViewById(SkinResource.getSkinResourceId("backcar_radar_voice_button","id"));
                if(backcar_radar_back_button!=null) {
                    backcar_radar_back_button.setOnClickListener(listener);
                }else{
                    Log.d(TAG,"backcar_radar_back_button is null ");
                }
                if(backcar_radar_voice_button!=null){
                    backcar_radar_voice_button.setOnClickListener(listener);
                    mRadarSound = Settings.Secure.getInt(mContext.getContentResolver(),RADAR_SOUND,1);
                    backcar_radar_voice_button.getBackground().setLevel(mRadarSound);
                    byte data[] ={0x00};
                    data[0]=(byte) mRadarSound;
                    MakeAndSendMessage(0x00,(byte)0x31,data);
                }
            }
            if(leftRadar==null){
                Log.d(TAG,"ops leftRadar ImageView is null");
            }
        }
    }

    View.OnClickListener listener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String tag =(String) v.getTag();
            if("000700f0".equals(tag)){
                hideView(0);
                Log.d(TAG,"back button click");

            }else if("0020230c".equals(tag)){
                if(getRadarSoundStatus()==1){
                    setRadarSoundStatus(0);
                }else if(getRadarSoundStatus()==0){
                    setRadarSoundStatus(1);
                }
                Settings.Secure.putInt(mContext.getContentResolver(),RADAR_SOUND,mRadarSound);
                byte data[] ={0x00};
                data[0]=(byte) mRadarSound;
                MakeAndSendMessage(0x00,(byte)0x31,data);
                if(backcar_radar_voice_button!=null){
                    backcar_radar_voice_button.getBackground().setLevel(mRadarSound);
                    backcar_radar_voice_button.invalidate();
                }
                Log.d(TAG,"radar voice button click ,and status = "+mRadarSound);
            }
        }
    };


    private final void showView(int whichView){
        if(mMainRadarView==null){
            Log.d(TAG,"ops mMainView is null");
            return;
        }else{
            if(mMainRadarView.getVisibility()!= View.VISIBLE){
                mMainRadarView.setVisibility(View.VISIBLE);
                onCarOpsRadar(REAR_RADAR,mLevel_leftRadar,mLevel_leftCenterRadar,mLevel_rightCenterRadar,mLevel_rightRadar);
                onCarOpsRadar(FRONT_RADAR,mLevel_leftRadar_front,mLevel_leftCenterRadar_front,mLevel_rightCenterRadar_front,
                        mLevel_rightRadar_front);
                mIsShowView = true;
                Log.d(TAG,"ops mMainView show ="+mIsShowView);
            }else{
                Log.d(TAG,"ops mMainView already show");
            }
        }

    }

    public void onCarOpsRadar(int i,int left, int leftCenter, int rightCenter, int right){
        if(radarType==0x01 ||radarType==0x02){
            if(left>=0 && left<255){
                if(REAR_RADAR==i){
                    if(radarType==1){
                        if(left>4) { left = 0;}
                        if(leftCenter>4){leftCenter=0;}
                        if(right>4){right=0;}
                        if(rightCenter>4){rightCenter=0;}
                    }
                    if(radarType==2){
                        if(left>5) { left = 0;}
                        if(leftCenter>12){leftCenter=0;}
                        if(right>5){right=0;}
                        if(rightCenter>12){rightCenter=0;}
                    }
                }else if(FRONT_RADAR==i){
                    if(radarType==1){
                        if(left>4) { left = 0;}
                        if(leftCenter>4){leftCenter=0;}
                        if(right>4){right=0;}
                        if(rightCenter>4){rightCenter=0;}
                    }
                    if(radarType==2){
                        if(left>5) { left = 0;}
                        if(leftCenter>9){leftCenter=0;}
                        if(right>5){right=0;}
                        if(rightCenter>9){rightCenter=0;}
                    }
                }
                saveRadarData(i,left,leftCenter,rightCenter,right);
            }
            if(REAR_RADAR == i) {
                if(leftRadar==null ||leftCenterRadar==null||rightCenterRadar==null||rightRadar==null){
                    Log.d(TAG,"Radar is null  ,return");
                    return ;
                }
                if(leftRadar.getBackground()==null ||leftCenterRadar.getBackground()==null ||
                        rightRadar.getBackground() ==null ||rightCenterRadar.getBackground()==null ) {
                    Log.d(TAG,"getBackground is null  ,return");
                    return ;
                }


                if (left == mInitValue ) {
                    leftRadar.getBackground().setLevel(0);
                } else {
                    leftRadar.getBackground().setLevel(left);
                }
                if (leftCenter == mInitValue) {
                    leftCenterRadar.getBackground().setLevel(0);
                } else {
                    leftCenterRadar.getBackground().setLevel(leftCenter);
                }
                if (right == mInitValue) {
                    rightRadar.getBackground().setLevel(0);
                } else {
                    rightRadar.getBackground().setLevel(right);
                }
                if (rightCenter == mInitValue) {
                    rightCenterRadar.getBackground().setLevel(0);
                } else {
                    rightCenterRadar.getBackground().setLevel(rightCenter);
                    rightCenterRadar.invalidate();
                }

            }else if(FRONT_RADAR== i ){

                if(leftRadar_front==null ||leftCenterRadar_front==null||rightCenterRadar_front==null||rightRadar_front==null){
                    Log.d(TAG,"Radar_front is null,return");
                    return ;
                }
                if(leftRadar_front.getBackground()==null ||leftCenterRadar_front.getBackground()==null ||
                        rightRadar_front.getBackground() ==null ||rightCenterRadar_front.getBackground()==null ) {
                    Log.d(TAG,"front getBackground is null ,return ");
                    return ;
                }

                if (left == mInitValue) {
                    leftRadar_front.getBackground().setLevel(0);
                } else {
                    leftRadar_front.getBackground().setLevel(left);
                    leftRadar_front.invalidate();
                }

                if (leftCenter == mInitValue) {
                    leftCenterRadar_front.getBackground().setLevel(0);
                } else {
                    leftCenterRadar_front.getBackground().setLevel(leftCenter);
                    leftCenterRadar_front.invalidate();
                }

                if (right == mInitValue) {
                    rightRadar_front.getBackground().setLevel(0);
                } else {
                    rightRadar_front.getBackground().setLevel(right);
                    rightRadar_front.invalidate();
                }

                if (rightCenter == mInitValue) {
                    rightCenterRadar_front.getBackground().setLevel(0);
                } else {
                    rightCenterRadar_front.getBackground().setLevel(rightCenter);
                    rightCenterRadar_front.invalidate();
                }

            }
        }else{
            return;
        }
    }

    private void saveRadarData(int i,int left, int leftCenter, int rightCenter, int right ){
        if(i==REAR_RADAR) {
            if (radarType == 1 || radarType == 2) {
                if (left >= 0 || leftCenter >= 0 || rightCenter >= 0 || right >= 0) {
                    mLevel_leftRadar = left;
                    mLevel_leftCenterRadar = leftCenter;
                    mLevel_rightCenterRadar = rightCenter;
                    mLevel_rightRadar = right;
                }
            }
        }else if(i == FRONT_RADAR){
            if(left>=0 ||leftCenter>=0 ||rightCenter>=0||right>=0){
                mLevel_leftRadar_front = left;
                mLevel_leftCenterRadar_front = leftCenter;
                mLevel_rightCenterRadar_front = rightCenter;
                mLevel_rightRadar_front = right;
            }
        }
    }

    public final void analyseData(byte data[],int len){
        if (len < 9) {
            return;
        }

        int ID =controlID(data);

        int type = data[7] & 0xFF;
        if(ID != 0x7053a ){
            return ;
        }
        Log.d(TAG,"ops analyseData data");

        if(ID == 0x7053a){
            switch (type){
                case 0x30:
                    // P key
                    requestShow =(int) (data[8] & 0xff);
                    if(requestShow==0 ||requestShow==1) {
                        mRadarHandler.removeCallbacks(radarRunnable);
                        if(requestShow==0){
                            mRadarHandler.post(radarRunnable);
                        }else {
                            mRadarHandler.postDelayed(radarRunnable,200);
                        }
                    }
                    break;
                case 0x31:
                    //radar sound
                    break;
                default:
                    break;
            }
        }

    }

    public int controlID(byte[] data) {
        int backcarControlID = (int) ((data[3] & 0xFF) << 24)
                | (int) ((data[4] & 0xFF) << 16)
                | (int) ((data[5] & 0xFF) << 8) | (data[6]) & 0xFF;
        return backcarControlID;
    }

    Handler mRadarHandler = new Handler();
    Runnable radarRunnable = new Runnable() {
        @Override
        public void run() {
            if(requestShow==1) {
                int step = FlyBackcarUI.getInstance().getmStep();
                if(step == 1||step==2){
                    requestShow =0;
                    Log.d(TAG,"backcar step = "+step+",no show ops,return");
                    return;
                }
                FlyBackcarOps.getInstance().showView(1);
                Log.d(TAG,"request show ops");
            }else if(requestShow == 0){
                FlyBackcarOps.getInstance().hideView(0);
                Log.d(TAG,"request hide ops");
            }
        }
    };

    public void setRadarSoundStatus(int status){
        mRadarSound = status;
    }
    public int getRadarSoundStatus(){
        return mRadarSound;
    }

    private void MakeAndSendMessage(int controlID,byte type,byte[]params){

        FlyBackcarUI.getInstance().MakeAndSendMessageToHal(controlID,type,params);
    }

    public final void requestOpsShow(int command ){
            byte data[] = {(byte)0xff,(byte)0x55,0x08,0x00,0x07,0x05,(byte)0x3a,(byte)0x30,0x00,0x00};
            data[8] =(byte) command;
            analyseData(data,data.length);
    }

}
