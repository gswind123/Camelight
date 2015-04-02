package com.camelight.android.util;

import android.app.Activity;
import android.content.Context;
import android.support.v4.app.FragmentActivity;
import android.util.DisplayMetrics;
import android.view.Display;

public class DeviceUtil {
	static public float getPixelFromDip(Context cxt, float dip){
		float density = cxt.getResources().getDisplayMetrics().density;
		return (dip*density + 0.5f);
	}
	
	/*
	 * @param: the context
	 * @return: Screen size: float[0]=width pixels;float[1]=height pixels
	 * */
	static public float[] getScreenSize(Context cxt) {
		float size[] = new float[2];
		DisplayMetrics metrics = cxt.getResources().getDisplayMetrics();
		size[0] = metrics.widthPixels;
		size[1] = metrics.heightPixels;
		return size;
	}
}
