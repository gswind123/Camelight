package com.camelight.android.util;

import android.content.Context;

public class DeviceUtil {
	static public float getPixelFromDip(Context cxt, float dip){
		float density = cxt.getResources().getDisplayMetrics().density;
		return (dip*density + 0.5f);
	}
}
