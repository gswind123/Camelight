package com.camelight.android.business;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.camelight.android.R;
import com.camelight.android.model.CacheBean;
import com.camelight.android.model.CalculateDistanceCacheBean;
import com.camelight.android.model.CameraFrame;
import com.camelight.android.util.DeviceUtil;
import com.camelight.android.util.ImageProcessor;
import com.camelight.android.view.CameraActivity;
import com.camelight.android.view.util.PropertyAnimation;
import com.camelight.android.view.util.PropertyAnimator;

public class NightSceneGuideInteraction extends Interaction{
	public CalculateDistanceCacheBean cacheBean_ = new CalculateDistanceCacheBean();
	public float drawWidth_ = 0;
	
	private Interactor calculateDistanceInteractor_ = null;
	private Thread calculateDistanceThread_ = null;
	private Handler msgHandler_ = null;
	private PropertyAnimator animator_ = new PropertyAnimator();
	
	private class DistanceAnimation extends PropertyAnimation {
		private final float MaxDistance = 2.3f;
		private final float MinDistance = 1.8f;
		private final float StandardDistance = 2.0f;
		
		private View distanceView_ = null;
		private View standardCircle_ = null;
		private View approachingCircle_ = null;
		private int screenWidth_ = 0;
		
		private final int ChangeDuration = 1000;
		
		private int curDuration_ = 0;
		private int startRadius_ = 0;
		private int dstRadius_ = 0;
		
		//yw_sun debug
		private TextView distanceText_ = null;
		//=============

		/*
		 * Let the screen width to be w.Set the approaching circle's size to be:
		 *    max: 0.8*n <= MAX_DISTANCE
		 *    min: 0.2*n <= MIN_DISTANCE
		 *    fit: 0.4*n (size of standard circle) <= STANDARD_DISTANCE
		 * */
		private int getRadiusByDistance(float distance) {
			int radius = 0;
			distance = Math.min(distance, MaxDistance);
			distance = Math.max(distance, MinDistance);
			if(distance > StandardDistance) {
				radius = (int)( 0.4*screenWidth_ * (1+(distance - StandardDistance)/(MaxDistance - StandardDistance)) );
			} else {
				radius = (int)( 0.2*screenWidth_ * (1+(StandardDistance - distance)/(StandardDistance - MinDistance)) );
			}
			return radius;
		}
		
		private void setDstCircle(int radius, Rect face) {
			ViewGroup.LayoutParams lp = approachingCircle_.getLayoutParams();
			if(lp == null) {
				return ;
			}
			startRadius_ = lp.width;
			dstRadius_ = radius;
			curDuration_ = 0;
		}
		
		private void updateCircle(long tween) {
			ViewGroup.LayoutParams lp = approachingCircle_.getLayoutParams();
			if(lp == null) {
				return ;
			}
			int cur_radius = (int)(startRadius_ + (dstRadius_-startRadius_)*Math.min(1.f, curDuration_/ChangeDuration));
			lp.width = cur_radius;
			approachingCircle_.setLayoutParams(lp);
			curDuration_ += tween;
		}
		
		@Override
		public boolean update(long tweenMillsec) {
			if (drawWidth_ != 0 ) {
				if(Float.isNaN(drawWidth_)) {
					drawWidth_ = MaxDistance;
				}
				FrameLayout.LayoutParams params = (FrameLayout.LayoutParams)approachingCircle_.getLayoutParams();
				if (params == null) {
					return false;
				}
				int radius = getRadiusByDistance(drawWidth_);
				if(Math.abs(radius - dstRadius_) > 5) {
					setDstCircle(radius, cacheBean_.faceRect_);
				}
				updateCircle(tweenMillsec);
				distanceText_.setText("distance:"+drawWidth_+"\nradius:"+radius);
			}
			return true;
		}
		
		@Override
		public boolean start() {
			screenWidth_ = (int)(DeviceUtil.getScreenSize(cacheBean_.context_)[0]);
			CameraActivity act = (CameraActivity) cacheBean_.context_;
			LayoutInflater inflater = (LayoutInflater)act.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			distanceView_ = inflater.inflate(R.layout.night_scene_show_distance_layout, null);
			standardCircle_ = distanceView_.findViewById(R.id.standard_circle);
			approachingCircle_ = distanceView_.findViewById(R.id.approaching_circle);
			/** adjust size of standard circle*/
			ViewGroup.LayoutParams lp = standardCircle_.getLayoutParams();
			if(lp != null) {
				lp.width = (int)(0.4 * screenWidth_);
			}
			/** add the view to the outer frame layout*/
			cacheBean_.layout_.addView(distanceView_);
			
			distanceText_ = (TextView)distanceView_.findViewById(R.id.distance_text);
			return true;
		}
		
		@Override
		public void finish() {
			cacheBean_.layout_.removeView(distanceView_);
		}
	}
	
	private DistanceAnimation distanceAnimation_ = new DistanceAnimation();
	
	@Override
	public boolean onInteractStart(CacheBean param) {
		CalculateDistanceCacheBean bean = checkParam(param);
		if(bean == null) {
			return false;
		}
		cacheBean_.uiInteraction_ = this;
		startCalculateDistance();
		animator_.addAnimation(this.distanceAnimation_);
		return true;
	}

	@Override
	public InteractState onInteracting(CacheBean param) {
		CalculateDistanceCacheBean bean = checkParam(param);
		if(bean == null) {
			return InteractState.CONTINUE;
		}
		animator_.update();
		return InteractState.CONTINUE;
	}

	@Override
	public void onInteractFinish(CacheBean param) {
		CalculateDistanceCacheBean bean = checkParam(param);
		if(bean == null) {
			return ;
		}
		animator_.removeAnimation(this.distanceAnimation_);
		if(calculateDistanceInteractor_ != null && calculateDistanceInteractor_.isRunning()) {
			calculateDistanceInteractor_.stopInteract();
		}
	}
	
	private CalculateDistanceCacheBean checkParam(CacheBean param) {
		if(param != null && param instanceof CalculateDistanceCacheBean) {
			CalculateDistanceCacheBean bean = (CalculateDistanceCacheBean)param;
			if(bean.context_ == null || bean.camera_ == null || bean.layout_ == null) {
				return null;
			}
			cacheBean_ = bean;
			return cacheBean_;
		}
		return null;
	}
	
	private void startCalculateDistance(){
		msgHandler_ = new Handler(){
			@Override
			public void handleMessage(Message msg) {
				drawWidth_ = cacheBean_.getDrawWidth();
			}
		};

		Thread thread = new Thread(){
			@Override
			public void run(){
				Looper.prepare();
				Handler handler = new Handler(Looper.myLooper());
				calculateDistanceInteractor_ = new Interactor(handler);
				calculateDistanceInteractor_.setParam(cacheBean_);
				calculateDistanceInteractor_.setInteraction(new CalculateDistanceInteraction());
				calculateDistanceInteractor_.startInteract(100);
				Looper.loop();
			}
		};
		calculateDistanceThread_ = thread;
		calculateDistanceThread_.start();
	}
	
	public void sendMessage(Message msg){
		if(msgHandler_ != null) {
			msgHandler_.sendMessage(msg);
		}
	}
	
}
