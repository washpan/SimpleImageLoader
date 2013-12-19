package com.washpan.imgtool;


import java.lang.ref.SoftReference;

import android.graphics.Bitmap;
/**
 * @author washpan 190848972@qq.com
 * 用于记录Bitmap的数据结构
 * */
public class SafeBitmap {
	/**
	 * Bitmap对象
	 * */
	public SoftReference<Bitmap> bitmap;
	/**
	 * Bitmap的格式
	 * */
	public Bitmap.Config config;
	public int size = 0;
}
