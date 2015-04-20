package com.camelight.android.business;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.camelight.android.R;
import com.camelight.android.business.Interaction.InteractState;
import com.camelight.android.model.CacheBean;
import com.camelight.android.model.CalculateDistanceCacheBean;
import com.camelight.android.model.CameraFrame;
import com.camelight.android.util.DeviceUtil;
import com.camelight.android.util.ImageProcessor;
import com.camelight.android.util.InteractionUtil;
import com.camelight.android.view.CameraActivity;
import com.camelight.android.view.util.CameDialog;
import com.camelight.android.view.util.PropertyAnimation;
import com.camelight.android.view.util.PropertyAnimator;

public class NightSceneGuideInteraction extends Interaction{
	public CalculateDistanceCacheBean cacheBean_ = new CalculateDistanceCacheBean();
	public float drawWidth_ = 0;
	
	private Interactor calculateDistanceInteractor_ = null;
	private Thread calculateDistanceThread_ = null;
	private Handler msgHandler_ = null;
	private PropertyAnimator animator_ = new PropertyAnimator();
	
	private boolean isGuideCanceled_ = false;
	private boolean isPausing_ = false;
	
	private final int autoFocusFrameThreshold_ = 100;
	private int autoFocusFrameCnt_ = 0;
	
	private OnClickListener onCancelClickListener = new OnClickListener() {	
		@Override
		public void onClick(View v) {
			if(InteractionUtil.isDoubleClick()) {
				return ;
			}
			pause(true);
			CameDialog dialog = new CameDialog();
			dialog.setDialogType(CameDialog.EXECUTE_DIALOG);
			dialog.setPositiveText(cacheBean_.context_.getResources().getString(R.string.yes));
			dialog.setNegativeText(cacheBean_.context_.getResources().getString(R.string.no));
			dialog.setDialogContent(cacheBean_.context_.getResources().getString(R.string.ask_to_close_guide));
			dialog.setOnPositiveListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if(cacheBean_.context_ instanceof CameraActivity) {
						isGuideCanceled_ = true;
						((CameraActivity)(cacheBean_.context_)).stopCurrentInteraction();
					}
				}
			});
			dialog.setOnNegativeClick(new OnClickListener() {
				@Override
				public void onClick(View v) {
					pause(false);
				}
			});
			dialog.show((FragmentActivity)(cacheBean_.context_));
		}
	};
	
	private OnClickListener onTakePhotoListener_ = new OnClickListener() {	
		@Override
		public void onClick(View v) {
			if(InteractionUtil.isDoubleClick()) {
				return ;
			}
			cacheBean_.camera_.setFlashLight(true);
			cacheBean_.camera_.takePicture();
			Handler handler = new Handler();
			handler.postDelayed(new Runnable() {
				@Override
				public void run() {
					cacheBean_.camera_.setFlashLight(false);
				}
			}, 1000);
		}
	};
	
	private class DistanceAnimation extends PropertyAnimation {
		
		private View distanceView_ = null;
		private View standardCircle_ = null;
		private View approachingCircle_ = null;
		private TextView guideText_ = null;
		
		private final int ChangeDuration = 1000;
		private int curDuration_ = 0;
		
		private final int DistanceFitDurationThreshold = 2000;
		private int curFitDuration_ = 0;
		
		private int startRadius_ = 0;
		private int dstRadius_ = 0;
		private int startStdRadius_ = 0;
		private int dstStdRadius_ = 0;
		
		//yw_sun debug
		private TextView distanceText_ = null;
		//=============

		/*
		 * Let the screen width to be w.Set the approaching circle's size to be:
		 *    max: 0.8*n <= MAX_DISTANCE
		 *    min: 0.2*n <= MIN_DISTANCE
		 *    fit: 0.4*n (size of standard circle) <= STANDARD_DISTANCE
		 * */
//		private int getRadiusByDistance(float distance) {
//			int radius = 0;
//			distance = Math.min(distance, MaxDistance);
//			distance = Math.max(distance, MinDistance);
//			if(distance > StandardDistance) {
//				radius = (int)( 0.4*screenWidth_ * (1+(distance - StandardDistance)/(MaxDistance - StandardDistance)) );
//			} else {
//				radius = (int)( 0.2*screenWidth_ * (1+(StandardDistance - distance)/(StandardDistance - MinDistance)) );
//			}
//			return radius;
//		}
		private boolean isRadiusFit(int cur,int std) {
			float indist = std * 0.15f;
			float dis = Math.abs(std - cur);
			return (dis < indist);
		}
		
		private void setDstCircle(int radius, Rect face) {
			ViewGroup.LayoutParams lp = approachingCircle_.getLayoutParams();
			if(lp == null) {
				return ;
			}
			startRadius_ = lp.width;
			dstRadius_ = radius;
			
			lp = standardCircle_.getLayoutParams();
			if(lp == null) {
				return ;
			}
			startStdRadius_ = lp.width;
			dstStdRadius_ = face.width();
			
			curDuration_ = 0;
		}
		
		private void updateCircle(long tween) {
			float rate = Math.min(1.f, curDuration_*1.f/ChangeDuration);
			/** update standard circle*/
			ViewGroup.LayoutParams  lp = standardCircle_.getLayoutParams(); 
			if(lp == null) {
				return ;
			}
			int cur_std_radius = (int)(startStdRadius_ + (dstStdRadius_-startStdRadius_)*rate);
			lp.width = cur_std_radius;
			lp.height = cur_std_radius;
			standardCircle_.setLayoutParams(lp);
			/** update approaching circle*/
			lp = approachingCircle_.getLayoutParams();
			if(lp == null) {
				return ;
			}
			int cur_radius = (int)(startRadius_ + (dstRadius_-startRadius_)*rate);
			if( isRadiusFit(dstRadius_, dstStdRadius_) &&
				isRadiusFit(cur_radius, cur_std_radius)) {
				/** 
				 * if the approaching circle is close enough to std circle,
				 * the light condition is good enough.
				 */ 
				curFitDuration_ += tween;
				cur_radius = cur_std_radius;
				standardCircle_.setBackgroundResource(R.drawable.green_circle);
			} else {
				curFitDuration_ = 0;
				standardCircle_.setBackgroundResource(R.drawable.red_circle);
			}
			lp.width = cur_radius;
			lp.height = cur_radius;
			approachingCircle_.setLayoutParams(lp);
			
			curDuration_ += tween;
		}
		
		@Override
		public boolean update(long tweenMillsec) {
			if(isPausing_) {
				return true;
			}
			/** none 0 means face-detected*/
			if (drawWidth_ >= 0 ) {
				if(Float.isNaN(drawWidth_)) {
					drawWidth_ = 100;
				}
				FrameLayout.LayoutParams params = (FrameLayout.LayoutParams)approachingCircle_.getLayoutParams();
				if (params == null) {
					return false;
				}
				int radius = (int) drawWidth_;
				if(Math.abs(radius - dstRadius_) > 5) {
					setDstCircle(radius, cacheBean_.faceRect_);
				}
				updateCircle(tweenMillsec);
				guideText_.setText(cacheBean_.context_.getResources().getString(R.string.desc_night_scene_guide));
			} else {
				guideText_.setText(cacheBean_.context_.getResources().getString(R.string.desc_search_face));	
			}
			return true;
		}
		
		@Override
		public boolean start() {
			CameraActivity act = (CameraActivity) cacheBean_.context_;
			LayoutInflater inflater = (LayoutInflater)act.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			distanceView_ = inflater.inflate(R.layout.night_scene_show_distance_layout, null);
			standardCircle_ = distanceView_.findViewById(R.id.standard_circle);
			approachingCircle_ = distanceView_.findViewById(R.id.approaching_circle);
			guideText_ = (TextView)distanceView_.findViewById(R.id.guide_text);
			View btn_cancel = distanceView_.findViewById(R.id.btn_cancel);
			btn_cancel.setOnClickListener(onCancelClickListener);
			View btn_take_photo = distanceView_.findViewById(R.id.btn_take_photo);
			btn_take_photo.setOnClickListener(onTakePhotoListener_);
			/** add the view to the outer frame layout*/
			cacheBean_.layout_.addView(distanceView_);
			
			distanceText_ = (TextView)distanceView_.findViewById(R.id.distance_text);
			return true;
		}
		
		@Override
		public void finish() {
			cacheBean_.layout_.removeView(distanceView_);
		}
		
		public boolean isDistanceFit() {
			return curFitDuration_ >= DistanceFitDurationThreshold;
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
		if(cacheBean_.curFrame_ != null && cacheBean_.faceRect_ != null) {
			autoFocusFrameCnt_++;
			if(autoFocusFrameCnt_ >= autoFocusFrameThreshold_) {
				autoFocusFrameCnt_ = 0;
				Rect rect = cacheBean_.faceRect_;
				int width = cacheBean_.curFrame_.getHeight();
				int height = cacheBean_.curFrame_.getWidth();
				int rect_width = rect.width();
				if(cacheBean_.curFrame_.isMirror()) {
					rect.left = width - rect.left - rect.width();
					rect.right = rect.left+rect_width;
				}
				cacheBean_.camera_.setFocusAt(rect, width, height);
			}
		}

//		if(distanceAnimation_.isDistanceFit()) {
//			isGuideCanceled_ = false;
//			return InteractState.STOP;
//		} else {
//			return InteractState.CONTINUE;		
//		}
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
		/** inform the activity that the guide is finish*/
		CameraActivity activity = (CameraActivity)cacheBean_.context_;
		Message msg = new Message();
		if(isGuideCanceled_) {
			msg.what = BusinessState.NIGHT_SCENE_GUIDE_CANCEL;
		} else {
			msg.what = BusinessState.NIGHT_SCENE_GUIDE_FINISH;
		}
		activity.getBusinessHandler().sendMessage(msg);
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
	
	public void pause(boolean pause) {
		isPausing_ = pause;
	}
}
