package com.camelight.android.model;

import android.content.Context;
import android.graphics.Rect;
import android.widget.FrameLayout;

import com.camelight.android.business.NightSceneGuideInteraction;
import com.camelight.android.view.util.CameraView;

public class CalculateDistanceCacheBean extends CacheBean{
	private float drawWidth_ = 0;
	private Object lock = new Object();
	
	public CameraView camera_ = null;
	public Context context_ = null;
	public FrameLayout layout_ = null;
	public NightSceneGuideInteraction uiInteraction_ = null;
	public CameraFrame curFrame_ = null;
	
	public Rect faceRect_ = new Rect();
	
	public void setDrawWidth(float drawWidth){
		synchronized (lock) {
			if(drawWidth == 0) {
				drawWidth_ = 0;
			} else {
				drawWidth_ = drawWidth;
			}
		}
	}
	
	public float getDrawWidth(){
		if(drawWidth_ == 0) {
			return 0;
		}
		float res = 0;
		synchronized(lock){
			res = drawWidth_;
		}
		return res;
	}
	
	public void setFaceRect(Rect faceRect){
		synchronized (lock) {
			if(faceRect == null) {
				faceRect_ = null;
			} else {
				faceRect_ = faceRect;
			}
		}
	}
	
	
	public Rect getFaceRect(){
		if(faceRect_ == null) {
			return null;
		}
		Rect res = new Rect();
		synchronized(lock){
			res = faceRect_;
		}
		return res;
	}
}
