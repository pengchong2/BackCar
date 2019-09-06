package com.autochips.android.backcar.tool;

import java.util.HashMap;

import android.content.Context;

public class PageManager {

	private Context mContext;
	private HashMap<Integer, String> PageMap = new HashMap<Integer, String>();
	
	public PageManager(Context context){
		mContext = context;
	}
	
	public void initPageMap(HashMap<Integer, String> map){
		PageMap = map;
	}

	public int getLayoutId(int pageId) {
		String layoutName = null;
		if(PageMap.containsKey(pageId)){
			layoutName = PageMap.get(pageId);
			return mContext.getResources().getIdentifier(layoutName, "layout", 
					mContext.getPackageName());
		}
		return -1; 
	}
	
	
	   public String getLayoutName(int pageId) {
	        String layoutName = null;
	        if(PageMap.containsKey(pageId)){
	            layoutName = PageMap.get(pageId);
	            return layoutName;
	        }
	        return null; 
	    }
	
	
	public boolean isPageValuable(int pageId){
		return PageMap.containsKey(pageId);
	}

}
