package com.camelight.android.model;


import com.camelight.android.business.FrontLightGuideInteraction;
import com.camelight.android.view.util.CameraView;

import android.content.Context;
import android.graphics.PointF;

public class DetectDegreeCacheBean extends CacheBean {
	private PointF degree_ = null;
	private Object lock = new Object();
	
	public CameraView camera_ = null;
	public Context context_ = null;
	public FrontLightGuideInteraction uiInteraction_ = null;
	
	public void setDegree(PointF degree){
		synchronized (lock) {
			if(degree == null) {
				degree_ = null;
			} else {
				degree_ = new PointF();
				degree_.x = degree.x;
				degree_.y = degree.y;	
			}
		}
	}
	public PointF getDegree(){
		if(degree_ == null) {
			return null;
		}
		PointF res = new PointF();
		synchronized(lock){
			res.x = degree_.x;
			res.y = degree_.y;
		}
		return res;
	}
}
