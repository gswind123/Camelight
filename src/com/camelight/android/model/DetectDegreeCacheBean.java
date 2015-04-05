package com.camelight.android.model;


import com.camelight.android.business.FrontLightGuideInteraction;
import com.camelight.android.view.util.CameraView;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class DetectDegreeCacheBean extends CacheBean {
	private float orientation_ = -1.f;
	private Object lock = new Object();
	
	public CameraView camera_ = null;
	public Context context_ = null;
	public FrameLayout layout_ = null;
	public Bitmap bitmap_ = null; // yw_sun debug
	public FrontLightGuideInteraction uiInteraction_ = null;
	
	public void setOrientation(float orientation){
		orientation_ = orientation;
	}
	public float getOrientation(){
		return orientation_;
	}
}
