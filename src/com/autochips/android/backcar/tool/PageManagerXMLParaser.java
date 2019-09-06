package com.autochips.android.backcar.tool;

import java.util.HashMap;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.content.Context;
import android.util.Log;


public class PageManagerXMLParaser extends DefaultHandler{
	
	private Context mContext;
	private PageManager pageManager = null;
	private HashMap<Integer, String> pageMap = null;
	
	public PageManagerXMLParaser(Context context){
		mContext = context;
	}
	
	public PageManager getPageManager(){
		return pageManager;
	}
	
	@Override
	public void startDocument() throws SAXException {
		// TODO Auto-generated method stub
		pageManager = new PageManager(mContext);
		pageMap = new HashMap<Integer, String>();
	}
	
	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		// TODO Auto-generated method stub
		if(qName.equals("item")){
			String pageId = attributes.getValue("pageId");
			String layoutName = attributes.getValue("layoutName");
			pageMap.put(HexToInteger(pageId), layoutName);
		}
	}
	
	@Override
	public void endDocument() throws SAXException {
		// TODO Auto-generated method stub
		pageManager.initPageMap(pageMap);
		Log.d("xmlpage","endDocument pageMap:"+pageMap.toString());
	}
	
	
	private int HexToInteger(String strId){
		if(!("").equals(strId) && strId != null){
			return Integer.parseInt(strId.substring(2), 16);
		}
		return -1;
	}

}
