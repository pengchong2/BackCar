package com.autochips.android.backcar.ui;

import android.content.Context;
import android.content.Intent;

import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;

import com.autochips.android.backcar.Native;
import com.autochips.android.backcar.SkinResource;
import com.autochips.android.backcar.service.BackcarService;
import com.autochips.android.backcar.tool.PageManager;
import com.autochips.android.backcar.tool.PageManagerXMLParaser;
import com.autochips.android.backcar.tool.XMLTool;
import com.autochips.android.backcar.view.AutoFitTextureView;
import com.autochips.android.backcar.view.Camera2Preview;
import com.autochips.android.backcar.view.CameraPreview;
import com.autochips.android.backcar.view.CameraSurfaceView;
import com.autochips.android.backcar.view.FlyVideoView;
import cn.flyaudio.sdk.FlySDKManager;
import cn.flyaudio.sdk.listener.SystemListener;
import cn.flyaudio.sdk.manager.FlySystemManager;
import android.hardware.Camera;
import android.graphics.PixelFormat;
import android.widget.TextView;
import android.graphics.Color;
/**
 * Created by chengzihong on 18-5-31.
 */

public class FlyBackcarUI implements SystemListener {
    private String TAG = "backcar";

    private ImageView leftRadar;
    private ImageView rightRadar;
    private ImageView leftCenterRadar;
    private ImageView rightCenterRadar;

    private ImageView leftRadar_front;
    private ImageView rightRadar_front;
    private ImageView leftCenterRadar_front;
    private ImageView rightCenterRadar_front;

    private ImageView leftUpOutRadar_middle;
    private ImageView leftUpInsideRadar_middle;
    private ImageView leftDownInsideRadar_middle;
    private ImageView leftDownOutRadar_middle;

    private ImageView rightUpOutRadar_middle;
    private ImageView rightUpInsideRadar_middle;
    private ImageView rightDownInsideRadar_middle;
    private ImageView rightDownOutRadar_middle;

    private ImageView m_backcar_track;
    private Button m_backcar_view_zoom;
    public  ImageView m_backcar_aux_line;

    private int radarType = -1;
    private int FRONT_RADAR = 0;
    private int REAR_RADAR = 1;
    private int LEFT_MID_RADAR = 2;
    private int RIGHT_MID_RADAR = 3;

    private Context mContext = null;

    private WindowManager wm;
    private WindowManager.LayoutParams params = new WindowManager.LayoutParams();
    private CameraManager mCM = null;
    private View mLayoutView = null;
    private AutoFitTextureView mTextureView = null;

    private FlyVideoView mFlyVideoView = null;
    private Camera mCamera = null;

    private boolean mIsViewAdded = false;
    public Camera2Preview mCamera2Preview = null;
    public Handler mHandler = null;

    public static final int REMOVE_VIEW = 0;

    private static FlyBackcarUI instance = null;

    public static int fd = -1;
    public boolean debug = true;
    public boolean firstTimeBackcar = false;
    private String mLayoutName = null;
    private boolean bShowVideo = false;

    private Drawable m_image=null;
    private boolean bTrack = false;
    private int min_angle = 39;
    private int max_angle = 141;
    private int trackID = 0x7053a;

    private String m_TrackLevel = null;
    private boolean bTrackProperty = false;
    private String m_ImageNamePre = "track" ;
    private int m_CurrentAngle = 0;


    private int mLevel_leftRadar;
    private int mLevel_rightRadar;
    private int mLevel_leftCenterRadar;
    private int mLevel_rightCenterRadar;

    private int mLevel_leftRadar_front;
    private int mLevel_rightRadar_front;
    private int mLevel_leftCenterRadar_front;
    private int mLevel_rightCenterRadar_front;

    private int mLevel_leftUpOutRadar_middle;
    private int mLevel_leftUpInsideRadar_middle;
    private int mLevel_leftDownInsideRadar_middle;
    private int mLevel_leftDownOutRadar_middle;

    private int mLevel_rightUpOutRadar_middle;
    private int mLevel_rightUpInsideRadar_middle;
    private int mLevel_rightDownInsideRadar_middle;
    private int mLevel_rightDownOutRadar_middle;
    private int mStep = -1;
    private boolean mIsShowTrackFinish = true;
    private int mVideoZoom = 0 ;
    private final String VideoZoom_TAG= "00070512";
    private final String VIDEO_ZOOM = "VIDEO_ZOOM";

     private WindowManager wmm = null;
    private WindowManager.LayoutParams param = new WindowManager.LayoutParams();
    private TextView mtextview = null;
    private static final int REMOVE_WARRING_VIEW = 3;
    private int mWidth,mHeight;

    private CameraSurfaceView mCameraSurfaceView = null;
    public CameraPreview mCameraPreview = new CameraPreview();

    public FlyBackcarUI(){

    }

