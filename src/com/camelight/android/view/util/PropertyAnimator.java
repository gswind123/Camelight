package com.camelight.android.view.util;

import java.util.ArrayList;

import android.view.animation.Interpolator;

import com.camelight.android.business.FrontLightGuideInteraction;
import com.camelight.android.model.CacheBean;

/*
 * This animator is to enable some continuous animations, not some
 * tween animations,but some animations that needs to keep updating and
 * need to know the tween of two frames.The animation will work when calling
 * addAnimation and continuously call update; 
 * The animation will run on the thread that calls update
 * */
public class PropertyAnimator {
	private long curTimeMillsec_;
	private ArrayList<PropertyAnimation> animQueue_ = new ArrayList<PropertyAnimation>();
	
	public PropertyAnimator(){
		curTimeMillsec_ = System.currentTimeMillis();
	}
	
	public void addAnimation(PropertyAnimation anim){
		animQueue_.add(anim);
		anim.start();
		curTimeMillsec_ = System.currentTimeMillis();
	}
	public void update(){
		long cur_time = System.currentTimeMillis();
		long interval = (int)(cur_time - curTimeMillsec_);
		for(PropertyAnimation anim:animQueue_) {
			boolean res = anim.update(interval);
			if(res == false) {
				removeAnimation(anim);
			}
		}
	}
	
	public void removeAnimation(PropertyAnimation anim){
		if(animQueue_.contains(anim)) {
			animQueue_.remove(anim);
			anim.finish();
		}
	}
}
