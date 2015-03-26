package com.camelight.android.business;

import android.content.Context;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.camelight.android.R;
import com.camelight.android.model.CacheBean;
import com.camelight.android.model.CalculateDistanceCacheBean;
import com.camelight.android.view.CameraActivity;
import com.camelight.android.view.util.PropertyAnimation;
import com.camelight.android.view.util.PropertyAnimator;

public class NightSceneGuideInteraction extends Interaction{
	public CalculateDistanceCacheBean cacheBean_ = new CalculateDistanceCacheBean();
	public float distance_ = 0;
	
	private Interactor calculateDistanceInteractor_ = null;
	private Thread calculateDistanceThread_ = null;
	private Handler msgHandler_ = null;
	private PropertyAnimator animator_ = new PropertyAnimator();
	private PropertyAnimation distanceAnimation_ = new PropertyAnimation() {
		private final int LEFT = 1;
		private final int RIGHT=2;
		private final float SPEED = 0.008f/** pixels per millsec */;

		private View distanceView_ = null;
		private View circleView_ = null;
		private Rect facerRect = null;
		private PointF circleCenter = null;
		private float targetCircleRadius = 0;
		private float currentCircleRadius = 0;
		/*
		 * Here we map 130 degrees to 400dps.
		 * 5	degrees=> 25dps;
		 * 130	degrees=> 400dps;
		 * */
		@Override
		public boolean update(long tweenMillsec) {
			if (distance_ != 0) {
				ViewGroup.LayoutParams params = circleView_.getLayoutParams();
				if (params == null) {
					return false;
				}
				facerRect = cacheBean_.getFaceRect();
				float temp = (facerRect.left+facerRect.right)/2;
				//note that faceRect.width = faceRect.height
				circleCenter = new PointF(temp, temp);
				targetCircleRadius = circleCenter.x - facerRect.left;
				
				//currentRadius = targetRadius + distance * A, where distance ->[0,2.5]
				//可以通過調節A來改變currentRadius的變化快慢。我也不是道怎麼突然變成繁體字了..
				//這裡應該需要轉換為像素操作。
				float A = 100;
				currentCircleRadius = targetCircleRadius + distance_ * A;
				params.width = (int)currentCircleRadius;
				params.height = (int)currentCircleRadius;
				circleView_.setLayoutParams(params);
			}
			return true;
		}
		
		@Override
		public boolean start() {
			CameraActivity act = (CameraActivity) cacheBean_.context_;
			LayoutInflater inflater = (LayoutInflater)act.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			distanceView_ = inflater.inflate(R.layout.night_scene_show_distance_layout, null);
			circleView_ = distanceView_.findViewById(R.id.circle_view);

			/** add the view to the outer frame layout*/
			int wrap_content = ViewGroup.LayoutParams.WRAP_CONTENT;
			ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(wrap_content, wrap_content);
			cacheBean_.layout_.addView(distanceView_, lp);
			return true;
		}
		
		@Override
		public void finish() {
			cacheBean_.layout_.removeView(distanceView_);
		}
	};
	
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
				distance_ = cacheBean_.getDistance();
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
