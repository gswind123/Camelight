package com.camelight.android.view.util;

import java.util.ArrayList;

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
		curTimeMillsec_ = 0;
	}
	
	public void addAnimation(PropertyAnimation anim){
		animQueue_.add(anim);
		anim.start();
		curTimeMillsec_ = System.currentTimeMillis();
	}
	public void update(){
		if(curTimeMillsec_ == 0) {
			curTimeMillsec_ = System.currentTimeMillis();
			return ;
		}
		long cur_time = System.currentTimeMillis();
		long interval = (int)(cur_time - curTimeMillsec_);
		for(PropertyAnimation anim:animQueue_) {
			boolean res = anim.update(interval);
			if(res == false) {
				removeAnimation(anim);
			}
		}
		curTimeMillsec_ = cur_time;
	}
	
	public void removeAnimation(PropertyAnimation anim){
		if(animQueue_.contains(anim)) {
			animQueue_.remove(anim);
			anim.finish();
		}
	}
}
