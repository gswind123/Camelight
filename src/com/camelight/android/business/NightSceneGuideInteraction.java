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

import com.camelight.android.R;
import com.camelight.android.model.CacheBean;
import com.camelight.android.model.CalculateDistanceCacheBean;
import com.camelight.android.model.CameraFrame;
import com.camelight.android.util.ImageProcessor;
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
			if (distance_ != 0 && !Float.isNaN(distance_)) {
				FrameLayout.LayoutParams params = (FrameLayout.LayoutParams)circleView_.getLayoutParams();
				if (params == null) {
					return false;
				}
				facerRect = cacheBean_.getFaceRect();
				circleCenter = new PointF();
				//note that faceRect.width = faceRect.height
				circleCenter.x = (facerRect.left + facerRect.right)/2;
				circleCenter.y = (facerRect.top + facerRect.bottom)/2;
				targetCircleRadius = circleCenter.x - facerRect.left;
				//yw_sun debug
				CameraFrame frame = cacheBean_.camera_.getLatestFrame();
				byte data[] = frame.getJPEGData();
				Bitmap bm = BitmapFactory.decodeByteArray(data, 0, data.length);
				bm = ImageProcessor.rotate(bm, frame.getRotation());
				((CameraActivity)(cacheBean_.context_)).updatePreview(Bitmap.createBitmap(bm,
						facerRect.left, facerRect.top, facerRect.width(), facerRect.height()));
				float A = 50;
				currentCircleRadius = targetCircleRadius + distance_ * A;
				//¶¨Î»Ô²ÐÄ
				int left = (int)(circleCenter.x - currentCircleRadius);
				int top = (int)(circleCenter.y - currentCircleRadius);
				params.width = (int)currentCircleRadius*2;
				params.height = (int)currentCircleRadius*2;
				params.setMargins(left, top, 0, 0);
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
			cacheBean_.layout_.addView(distanceView_);
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
