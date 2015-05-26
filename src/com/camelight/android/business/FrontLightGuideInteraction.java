package com.camelight.android.business;

import java.sql.Date;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.media.Image;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.camelight.android.R;
import com.camelight.android.model.CacheBean;
import com.camelight.android.model.DetectDegreeCacheBean;
import com.camelight.android.util.DeviceUtil;
import com.camelight.android.util.InteractionUtil;
import com.camelight.android.util.OrientationUtil;
import com.camelight.android.view.CameraActivity;
import com.camelight.android.view.util.CameDialog;
import com.camelight.android.view.util.PropertyAnimation;
import com.camelight.android.view.util.PropertyAnimator;

public class FrontLightGuideInteraction extends Interaction{

	public DetectDegreeCacheBean cacheBean_ = new DetectDegreeCacheBean();
	public float lightSrcOrientation_ = -1.f;
	
	private boolean isCanceled_ = false;
	private boolean isPausing_ = false;
	
	private Interactor detectDegreeInteractor_ = null;
	private Thread detectDegreeThread_ = null;
	private Handler msgHandler_ = null;
	private PropertyAnimator animator_ = new PropertyAnimator();
	
	private class FrontLightAnimation extends PropertyAnimation {
		private final int LEFT = 1;
		private final int RIGHT=2;
		
		private boolean isVisible_ = false;
		private boolean showLock_ = false;
		
		private View mainView_ = null;
		private View wrapContainer_ = null;
		private View degreeView_ = null;
		private View rotateArrow_ = null;
		private int curDirection_ = 0;
		private int durationOfFrontLight_ = 0;
		private int durationOfNonfrontLight_ = 0;
		
		private float screenWidth_ = 0.f;
		
		private int curLevel_ = 0;

		private final int FrontLightDurationThreshold = 500;
		
		/*
		 * @param dir:1,left; 2,right
		 * */
		private void setDirection(int dir){
			if(degreeView_ == null || dir == curDirection_) {
				return ;
			}
			int anim_id = R.anim.dynamic_arrow_left;
			if(dir == RIGHT) {
				anim_id = R.anim.dynamic_arrow_right;
			}
			ImageView arrow = getArrowView();
			arrow.setBackgroundResource(anim_id);
			((AnimationDrawable)(arrow.getBackground())).start();
			curDirection_ = dir;
		}
		
		private ImageView getArrowView(){
			return (ImageView)rotateArrow_;
		}
		
		/*
		 * The arrow rotating speed(ars) will be divided into 5 levels.
		 * suppose the screen width is w,and the deviation angle is a:
		 * level 1: a in [0,  35 ]; ars = 0 fps
		 * level 2: a in [35, 50 ]; ars = 6 fps
		 * level 3: a in [50, 80 ]; ars = 12
		 * level 4: a in [80, 120]; ars = 18
		 * level 5: a in [120, + ]; ars = 24
		 * */
		private final float levelBounds_[] = new float[]{
//			0, 35.f, 50.f, 80.f, 120.f
			0, 50.f, 60.f, 80.f, 120.f
		};
		private final float levelSpeed[] = new float[]{
			0.01f, /*L1:*/0.01f, /*L2:*/6.f, /*L3:*/12.f, /*L4:*/18.f,/*L5:*/24.f   	
		};
		private int calcDeltaDegree(float lt_src) {
			int delta = (int)(lt_src - OrientationUtil.getOrientation());
			delta = (delta+360)%360;
			delta = Math.min(delta, 360-delta);
			return delta;
		}
		private int calcLightLevel(float lt_src){
			int delta = calcDeltaDegree(lt_src);
			for(int i=0;i<levelBounds_.length;i++) {
				if(delta < levelBounds_[i]) {
					return i;
				}
			}
			return levelBounds_.length;
		}
		private int calcDstDirection(float lt_src) {
			int delta = (int)(lt_src - OrientationUtil.getOrientation());
			delta = (delta+360)%360;
			if(delta > 180) {
				return LEFT;
			} else {
				return RIGHT;
			}
		}
		/*
		 * @return: the millsec of one frame
		 * */
		private int getLevelSpeed(int level){
			float fps = levelSpeed[level];
			int mspf = (int)(1000/fps);
			mspf = Math.max(1, mspf);
			return mspf;
		}
		
