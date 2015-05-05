package com.camelight.android.model;


import com.camelight.android.business.BusinessMode;
import com.camelight.android.business.FrontLightGuideInteraction;
import com.camelight.android.view.util.CameraView;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.widget.FrameLayout;

public class DetectDegreeCacheBean extends CacheBean {
	private float orientation_ = -1.f;
	
	public Rect faceRect_ = new Rect();
	public Bitmap adjustedFrame_ = null;
	public CameraView camera_ = null;
	public Context context_ = null;
	public FrameLayout layout_ = null;
	public BusinessMode dstMode_ = BusinessMode.NULL;
	public FrontLightGuideInteraction uiInteraction_ = null;
	
	public void setOrientation(float orientation){
		orientation_ = orientation;
	}
	public float getOrientation(){
		return orientation_;
	}
}