    public static synchronized FlyBackcarUI getInstance(){
        if(instance == null){
            instance = new FlyBackcarUI();
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
        mWidth = params.width;
        mHeight = params.height;
        Log.d(TAG,"width = "+params.width+" height = "+params.height);
        params.x = 0;
        params.y = 0;
        params.alpha = 1;
        params.gravity = Gravity.LEFT | Gravity.TOP ;
        mCM = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        FlySystemManager.getInstance().setSystemListener(this);
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case REMOVE_VIEW:
                        Log.d(TAG, "mHandler  mIsViewAdded: "  + mIsViewAdded);
                        if (mIsViewAdded){
                            disableStatusbarExpand(false);
                            collapsePnanels();
                            wm.removeView(mLayoutView);
                            mIsViewAdded = false;
                        } else {
                            Log.d(TAG, "Handler : View is already removed");
                        }
                        break;
                        case REMOVE_WARRING_VIEW:
                        if(wmm!=null && mtextview!=null){
                            wmm.removeView(mtextview);
                            wmm = null;
                            mtextview = null;
                        }
                        break;
                    default:
                        break;
                }
                super.handleMessage(msg);
            }
        };
        if(mPageManager==null){
            parsexml();
        }
        fd = Native._open();
        Log.d(TAG,"Native open fd = "+fd);
        if(fd==-1){
            Log.d(TAG,"Native open failed for backar");
        }

        String name = null;
        if(this.mPageManager!=null){
            int pageID = 0x701;
            name = mPageManager.getLayoutName(pageID);
            if(radarType==-1) {
                if ("fly_backcarvideo_layout".equals(name)) {
                    this.radarType = 0;
                    mLayoutName = "mtk_"+name;
                } else if ("fly_backcarvideo_radar_layout".equals(name)) {
                    this.radarType = 1;
                    mLayoutName = "mtk_"+name;
                } else if("fly_backcarvideo_radar_golf_layout".equals(name)){
                    this.radarType = 2;
                    mLayoutName = "mtk_"+name;
                }else if ("fly_backcarvideo_radar_edge_layout".equals(name)) {
                    //only rear radar,zoom,rear3883
                    this.radarType = 3;
                    mLayoutName = "mtk_"+name;
                }else if("fly_backcarvideo_radar_edge_high_layout".equals(name)){
                    //rear and front ,left and right radar,zoom,front2332,rear2662
                    this.radarType = 4;
                    mLayoutName = "mtk_"+name;
                }else if("fly_backcarvideo_radar_rx5_layout".equals(name)){
                    //only rear radar
                    this.radarType = 1;
                    mLayoutName = "mtk_"+name;
                }
                else if("fly_backcarvideo_radar_mondeo_layout".equals(name)){
                    //only rear radar,track rear2662
                    this.radarType = 13;
                    mLayoutName = "mtk_"+name;
                }else if("fly_backcarvideo_radar_mondeo_low_layout".equals(name)){
                    //only rear radar,track,rear3883
                    this.radarType = 13;
                    mLayoutName = "mtk_"+name;
                }else if("fly_backcarvideo_radar_mondeo_mid_layout".equals(name)){
                    //rear and front radar ,track and zoom,front2332,rear2662
                    this.radarType = 14;
                    mLayoutName = "mtk_"+name;
                }else if("fly_backcarvideo_radar_outlander_layout".equals(name)){
                    //only rear radar ,7777
                    this.radarType = 15;
                    mLayoutName = "mtk_"+name;
                }
            }
            if(name !=null){
                Log.d(TAG,"bakcar config  :"+name+" radar type:"+this.radarType);
            }
        }

    }

    private void disableStatusbarExpand(boolean dis) {

        try {
            Object service = mContext.getSystemService("statusbar");
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
            Object service = mContext.getSystemService("statusbar");
            Class<?> statusbarManager = Class.forName("android.app.StatusBarManager");
            Method method = statusbarManager.getMethod("collapsePanels");
            method.invoke(service);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public final void processBackcarEvent(int event){
         if(event == 0){
             if(mLayoutView!=null){
                 if(m_backcar_aux_line!=null){
                     m_backcar_aux_line.setImageBitmap(null);
                     Log.d("track","hide aux line");
                     m_backcar_aux_line.invalidate();
                 }
                 if(m_backcar_track!=null){
                     m_backcar_track.setImageBitmap(null);
                     m_backcar_track.invalidate();
                 }
             }
             if(mLayoutView!=null){
                 mLayoutView.setVisibility(View.GONE);
                 mLayoutView.invalidate();
                 wm.removeView(mLayoutView);
                 Log.d(TAG,"exit backar ,remove view");
             }
             if(wmm!=null && mtextview!=null){
                 wmm.removeView(mtextview);
                 wmm = null;
                 mtextview = null;
             }            

             //close camera
             hideView();
             setmStep(4);
         }else if(event == 1){
             initShowView();
             showView();
             //横屏才显示倒车提醒
             if(mWidth>mHeight){
                  wmm = (WindowManager)mContext.getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
                  param.flags = WindowManager.LayoutParams.FLAG_FULLSCREEN|WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                  |WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
                 param.type =2016;
                 Display display = wm.getDefaultDisplay();
                Point p  = new Point();
                display.getRealSize(p);
                param.width = 310;
                param.height = 70;

                 param.alpha = 0.7f;
                 param.gravity = Gravity.CENTER ;
                 mtextview = new TextView(mContext.getApplicationContext());
                 mtextview.setTextSize(25);
                 mtextview.setText(SkinResource.getSkinStringByName("backcar_warningtxt"));
                 mtextview.setTextColor(Color.WHITE);
                 mtextview.setGravity(Gravity.CENTER);
                 wmm.addView(mtextview,param );
                 mHandler.sendEmptyMessageDelayed(3,3000);
             }
           

             /**
             if(mFlyVideoView!=null) {
                 mFlyVideoView.setVisibility(View.GONE);
                 mFlyVideoView.invalidate();
             }

             bShowVideo = true;
             if(mLayoutView!=null){
                 mLayoutView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                     @Override
                     public void onGlobalLayout() {
                         if(bShowVideo){
                             if(mFlyVideoView!=null) {
                                 mFlyVideoView.setVisibility(View.VISIBLE);
                                 mFlyVideoView.invalidate();
                             }
                             bShowVideo = false;
                             Log.d(TAG,"show video ");
                             showView();
                         }
                     }
                 });
             }
            **/
         }
    }

    public void initShowView(){

        if (!mIsViewAdded) {

            LayoutInflater inflater = LayoutInflater.from(SkinResource.getSkinContext());
            String layout = "backcar_camera_preview";
             if(mLayoutName!=null && this.radarType>-1){
                 layout = mLayoutName;
             }
            Log.d(TAG,"layout = "+layout+" radarType  = "+radarType);

            mLayoutView = inflater.inflate(SkinResource.getSkinLayoutIdByName(layout), null);


            //radarType = -1;
            if(mLayoutView!=null){
                //mFlyVideoView = (FlyVideoView) mLayoutView.findViewById(SkinResource.getSkinResourceId("mtk_camera", "id"));
               // mTextureView = (AutoFitTextureView) mLayoutView.findViewById(SkinResource.getSkinResourceId("mtk_camera", "id"));
                //mTextureView = (AutoFitTextureView) mLayoutView.findViewById(SkinResource.getSkinResourceId("backcar_texture", "id"));
                mCameraSurfaceView = (CameraSurfaceView)mLayoutView.findViewById(SkinResource.getSkinResourceId("mtk_camera", "id"));
                mCameraSurfaceView.setCameraPreview(mCameraPreview);

            }
            if(mLayoutView!=null) {
                if (radarType == 1 || radarType == 2 || radarType == 3 || radarType == 4 || radarType == 13 || radarType == 14 || radarType == 15) {
                    leftRadar = (ImageView) mLayoutView.findViewById(SkinResource.getSkinResourceId("radarrear_leftdown_out", "id"));
                    leftCenterRadar = (ImageView) mLayoutView.findViewById(SkinResource.getSkinResourceId("radarrear_leftdown_inside", "id"));
                    rightRadar = (ImageView) mLayoutView.findViewById(SkinResource.getSkinResourceId("radarrear_rightdown_out", "id"));
                    rightCenterRadar = (ImageView) mLayoutView.findViewById(SkinResource.getSkinResourceId("radarrear_rightdown_inside", "id"));
                    if (radarType == 13 || radarType == 14) {
                        //back track+rear radar
                        this.m_backcar_track = (ImageView) mLayoutView.findViewById(SkinResource.getSkinResourceId("backcar_track", "id"));
                        m_backcar_aux_line = (ImageView) mLayoutView.findViewById(SkinResource.getSkinResourceId("backcar_aux_line", "id"));
                        if (m_backcar_track != null) {
                            m_backcar_track.setVisibility(View.VISIBLE);
                            if (this.m_CurrentAngle > 0 && m_image == null) {
                                m_image = getTrackDrawable(m_CurrentAngle);
                                if (m_image != null) {
                                    m_backcar_track.setBackground(m_image);
                                    Log.d(TAG, "init track m_CurrentAngle = " + m_CurrentAngle);
                                    m_image = null;
                                }
                            }
                        }
                    }
                    if (radarType == 3 || radarType == 4 || radarType == 14) {
                        //back track +front and rear radar
                        this.m_backcar_view_zoom = (Button) mLayoutView.findViewById(SkinResource.getSkinResourceId("backcar_view_zoom", "id"));
                        if (m_backcar_view_zoom != null) {
                            m_backcar_view_zoom.setOnClickListener(listener);
                            mVideoZoom = Settings.Secure.getInt(mContext.getContentResolver(), VIDEO_ZOOM, 0);
                            sendMessageForVideoZoom(mVideoZoom);
                        }
                    }

                    onCarRadar(REAR_RADAR, mLevel_leftRadar, mLevel_leftCenterRadar, mLevel_rightCenterRadar, mLevel_rightRadar);
                }
                if (radarType == 1 || radarType == 2 || radarType == 4 || radarType == 14) {
                    leftRadar_front = (ImageView) mLayoutView.findViewById(SkinResource.getSkinResourceId("radarfront_leftup_out", "id"));
                    leftCenterRadar_front = (ImageView) mLayoutView.findViewById(SkinResource.getSkinResourceId("radarfront_leftup_inside", "id"));
                    rightRadar_front = (ImageView) mLayoutView.findViewById(SkinResource.getSkinResourceId("radarfront_rightup_out", "id"));
                    rightCenterRadar_front = (ImageView) mLayoutView.findViewById(SkinResource.getSkinResourceId("radarfront_rightup_inside", "id"));
                    onCarRadar(FRONT_RADAR, mLevel_leftRadar_front, mLevel_leftCenterRadar_front, mLevel_rightCenterRadar_front, mLevel_rightRadar_front);
                }
                if (radarType == 4) {
                    leftUpOutRadar_middle = (ImageView) mLayoutView.findViewById(SkinResource.getSkinResourceId("radarmid_leftup_out", "id"));
                    leftUpInsideRadar_middle = (ImageView) mLayoutView.findViewById(SkinResource.getSkinResourceId("radarmid_leftup_inside", "id"));
                    leftDownOutRadar_middle = (ImageView) mLayoutView.findViewById(SkinResource.getSkinResourceId("radarmid_leftdown_out", "id"));
                    leftDownInsideRadar_middle = (ImageView) mLayoutView.findViewById(SkinResource.getSkinResourceId("radarmid_leftdown_inside", "id"));

                    rightUpOutRadar_middle = (ImageView) mLayoutView.findViewById(SkinResource.getSkinResourceId("radarmid_rightup_out", "id"));
                    rightUpInsideRadar_middle = (ImageView) mLayoutView.findViewById(SkinResource.getSkinResourceId("radarmid_rightup_inside", "id"));
                    rightUpOutRadar_middle = (ImageView) mLayoutView.findViewById(SkinResource.getSkinResourceId("radarmid_rightdown_out", "id"));
                    rightUpInsideRadar_middle = (ImageView) mLayoutView.findViewById(SkinResource.getSkinResourceId("radarmid_rightdown_inside", "id"));
                    onCarRadar(LEFT_MID_RADAR, mLevel_leftUpOutRadar_middle, mLevel_leftUpInsideRadar_middle, mLevel_leftDownInsideRadar_middle, mLevel_leftDownOutRadar_middle);
                    onCarRadar(RIGHT_MID_RADAR, mLevel_rightUpOutRadar_middle, mLevel_rightUpInsideRadar_middle, mLevel_rightDownInsideRadar_middle, mLevel_rightDownOutRadar_middle);
                }
            }

            wm.addView(mLayoutView, params);
            mIsViewAdded = true;
            if(mLayoutView!=null){
                mLayoutView.setVisibility(View.VISIBLE);
                mLayoutView.invalidate();
            }

            if(mFlyVideoView!=null) {
                int width=1024;
                int height=600;
                if(params.width==960){
                    width = 965;
                    height = 670;
                }else if(params.width==768){
                    width = 768;
                    height = 536;
                }
                mFlyVideoView.setVideoSize(width, height);
                mFlyVideoView.initFlyVideoView();
            }

        } else {
            Log.d(TAG, "onCreate : View is already added");
        }
    }

    public void showView(){
        //initShowView();
        //api2
//        if( mCamera2Preview == null){
//            Log.d(TAG,"mCamera2Preview");
//            int rotation = wm.getDefaultDisplay().getRotation();
//            setBackcarFD(true);
//            try {
//                if (mTextureView != null) {
//                    mCamera2Preview = Camera2Preview.newInstance(mTextureView, mCM, rotation);
//                    Log.d(TAG, "enter backcar");
//                }
//            }catch(RuntimeException e){
//                Log.d(TAG,"open camera api2 exception");
//                e.printStackTrace();
//            }
//        }

        mCameraPreview.initCamera();
        /**  api1
        try {
            setBackcarFD(true);
            Camera.CameraInfo info = new Camera.CameraInfo();
            if(info!=null) {
                Camera.getCameraInfo(0, info);
                if(info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    this.mCamera = Camera.open(0);
                }
            }
        }catch (RuntimeException e){
            e.printStackTrace();
        }
        if(mFlyVideoView!=null){
            mFlyVideoView.setCamera(mCamera);
        }**/

    }

    public void hideView(){
        /**
        if(this.radarType>0){
            if(radarType == 1 ||radarType==2 ||radarType ==3 ||radarType == 4 ||radarType ==13 ||radarType == 14) {
                clearDataForExitBackcar(0x00,REAR_RADAR);
            }
            if(radarType==1 ||radarType ==2 ||radarType == 4||radarType==14){
                clearDataForExitBackcar(0x00,FRONT_RADAR);
            }
            if(radarType == 4){
                clearDataForExitBackcar(0x00,LEFT_MID_RADAR);
                clearDataForExitBackcar(0x00,RIGHT_MID_RADAR);
            }
        }**/

//        if(mCamera2Preview!=null){
//            mCamera2Preview.closeCamera();
//        }

        /**
        if(mCamera!=null){
            mFlyVideoView.setCamera(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }**/
        mCameraPreview.deInitCamera();
        setBackcarFD(false);
        mIsViewAdded = false;
        mCamera2Preview = null;
        Log.d(TAG,"close backcar mIsViewAdded="+mIsViewAdded);


    }

    public void setBackcarFD(boolean bOn)
    {
        File file = null;

        file = new File("/dev/flydev0");
        FileOutputStream fos = null;
        try
        {
            fos = new FileOutputStream(file,false);
            if(fos != null)
            {
                String status = "cvbs_back";
                if(!bOn) {
                    status = "cvbs_off";
                }

                Log.d("JumpPage", "set enter backcar FD ");
                //fos.write(new String("vehicle_open").getBytes());
                fos.write(status.getBytes());
                fos.close();
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }


    @Override
    public void onVolumeStatus(int i, int i1, int i2) {

    }

    @Override
    public void onScreenLightLevelStatus(int i) {

    }

    /**
     *
     * @param i        i=0 is front radar,i=1 is rear radar
     * @param left
     * @param leftCenter
     * @param rightCenter
     * @param right
     */
    @Override
    public void onCarRadar(int i, int left, int leftCenter, int rightCenter, int right) {

        if(debug) {
            Log.d(TAG, "showCarRadarmassage  left:  " + left);
            Log.d(TAG, "showCarRadarmassage  leftCenter:  " + leftCenter);
            Log.d(TAG, "showCarRadarmassage  rightCenter:  " + rightCenter);
            Log.d(TAG, "showCarRadarmassage  right:  " + right);
            Log.d(TAG, "showCarRadarmassage  0 front or 1 rear:  " + i);
        }
        if(left>=0 && left<255) {
            if(i==REAR_RADAR){
                if(radarType==4||radarType==14){
                    if(left>2 && left<255) { left = 2;}
                    if(leftCenter>6 && leftCenter<255){leftCenter=6;}
                    if(right>2&& right<255){right=2;}
                    if(rightCenter>6 && rightCenter<255){rightCenter=6;}
                }else if(radarType == 15){
                    if(left>7 && left<255) { left = 7;}
                    if(leftCenter>7 && leftCenter<255){leftCenter=7;}
                    if(right>7&& right<255){right=7;}
                    if(rightCenter>7 && rightCenter<255){rightCenter=7;}
                }
            }
            if(i==FRONT_RADAR){
                if(radarType==4||radarType==14){
                    if(left>2 && left<255) { left = 2;}
                    if(leftCenter>3 && leftCenter<255){leftCenter=3;}
                    if(right>2 && right<255){right=2;}
                    if(rightCenter>3 && rightCenter <255){rightCenter=3;}
                }
            }
            setRadarData(i, left, leftCenter, rightCenter, right);
        }
        if(leftRadar == null ||leftCenterRadar ==null){
            Log.d(TAG," leftRadar is null return");
            return;
        }
        //if(mIsViewAdded==false){return;}
        if(REAR_RADAR == i && this.radarType >0) {
            if(leftRadar==null ||leftCenterRadar==null||rightCenterRadar==null||rightRadar==null){
                Log.d(TAG,"Radar is null  ,return");
                return ;
            }
            if(leftRadar.getBackground()==null ||leftCenterRadar.getBackground()==null ||
                    rightRadar.getBackground() ==null ||rightCenterRadar.getBackground()==null ) {
                Log.d(TAG,"getBackground is null  ,return");
                return ;
            }

            if (left == FlySystemManager.RADAR_INIT) {
                leftRadar.getBackground().setLevel(0);
            } else {
                leftRadar.getBackground().setLevel(left);
            }
            if (leftCenter == FlySystemManager.RADAR_INIT) {
                leftCenterRadar.getBackground().setLevel(0);
            } else {
                leftCenterRadar.getBackground().setLevel(leftCenter);
            }
            if (right == FlySystemManager.RADAR_INIT) {
                rightRadar.getBackground().setLevel(0);
            } else {
                rightRadar.getBackground().setLevel(right);
            }
            if (rightCenter == FlySystemManager.RADAR_INIT) {
                rightCenterRadar.getBackground().setLevel(0);
            } else {
                rightCenterRadar.getBackground().setLevel(rightCenter);
                rightCenterRadar.invalidate();
            }
        }else if(FRONT_RADAR== i && this.radarType >0){
            if(this.radarType == 3){
                //edge only rear radar
                return ;
            }
            if(leftRadar_front==null ||leftCenterRadar_front==null||rightCenterRadar_front==null||rightRadar_front==null){
                Log.d(TAG,"Radar_front is null,return");
                return ;
            }
            if(leftRadar_front.getBackground()==null ||leftCenterRadar_front.getBackground()==null ||
                    rightRadar_front.getBackground() ==null ||rightCenterRadar_front.getBackground()==null ) {
                Log.d(TAG,"front getBackground is null ,return ");
                return ;
            }

            if (left == FlySystemManager.RADAR_INIT) {
                leftRadar_front.getBackground().setLevel(0);
            } else {
                leftRadar_front.getBackground().setLevel(left);
                leftRadar_front.invalidate();
            }

            if (leftCenter == FlySystemManager.RADAR_INIT) {
                leftCenterRadar_front.getBackground().setLevel(0);
            } else {
                leftCenterRadar_front.getBackground().setLevel(leftCenter);
                leftCenterRadar_front.invalidate();
            }

            if (right == FlySystemManager.RADAR_INIT) {
                rightRadar_front.getBackground().setLevel(0);
            } else {
                rightRadar_front.getBackground().setLevel(right);
                rightRadar_front.invalidate();
            }

            if (rightCenter == FlySystemManager.RADAR_INIT) {
                rightCenterRadar_front.getBackground().setLevel(0);
            } else {
                rightCenterRadar_front.getBackground().setLevel(rightCenter);
                rightCenterRadar_front.invalidate();
            }
        }else if(i==LEFT_MID_RADAR && this.radarType == 4){
            if(leftUpOutRadar_middle==null ||leftUpInsideRadar_middle==null||leftDownOutRadar_middle==null||leftDownInsideRadar_middle==null){
                Log.d(TAG,"left Radar middle is null");
                return ;
            }
            if(leftUpOutRadar_middle.getBackground()==null ||leftUpInsideRadar_middle.getBackground()==null||
                    leftDownInsideRadar_middle.getBackground()==null||leftDownOutRadar_middle.getBackground()==null){
                Log.d(TAG,"left Radar middle getBackground is null");
                return;
            }

            if (left == FlySystemManager.RADAR_INIT) {
                leftUpOutRadar_middle.getBackground().setLevel(0);
                leftUpOutRadar_middle.invalidate();
            } else {
                leftUpOutRadar_middle.getBackground().setLevel(left);
                leftUpOutRadar_middle.invalidate();
            }
            if (leftCenter == FlySystemManager.RADAR_INIT) {
                leftUpInsideRadar_middle.getBackground().setLevel(0);
            } else {
                leftUpInsideRadar_middle.getBackground().setLevel(leftCenter);
                leftUpInsideRadar_middle.invalidate();
            }
            if (right == FlySystemManager.RADAR_INIT) {
                leftDownInsideRadar_middle.getBackground().setLevel(0);
            } else {
                leftDownInsideRadar_middle.getBackground().setLevel(right);
                leftDownInsideRadar_middle.invalidate();
            }
            if (rightCenter == FlySystemManager.RADAR_INIT) {
                leftDownOutRadar_middle.getBackground().setLevel(0);
            } else {
                leftDownOutRadar_middle.getBackground().setLevel(rightCenter);
                leftDownOutRadar_middle.invalidate();

            }

        }else if(i==RIGHT_MID_RADAR && this.radarType == 4){
            if(rightUpOutRadar_middle==null ||rightUpInsideRadar_middle==null||rightDownOutRadar_middle==null||rightDownInsideRadar_middle==null){
                Log.d(TAG,"right radar middle is null");
                return ;
            }
            if(rightUpOutRadar_middle.getBackground()==null||rightUpInsideRadar_middle.getBackground()==null||
                    rightDownInsideRadar_middle.getBackground()==null||rightDownOutRadar_middle.getBackground()==null){
                Log.d(TAG,"right radar middle getBackground is null");
                return;
            }
            if (left == FlySystemManager.RADAR_INIT) {
                rightUpOutRadar_middle.getBackground().setLevel(0);
                rightUpOutRadar_middle.invalidate();
            } else {
                rightUpOutRadar_middle.getBackground().setLevel(left);
                rightUpOutRadar_middle.invalidate();
            }
            if (leftCenter == FlySystemManager.RADAR_INIT) {
                rightUpInsideRadar_middle.getBackground().setLevel(0);
                rightUpInsideRadar_middle.invalidate();
            } else {
                rightUpInsideRadar_middle.getBackground().setLevel(leftCenter);
                rightUpInsideRadar_middle.invalidate();
            }
            if (right == FlySystemManager.RADAR_INIT) {
                rightDownOutRadar_middle.getBackground().setLevel(0);
                rightDownOutRadar_middle.invalidate();
            } else {
                rightDownOutRadar_middle.getBackground().setLevel(right);
                rightDownOutRadar_middle.invalidate();
            }
            if (rightCenter == FlySystemManager.RADAR_INIT) {
                rightDownInsideRadar_middle.getBackground().setLevel(0);
                rightDownInsideRadar_middle.invalidate();
            } else {
                rightDownInsideRadar_middle.getBackground().setLevel(rightCenter);
                rightDownInsideRadar_middle.invalidate();
            }
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

    public final void MakeAndSendMessage(int ControlID, byte ControlType,byte[] param){

        int sendbufLen = 9 + param.length;
        if(sendbufLen>512){return;}

        byte[] sendbuf = new byte[sendbufLen];
        sendbuf[0] = (byte) 0xff;
        sendbuf[1] = 0x55;
        sendbuf[2] = (byte) (param.length + 6);
        sendbuf[3] = (byte) ((ControlID >> 24) & 0xff);
        sendbuf[4] = (byte) ((ControlID >> 16) & 0xff);
        sendbuf[5] = (byte) ((ControlID >> 8) & 0xff);
        sendbuf[6] = (byte) (ControlID & 0xff);
        sendbuf[7] = ControlType;
        for (int i = 0; i < param.length; i++) {
            sendbuf[8 + i] = param[i];
        }


        Bundle bundle = new Bundle();
        bundle.putByteArray("data", sendbuf);
        bundle.putInt("len", sendbufLen);
        FlySystemManager.getInstance().sendMessage(bundle);
    }

    private void parsexml(){
        String carselectPath = SystemProperties.get("persist.fly.car.select","default");
        carselectPath = "/flysystem/flyconfig/"+ carselectPath + "/uiconfig/pagemap.xml";
        File file = new File(carselectPath);
        if(!file.exists()){
            file = new File("/flysystem/flyconfig/default/uiconfig/pagemap.xml");
        }
        Log.d("backar","backcar car type is"+carselectPath);

        InputStream inStream = null;
        try {
            inStream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Log.d("backar","openFileInput pagemap.xml error");
        }

        if(inStream != null){
            PageManagerXMLParaser parser = new PageManagerXMLParaser(this.mContext);
            try {
                mPageManager = ((PageManagerXMLParaser) XMLTool.parse(inStream, parser)).getPageManager();
            }catch (Exception e){
                Log.d("backar","parse xml for backcar exception");
                e.printStackTrace();
            }
        }
        //END

    }

    private PageManager mPageManager = null;

    public String getLayoutName(int pageId) {
        return mPageManager != null ? mPageManager.getLayoutName(pageId) : null;
    }

    public boolean isPageValuable(int pageId){
        return mPageManager != null ? mPageManager.isPageValuable(pageId) : false;
    }

    public final void MakeAndSendMessageToHal(int ControlID, byte ControlType,byte[] param){
        int sendbufLen = 9 + param.length;
        if(sendbufLen>512){return;}

        byte[] sendbuf = new byte[sendbufLen];
        sendbuf[0] = (byte) 0xff;
        sendbuf[1] = 0x55;
        sendbuf[2] = (byte) (param.length + 6);
        sendbuf[3] = (byte) ((ControlID >> 24) & 0xff);
        sendbuf[4] = (byte) ((ControlID >> 16) & 0xff);
        sendbuf[5] = (byte) ((ControlID >> 8) & 0xff);
        sendbuf[6] = (byte) (ControlID & 0xff);
        sendbuf[7] = ControlType;
        for (int i = 0; i < param.length; i++) {
            sendbuf[8 + i] = param[i];
        }
        if(fd>-1) {
           Native._write(sendbuf, sendbufLen);
        }

    }

    public void requestHalControlBackLight(int time){
        /**
        if(firstTimeBackcar==false){
            time = 1500;
            firstTimeBackcar = true;
        }
        if("1".equals(SystemProperties.get("agreepage.state.displaying"))){
            time = 3000;
        }**/

        int controlID = 0x00;
        byte controlType =(byte) 0x10;
        byte param[] = {0x00,0x00,0x00,0x00};
        param[0] = (byte) ((time >> 24) & 0xff);
        param[1] = (byte) ((time >> 16) & 0xff);
        param[2] = (byte) ((time >> 8) & 0xff);
        param[3] = (byte) (time & 0xff);

        MakeAndSendMessageToHal(controlID,controlType,param);
        if(debug) {
            Log.d(TAG, "backcar request hal to control back light time = "+time);
        }
    }

    public void setBackcarStatus(boolean bOnBackcar){
        Intent intent = new Intent("com.autochips.android.backcar.status");
        //Intent enableIntent =new Intent("flyaudio.intent.action.CONTROL_VOICE");

        if(bOnBackcar){
            intent.putExtra("backcar","enter");
            SystemProperties.set("com.autochips.backckar.status","enter");
            //enableIntent.putExtra("ENABLE_VOICE","disable_voice");
            //mContext.sendBroadcast(enableIntent);

        }else{
            intent.putExtra("backcar","exit");
            SystemProperties.set("com.autochips.backckar.status","exit");
            //enableIntent.putExtra("ENABLE_VOICE","enable_voice");
            //mContext.sendBroadcast(enableIntent);

        }
        mContext.sendBroadcast(intent);

    }

    public void updateTrack(int level){
        if(this.m_backcar_track==null){
            return;
        }
        if(this.m_backcar_track!=null){
            m_backcar_track.setVisibility(View.VISIBLE);
        }
        m_image = getTrackDrawable(level);
        if(m_image!=null) {
            m_backcar_track.setBackground(m_image);
            m_CurrentAngle = level;
            m_image = null;
        }
    }

    public int controlID(byte[] data) {
        int backcarControlID = (int) ((data[3] & 0xFF) << 24)
                | (int) ((data[4] & 0xFF) << 16)
                | (int) ((data[5] & 0xFF) << 8) | (data[6]) & 0xFF;
        return backcarControlID;
    }

    public Drawable getTrackDrawable(int angle){
        Drawable image = null;

        if(!bTrackProperty){
            m_TrackLevel = SystemProperties.get("fly.backcar.tracklevel",null);

            if(m_TrackLevel==null){
                m_ImageNamePre = null;
            }else if("0".equals(m_TrackLevel)){
                m_ImageNamePre = "track1_";
            }else if("1".equals(m_TrackLevel)){
                m_ImageNamePre = "track2_";
            }else if("2".equals(m_TrackLevel)){
                m_ImageNamePre = "track3_";
            }else if("3".equals(m_TrackLevel)){
                m_ImageNamePre = "track4_";
            }else if("4".equals(m_TrackLevel)){
                m_ImageNamePre = "track5_";
            }else{
                bTrackProperty = false;
            }
            if(m_TrackLevel!=null){
                bTrackProperty = true;
                Log.d(TAG,"track iamge pre = "+m_ImageNamePre);
            }
        }
        if(m_ImageNamePre == null){
            Log.d(TAG,"NO config btrack =false");
            return null;
        }else{
            bTrack = true;
        }
        String imageName = m_ImageNamePre+String.valueOf(angle);
        try{
            image = SkinResource.getSkinDrawableByName(imageName);
        }catch(Exception e){
            e.printStackTrace();
            Log.d(TAG,"get Image error");
            return null;
        }
        Log.d("track" ,"track imageName = "+imageName+image);
        return image;
    }

    private void setRadarData(int index ,int left,int leftCenter,int rightCenter,int right){
        if(index == REAR_RADAR && this.radarType>0){
            if(left>=0 ||leftCenter>=0 ||rightCenter>=0||right>=0){
                mLevel_leftRadar = left;
                mLevel_leftCenterRadar = leftCenter ;
                mLevel_rightCenterRadar = rightCenter;
                mLevel_rightRadar = right;
                Log.d(TAG,"save radar data");
            }
        }else if(index == FRONT_RADAR){
            if(left>=0 ||leftCenter>=0 ||rightCenter>=0||right>=0){
                mLevel_leftRadar_front = left;
                mLevel_leftCenterRadar_front = leftCenter;
                mLevel_rightCenterRadar_front = rightCenter;
                mLevel_rightRadar_front = right;
            }
        }else if(index == LEFT_MID_RADAR){
            if(left>=0 ||leftCenter>=0 ||rightCenter>=0||right>=0){
                mLevel_leftUpOutRadar_middle = left;
                mLevel_leftUpInsideRadar_middle = leftCenter;
                mLevel_leftDownInsideRadar_middle = rightCenter;
                mLevel_leftDownOutRadar_middle = right;
            }
        }else if(index == RIGHT_MID_RADAR){
            if(left>=0 ||leftCenter>=0 ||rightCenter>=0||right>=0){
                mLevel_rightUpOutRadar_middle = left;
                mLevel_rightUpInsideRadar_middle = leftCenter;
                mLevel_rightDownInsideRadar_middle = rightCenter;
                mLevel_rightDownOutRadar_middle = right;
            }
        }

    }

    public void analyseData(byte data[],int len){
            int controlID = controlID(data);
            //level must be init 0 for +
            int type = data[7] & 0xFF;
            if(controlID!= 0x7053a){return;}
            int level = 0;
            switch (type){
                case 0x20:
                    if(data.length<12){return;}
                    level += (int) ((data[8] << 24) & 0xFF000000);
                    level += (int) ((data[9] << 16) & 0xFF0000);
                    level += (int)((data[10] << 8) & 0xFF00);
                    level += (int) (data[11] & 0xFF);
                    if(level>0 && level<65000){
                        if(level==m_CurrentAngle){
                            return;
                        }
                        if((level%100) == 50){
                            //Log.d(TAG,"miss track angle "+level);
                            return ;
                        }
                        m_CurrentAngle = level;
                        Log.d(TAG,"track angle is "+level);
                    }else{
                        return;
                    }
                    if(mIsShowTrackFinish) {
                        mIsShowTrackFinish = false;
                        mTrackHandler.removeCallbacks(trackRunnable);
                        mTrackHandler.postDelayed(trackRunnable, 40);
                    }else{
                        //Log.d(TAG,"lost track angle = "+level);
                    }
                    break;
                case 0x40:
                    // video zoom switch,0 small ,1 big
                    int command = (int)(data[8] & 0xff);
                    updateVideoZoom(command);
                    break;
                case 0x41:
                    //video delay switch ,0 close,1 open
                    break;
                default:
                    break;
            }
    }

    Handler mTrackHandler = new Handler();
    Runnable trackRunnable = new Runnable() {
        @Override
        public void run() {
            if(m_CurrentAngle >0) {
                FlyBackcarUI.getInstance().updateTrack(m_CurrentAngle);
                mIsShowTrackFinish = true;
            }
        }
    };


    public void clearDataForExitBackcar(int type,int index){
        if(type==0x00){
            //radar data
            switch (index){
                case 0x00:
                    mLevel_leftRadar_front = 0;
                    mLevel_leftCenterRadar_front = 0;
                    mLevel_rightCenterRadar_front = 0;
                    mLevel_rightRadar_front = 0;
                    break;
                case 0x01:
                    mLevel_leftRadar = 0;
                    mLevel_leftCenterRadar=0;
                    mLevel_rightCenterRadar = 0;
                    mLevel_rightRadar = 0;
                    break;
                case 0x02:
                    mLevel_leftUpOutRadar_middle = 0;
                    mLevel_leftUpInsideRadar_middle = 0;
                    mLevel_leftDownInsideRadar_middle = 0;
                    mLevel_leftDownOutRadar_middle = 0;
                    break;
                case 0x03:
                    mLevel_rightUpOutRadar_middle = 0;
                    mLevel_rightUpInsideRadar_middle = 0;
                    mLevel_rightDownInsideRadar_middle = 0;
                    mLevel_rightDownOutRadar_middle = 0;
                    break;
                default:
                    break;
            }
        }else if(type ==0x01){
            //track angle data,no need to clear,need it anytime for backcar

        }
    }

    public void setmStep(int step){
        mStep = step;
    }

    public int getmStep(){
        return mStep;
    }

    public String getLayoutName(){
        return mLayoutName;
    }
    public int getRadarType(){
        return radarType;
    }

    private void sendMessageForVideoZoom(int command ){
        byte data[] = {0x00};
        data[0] = (byte)command;
        byte type = (byte)0x40;
        MakeAndSendMessageToHal(0x00,type,data);
    }

    View.OnClickListener listener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String tag = (String)v.getTag();
            if(VideoZoom_TAG.equals(tag)){
                if(mVideoZoom ==0){
                    mVideoZoom = 1;
                    sendMessageForVideoZoom(mVideoZoom);
                }else if(mVideoZoom == 0){
                    mVideoZoom =1;
                    sendMessageForVideoZoom(mVideoZoom);
                }
                Log.d(TAG,"zoom view click and set "+mVideoZoom);
            }
        }
    };

    private void updateVideoZoom(int command){
        if(command == 0||command==1){
            Settings.Secure.putInt(mContext.getContentResolver(),VIDEO_ZOOM,command);
            if(m_backcar_view_zoom!=null){
                m_backcar_view_zoom.getBackground().setLevel(command);
                m_backcar_view_zoom.invalidate();
            }
            Log.d(TAG,"updateVideoZoom command = "+command);
        }
    }

}