		@Override
		public boolean update(long tweenMillsec) {
			if(isPausing_) {
				return true;
			}
			if(lightSrcOrientation_ >0) {
				/** calculate direction and level to move*/
				int dir = calcDstDirection(lightSrcOrientation_);
				setDirection(dir);
				curLevel_ = calcLightLevel(lightSrcOrientation_);
				
				TextView text = (TextView)mainView_.findViewById(R.id.test_data);
				String str = "";
				if(curDirection_ == LEFT) {
					str = "×ó×ª:";
				} else {
					str = "ÓÒ×ª£º";
				}
				str += calcDeltaDegree(lightSrcOrientation_);
				str += "¶È";
				
				text.setText(str);
				
				/** judge if front light is satisfied*/
				if(curLevel_ <= 1) {
					durationOfFrontLight_ += tweenMillsec;
					durationOfNonfrontLight_ = 0;
				} else {
					durationOfFrontLight_ = 0;
					durationOfNonfrontLight_ += tweenMillsec;
				}
			}
			return true;
		}
		
		@Override
		public boolean start() {
			screenWidth_ = DeviceUtil.getScreenSize(cacheBean_.context_)[0];
			CameraActivity act = (CameraActivity) cacheBean_.context_;
			LayoutInflater inflater = (LayoutInflater)act.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			mainView_ = inflater.inflate(R.layout.front_light_show_degree_layout, null);
			wrapContainer_ = mainView_.findViewById(R.id.wrap_container);
			degreeView_ = mainView_.findViewById(R.id.degree_view);
			rotateArrow_ = mainView_.findViewById(R.id.rotate_arrow);
			LayoutParams lp = rotateArrow_.getLayoutParams();
			lp.width = (int)(0.5*screenWidth_);
			lp.height = lp.width*80/310;
			rotateArrow_.setLayoutParams(lp);
			setDirection(LEFT);
			/** add the view to the outer frame layout*/
			wrapContainer_.setVisibility(View.GONE);
			isVisible_ = false;
			cacheBean_.layout_.addView(mainView_);
			
			return true;
		}
		
		@Override
		public void finish() {
			cacheBean_.layout_.removeView(mainView_);
		}
		
		/*
		 * @return: true if it is confirm to be front light; false otherwise.
		 * */
		public boolean confirmFrontLight(){
			return (durationOfFrontLight_ >= FrontLightDurationThreshold);
		}
		
		/*
		 * @return: true if it is confirm to be not front light; false otherwise.
		 * */
		public boolean confirmNonfrontLight(){
			return (durationOfNonfrontLight_ >= FrontLightDurationThreshold);
		}
		
		public void hideGuide() {
			if(showLock_) {
				return ;
			}
			showLock_ = true;
			Animation anim = new AlphaAnimation(1.f, 0.f);
			anim.setDuration(1000);
			anim.setAnimationListener(new AnimationListener() {

				public void onAnimationStart(Animation animation) {}
				@Override
				public void onAnimationEnd(Animation animation) {
					wrapContainer_.setVisibility(View.GONE);
					showLock_ = false;
					isVisible_ = false;
				}
				@Override
				public void onAnimationRepeat(Animation animation) {}
			});
			wrapContainer_.startAnimation(anim);
		}
		public void showGuide() {
			if(showLock_) {
				return ;
			}
			showLock_ = true;
			Animation anim = new AlphaAnimation(0.f, 1.f);
			anim.setDuration(1000);
			anim.setAnimationListener(new AnimationListener() {

				public void onAnimationStart(Animation animation) {}
				@Override
				public void onAnimationEnd(Animation animation) {
					showLock_ = false;
				}
				@Override
				public void onAnimationRepeat(Animation animation) {}
			});
			wrapContainer_.setVisibility(View.VISIBLE);
			wrapContainer_.startAnimation(anim);
			isVisible_ = true;
		}
		
