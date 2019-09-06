package com.autochips.android.backcar;

import java.io.InputStream;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

/**
 * 用于加载包括其他apk中的资源（raw，layout，drawable，values/strings,colors）
 * <p>
 * 加载其他APK的资源，如果找不到就加载本地资源
 */
public class SkinResource {

	public static final String TAG = "SkinResource";
	private static Context mSkinContext = null;
	private static Context mLocalContext = null;

	public static Context getSkinContext() {
		return mSkinContext;
	}

	public static void initSkinResource(Context context, String pkgName) {
		try {
			mLocalContext = context;
			mSkinContext = context;
			mSkinContext = context.createPackageContext(pkgName, Context.CONTEXT_IGNORE_SECURITY);
			Log.d(TAG, "The skin resource packageName:" + pkgName);
		} catch (NameNotFoundException e) {
			Log.e(TAG, "Can't find the skin resource packageName:" + pkgName);
			e.printStackTrace();
			mSkinContext = context;
		}
	}

	private static int getIdentifier(String name, String type, Context context) {
		try {
			return context.getResources().getIdentifier(name, type, context.getPackageName());
		} catch (Exception e) {
			String packageName = null;
			if(context!=null){
				 packageName = context.getPackageName();}
			final String str = "getIdentifier() Fail!! name:" + name + " type:" + type + " packageName:"
					+ packageName;
			Log.e(TAG, str);
			e.printStackTrace();
			return 0;
		}
	}
	
	/**
	 * 皮肤包找不到，找本地
	 * @param name
	 * @param type
	 * @return
	 */
	private static int getIdentifier(String name,String type) {
		int id = getIdentifier(name, type, mSkinContext);
		if (id == 0 && mSkinContext != mLocalContext) {
			Log.d(TAG, "getIdentifier id == 0 ,name="+name+",type=="+type);
			id = getIdentifier(name, type, mLocalContext);
		}
		return id;
	}

	public static int getSkinDrawableIdByName(String name) {
		return getIdentifier(name, "drawable");
	}
	
	public static int getSkinLayoutIdByName(String name) {
		return getIdentifier(name, "layout");
	}
	
	public static int getSkinResourceId(String name, String type) {
		return getIdentifier(name, type);
	}
	
	public static InputStream getSkinRawInputStream(String name) {
		Context context = mSkinContext;
		int rawId = getIdentifier(name, "raw", context);
		if (rawId == 0 && mSkinContext != mLocalContext) {
			context = mLocalContext;
			rawId = getIdentifier(name, "raw", context);
		}
		if (rawId == 0){
			return null;}
		return context.getResources().openRawResource(rawId);
	}

	public static View getSkinLayoutViewByName(String name) {
		Context context = mSkinContext;
		int id = getIdentifier(name, "layout", context);
		if (id == 0 && mSkinContext != mLocalContext) {
			context = mLocalContext;
			id = getIdentifier(name, "layout", context);
		}
		if(id == 0){
			return null;}
		LayoutInflater mInflater = LayoutInflater.from(context);
		return mInflater.inflate(id, null);
	}

	public static String getSkinStringByName(String name) {
		Context context = mSkinContext;
		int id = getIdentifier(name, "string", context);
		if (id == 0 && mSkinContext != mLocalContext) {
			context = mLocalContext;
			id = getIdentifier(name, "string", context);
		}
		if (id == 0) {
			return null;
		}
		return context.getResources().getString(id);
	}

	public static Drawable getSkinDrawableByName(String name) {
		Context context = mSkinContext;
		int id = getIdentifier(name, "drawable", context);
		if (id == 0 && mSkinContext != mLocalContext) {
			context = mLocalContext;
			id = getIdentifier(name, "drawable", context);
		}
		if (id == 0) {
			return null;
		}
		return context.getResources().getDrawable(id);

	}

	public static ColorStateList getSkinColorStateList(String name) {
		Context context = mSkinContext;
		int id = getIdentifier(name, "drawable", context);
		if (id == 0 && mSkinContext != mLocalContext) {
			context = mLocalContext;
			id = getIdentifier(name, "drawable", context);
		}
		if (id == 0) {
			return null;
		}
		return context.getResources().getColorStateList(id);
	}

	/**
	 * 注意顺序问题
	 * @param names
	 * @return
	 */
	public static int[] getSkinStyleableIdByName(String[] names) {
		int[] result = new int[names.length];
		for (int i = 0; i < names.length; i++) {
			result[i] = getIdentifier(names[i], "attr");
		}
		return result;
	}
	
	private static class Result{
		private final int id;
		private final Context context;
		public Result(int id,Context context){
			this.id = id;
			this.context = context;
		}
		public int getId() {
			return id;
		}
		public Context getContext() {
			return context;
		}
	}
	
	private static Result getIdentifierAndContext(String name,String type) {
		Context context = mSkinContext;
		int id = getIdentifier(name, type, context);
		//皮肤包没找到，找本地
		if (id == 0 && mSkinContext != mLocalContext) {
			context = mLocalContext;
			id = getIdentifier(name, type, context);
		}
		//返回结果中包含id和context
		return new Result(id,context);
	}

}
