package com.autochips.android.backcar.tool;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class XMLTool {
	
	private static SAXParser getSAXParser() throws ParserConfigurationException, SAXException{
		
		// 1.构建一个工厂SAXParserFactory
        SAXParserFactory parserFactory = SAXParserFactory.newInstance();
        // 2.构建并实例化SAXPraser对象
        return parserFactory.newSAXParser();
	}
	
	public static DefaultHandler parse(InputStream inStream,DefaultHandler handler){
		if(inStream!=null){
			try {
				SAXParser parser = getSAXParser();
				parser.parse(inStream, handler);
				return handler;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}finally{
				if(inStream!=null){
					try {
						inStream.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
		return null;
	}
}
