package com.camelight.android.business;

import org.opencv.core.Mat;

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
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
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
	
	private boolean isPausing_ = false;
	
	private final int autoFocusFrameThreshold_ = 60;
	private int autoFocusFrameCnt_ = 0;
	
	private int quitMessage_ = BusinessState.NULL;
	
	private Point focusCenter_ = new Point(0,0);
	private int cost_ = 0;
		
	private class DistanceAnimation extends PropertyAnimation {
		
		private View distanceView_ = null;
		private View standardCircle_ = null;
		private View approachingCircle_ = null;
		
		private final float ChangeDuration = 1000.f;
		private int curDuration_ = 0;
		
		private final int DistanceFitDurationThreshold = 500;
		private int curFitDuration_ = 0;
		private int curNonFitDuration_ = 0;
		
		private int startRadius_ = 0;
		private int dstRadius_ = 0;
		private int startStdRadius_ = 0;
		private int dstStdRadius_ = 0;
		private int tracker_ = 0;
		
		private boolean showLock_ = false;
		private boolean isVisible_ = false;
		private boolean isAnimating = false;
		
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
			float indist = std * 0.2f;
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
			isAnimating = true;
		}
		
		private void updateCircle(long tween) {
			if(isAnimating == false) {
				return ;
			}
			float rate = Math.min(1.f, curDuration_*1.f/ChangeDuration);
			if(rate >= 1.f) {
				isAnimating = false;
				rate = 1.f;
			}
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
			tracker_ = cur_radius;
			if( isRadiusFit(dstRadius_, dstStdRadius_) &&
				isRadiusFit(cur_radius, cur_std_radius)) {
				/** 
				 * if the approaching circle is close enough to std circle,
				 * the light condition is good enough.
				 */
				curFitDuration_ += tween;
				curNonFitDuration_ = 0;
				cur_radius = cur_std_radius;
				standardCircle_.setBackgroundResource(R.drawable.green_circle);
			} else {
				curFitDuration_ = 0;
				curNonFitDuration_ += tween;
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
			String text = "计算半径:"+String.valueOf(drawWidth_)+"\n";
			text += "目标半径:"+dstRadius_+"\n";
			text += "是否可见:"+isVisible_ + "\n";
			text += "算法开销:"+cost_+"\n";
			text += "是否动画:"+isAnimating;
			distanceText_.setText(text);
			/** none 0 means face-detected*/
			if (drawWidth_ >= 0 ) {
				if(Float.isNaN(drawWidth_)) {
					return true;
				}
				FrameLayout.LayoutParams params = (FrameLayout.LayoutParams)approachingCircle_.getLayoutParams();
				if (params == null) {
					return false;
				}
				int radius = (int) drawWidth_;
				if(Math.abs(radius - dstRadius_) > 3) {
					setDstCircle(radius, cacheBean_.faceRect_);
				}
				updateCircle(tweenMillsec);
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
			/** add the view to the outer frame layout*/
			distanceView_.setVisibility(View.GONE);
			isVisible_ = false;
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
		public boolean isDistanceNonfit(){
			return curNonFitDuration_ >= DistanceFitDurationThreshold;
		}
		
		public void hideGuide() {
			if(showLock_) {
				return ;
			}
			showLock_ = true;
			Animation anim = new AlphaAnimation(1.f, 0.f);
			anim.setDuration(100);
			anim.setAnimationListener(new AnimationListener() {

				public void onAnimationStart(Animation animation) {}
				@Override
				public void onAnimationEnd(Animation animation) {
					distanceView_.setVisibility(View.GONE);
					showLock_ = false;
					isVisible_ = false;
				}
				@Override
				public void onAnimationRepeat(Animation animation) {}
			});
			distanceView_.startAnimation(anim);
		}
		public void showGuide() {
			if(showLock_) {
				return ;
			}
			showLock_ = true;
			Animation anim = new AlphaAnimation(0.f, 1.f);
			anim.setDuration(100);
			anim.setAnimationListener(new AnimationListener() {

				public void onAnimationStart(Animation animation) {}
				@Override
				public void onAnimationEnd(Animation animation) {
					showLock_ = false;
				}
				@Override
				public void onAnimationRepeat(Animation animation) {}
			});
			distanceView_.setVisibility(View.VISIBLE);
			distanceView_.startAnimation(anim);
			isVisible_ = true;
		}
		
		public boolean isVisible(){
			return isVisible_;
		}
		
		public boolean isLocked(){
			return showLock_;
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
				Point cur_center = new Point(rect.centerX(), rect.centerY());
				int width = cacheBean_.curFrame_.getHeight();
				int height = cacheBean_.curFrame_.getWidth();
				int rect_width = rect.width();
				if(cacheBean_.curFrame_.isMirror()) {
					rect.left = width - rect.left - rect.width();
					rect.right = rect.left+rect_width;
				}
				int distance = (int)Math.sqrt((cur_center.x - focusCenter_.x)*(cur_center.x - focusCenter_.x) + 
										 (cur_center.y - focusCenter_.y)*(cur_center.y - focusCenter_.y));
				if(distance > rect_width) {
					cacheBean_.camera_.setMeteringArea(rect, width, height);
					focusCenter_ = cur_center;
				}
				
			}
		}
		
		/*judge if to hide guide*/
		if(distanceAnimation_.isVisible() && distanceAnimation_.isDistanceFit()) {
			distanceAnimation_.hideGuide();
		}
		else if(!distanceAnimation_.isVisible() && distanceAnimation_.isDistanceNonfit()){
			distanceAnimation_.showGuide();
		}
		
		/*judge if to switch mode*/
		BusinessMode mode = cacheBean_.mode_;
		((CameraActivity)cacheBean_.context_).updateMode(mode);
		if(mode != BusinessMode.NULL && mode!=BusinessMode.NIGHT) {
			if(mode == BusinessMode.FRONTLIGHT) {
				quitMessage_ = BusinessState.SWITCH_MODE_FRONTLIGHT;
			} else if(mode == BusinessMode.BACKLIGHT) {
				quitMessage_ = BusinessState.SWITCH_MODE_BACKLIGHT;
			}
			return InteractState.STOP;
		} else {
			return InteractState.CONTINUE;	
		}	
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
		msg.what = quitMessage_;
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
				if(Float.isNaN(drawWidth_) == false) {
					drawWidth_ += 6;
				}
				cost_ = msg.what;
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
				calculateDistanceInteractor_.startInteract(30);
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
