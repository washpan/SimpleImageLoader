package com.washpan.imgtool;


import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;


public class NameUtils {
	public static  String generateKey(String url) {
		String key = "";
		try {
			key = Sha1Util.SHA1(url);
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return key;
	}
}
