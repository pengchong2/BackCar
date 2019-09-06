package com.autochips.android.backcar;

import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;

public class Backcar_GPIO {

    private static final String TAG = "BackcarGPIO";
    private static final int MODE = 0;
    private static final int INPUT_VALUE = 2;
    private static final int DIRECTION = 5;
    private static final String path = "/sys/devices/platform/caci/cmd";
    private static final int GPIO_HIGH = 1;
    private static final int GPIO_LOW = 0;

    private static int mLastGPIOValue = GPIO_LOW;


    public static final int BACKCAR_STOP = 0;
    public static final int BACKCAR_START = 1;
    public static final int BACKCAR_ERROR = 2;

    static {
        System.loadLibrary("backcar_jni");
    }

    public Backcar_GPIO() {
        Log.i(TAG, "construct");
    }

    //    static public int Backcar_Get_GPIO_Status() {
//        FileReader reader = null;
//        BufferedReader br = null;
//        String line_string = null;
//        int gpio_status = -1;
//        boolean bfind = false;
//
//        try {
//            reader = new FileReader("/sys/class/misc/mtgpio/pin");
//            br = new BufferedReader(reader);
//            line_string = br.readLine();
//
//            while(line_string != null) {
//                bfind = line_string.trim().startsWith("66:");
//                if(bfind) {
//                    line_string = line_string.replaceAll("66:","");
//                    line_string = line_string.trim();
//                    //Log.i(TAG, "the match string([MODE] [PULL_SEL] [DIN] [DOUT] [PULL EN] [DIR] [IES] [SMT]) is " + line_string);
//                    /*0 indicate general gpio; 1 indicate output, 0 indicate input*/
//                    if((line_string.charAt(MODE) == '0') && (line_string.charAt(DIRECTION) == '0')) {
//                        if (line_string.charAt(INPUT_VALUE) == '1') {
//                            gpio_status = GPIO_HIGH;
//                        } else {
//                            gpio_status = GPIO_LOW;
//                        }
//                    }
//                    break;
//                }
//
//                line_string = br.readLine();
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//            Log.e(TAG, "some exception have happened");
//        } finally {
//            if (br != null) {
//                try {
//                    reader.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//        return gpio_status;
//    }
    //For IPO
    static public void resetGpioFlag() {
        Log.d(TAG, "reset gpio flg to low");
//        mLastGPIOValue = GPIO_LOW;
        mLastGPIOValue = GPIO_LOW; //1进入倒车，0退出倒车
    }

    //
    static public int GetBackcarEvent() {
//        int currentGPIOValue = 0;
        int currentGPIOValue = 1;
        int dwARM2Status = 0;
        int BCEvent = BACKCAR_ERROR;

        currentGPIOValue = Backcar_Get_GPIO_Status();

        Log.e(TAG, "GetBackcarEvent(): Last Evt Val=" + mLastGPIOValue + "Current Evt Val =" + currentGPIOValue);

//****************************高电平触发倒车****************************
//        if (mLastGPIOValue != currentGPIOValue) {
//            if (GPIO_LOW == currentGPIOValue) {
//                Log.e(TAG, "GetBackcarEvent(): Stop! \r\n");
//                BCEvent = BACKCAR_STOP;
//            } else if (GPIO_HIGH == currentGPIOValue) {
//                Log.e(TAG, "GetBackcarEvent(): Start! \r\n");
//                BCEvent = BACKCAR_START;
//            } else {
//                BCEvent = BACKCAR_ERROR;
//            }
//            mLastGPIOValue = currentGPIOValue;
//        }

//****************************低电平触发倒车****************************

        if (mLastGPIOValue != currentGPIOValue) {
            if ( GPIO_HIGH== currentGPIOValue) {
                Log.e(TAG, "GetBackcarEvent(): Stop! \r\n");
                BCEvent = BACKCAR_STOP;
            } else if (GPIO_LOW == currentGPIOValue) {
                Log.e(TAG, "GetBackcarEvent(): Start! \r\n");
                BCEvent = BACKCAR_START;
            } else {
                BCEvent = BACKCAR_ERROR;
            }
            mLastGPIOValue = currentGPIOValue;
        }

        Log.e(TAG, "GetBackcarEvent(): Leave! \r\n");
        return BCEvent;
    }

    //************ inform arm2 to exit *********
    static public void TakeOverControl() {
        Log.i(TAG, "TakeOverControl start");
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path)));
            // inform arm 1 is ready
            out.write("1");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (null != out) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Log.i(TAG, "TakeOverControl end");
    }

    public static void takeoverfromArm2(){
        Log.i(TAG, "taveoverfromArm2 enter");
        notify_Arm2_Android_System_Ready();
        Log.i(TAG, "taveoverfromArm2 leave");
    }

    private native static void notify_Arm2_Android_System_Ready();


    private static int pmState = -1;
//    private static String pmFile = "/dev/flyaudio_event";
//    private static String pmFile = "/dev/flyaudio_detect_carback";  // read from driver
      private static String pmFile = "/dev/flyaudio_carback_transpond"; //read from hal

    static public int Backcar_Get_GPIO_Status(){

        pmState = readFromFile(pmFile);
        Log.d(TAG, "Backcar_Get_GPIO_Status: " + pmState);
        return pmState;
    }


    public static int readFromFile(String fileName){
        try {
            int temp = -1;
            RandomAccessFile raf = new RandomAccessFile(fileName, "r");
            temp = raf.readUnsignedByte();
            return temp;
        } catch (FileNotFoundException e){
            // TODO Auto-generated catch block
            Log.d(TAG, "Backcar_Get_GPIO_Status FileNotFoundException: " + e.toString());
            return -1;
        } catch (IOException e){
            Log.d(TAG, "Backcar_Get_GPIO_Status IOException: " + e.toString());
            // TODO Auto-generated catch block
            return -1;
        }
    }


}
