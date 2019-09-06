package com.autochips.android.backcar;

import android.util.Log;

public class Native {
	 static {
	        // The runtime will add "lib" on the front and ".o" on the end of
	        // the name supplied to loadLibrary.
	        /*
	         * 加载了libcomjni.so库，注意这个路径是com.android.aidltest.Native
	         * 如果comjni注册时不是用这个路径，那么加载会失败
	         */

	    try
		{
			Log.d("BackcarService","BackcarService load hal so");
			System.load("/flysystem/lib/libBackcarJni.so");

		} catch (Exception e)
		{
			Log.d("BackcarService"," not found hal so !!");
			// TODO: handle exception
		}
	    }
	    
	    /*这是libcomjni.so全部的接口函数*/
		native public static int _open();
	    native public static int _write(byte[] buf, int len);
	    native public static boolean _close();
	    native public static int _read(byte[] buf,int len);

}