		public boolean isVisible(){
			return isVisible_;
		}
		
		public boolean isLocked(){
			return showLock_;
		}
	}
	
	private FrontLightAnimation degreeAnimation_ = new FrontLightAnimation();
	private int quitMessage_ = BusinessState.NULL;
	
	@Override
	public boolean onInteractStart(CacheBean param) {
		DetectDegreeCacheBean bean = checkParam(param);
		if(bean == null) {
			return false;
		}
		cacheBean_.uiInteraction_ = this;
		startDetectDegree();

		animator_.addAnimation(this.degreeAnimation_);
		return true;
	}

	@Override
	public InteractState onInteracting(CacheBean param) {
		DetectDegreeCacheBean bean = checkParam(param);
		if(bean == null) {
			return InteractState.CONTINUE;
		}
		//update animations
		animator_.update();
		
		//judge if to hide the guide view
		if(degreeAnimation_.isVisible()) {
			if(degreeAnimation_.confirmFrontLight()) {
				degreeAnimation_.hideGuide();
			}
		} else {
			if(degreeAnimation_.confirmNonfrontLight()){
				degreeAnimation_.showGuide();
			}
		}
		
		//judge if to switch mode
		BusinessMode mode = cacheBean_.dstMode_;
		((CameraActivity)cacheBean_.context_).updateMode(mode);
		if(mode != BusinessMode.FRONTLIGHT && mode != BusinessMode.NULL) {
			if(mode == BusinessMode.BACKLIGHT) {
				quitMessage_ = BusinessState.SWITCH_MODE_BACKLIGHT;
			} else if(mode == BusinessMode.NIGHT) {
				quitMessage_ = BusinessState.SWITCH_MODE_NIGHT;
			}
			return InteractState.STOP;
		} else {
			return InteractState.CONTINUE;	
		}	
	}

	@Override
	public void onInteractFinish(CacheBean param) {
		DetectDegreeCacheBean bean = checkParam(param);
		if(bean == null) {
			return ;
		}
		/** stop working thread*/
		animator_.removeAnimation(this.degreeAnimation_);
		if(detectDegreeInteractor_ != null && detectDegreeInteractor_.isRunning()) {
			detectDegreeInteractor_.stopInteract();
		}
		/** inform the activity*/
		CameraActivity activity = (CameraActivity) cacheBean_.context_;
		Message msg = new Message();
		msg.what = quitMessage_;
		activity.getBusinessHandler().sendMessage(msg);
	}
	
	private DetectDegreeCacheBean checkParam(CacheBean param) {
		if(param != null && param instanceof DetectDegreeCacheBean) {
			DetectDegreeCacheBean bean = (DetectDegreeCacheBean)param;
			if(bean.context_ == null || bean.camera_ == null || bean.layout_ == null) {
				return null;
			}
			cacheBean_ = bean;
			return cacheBean_;
		}
		return null;
	}
	
	private void startDetectDegree(){
		msgHandler_ = new Handler(){
			@Override
			public void handleMessage(Message msg) {
				lightSrcOrientation_ = cacheBean_.getOrientation();
			}
		};

		Thread thread = new Thread(){
			@Override
			public void run(){
				Looper.prepare();
				Handler handler = new Handler(Looper.myLooper());
				detectDegreeInteractor_ = new Interactor(handler);
				detectDegreeInteractor_.setParam(cacheBean_);
				detectDegreeInteractor_.setInteraction(new DetectDegreeInteraction());
				detectDegreeInteractor_.startInteract(100);
				Looper.loop();
			}
		};
		detectDegreeThread_ = thread;
		detectDegreeThread_.start();
	}
	
	/*
	 * Used to pause the degree animation
	 * @param: pause:true if to pause the interaction,false otherwise;
	 * */
	public void pause(boolean pause) {
		isPausing_ = pause;
	}
	
	public void sendMessage(Message msg){
		if(msgHandler_ != null) {
			msgHandler_.sendMessage(msg);
		}
	}
	
}
