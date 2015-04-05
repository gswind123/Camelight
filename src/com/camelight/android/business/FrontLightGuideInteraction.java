package com.camelight.android.business;

import javax.security.auth.PrivateCredentialPermission;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.camelight.android.R;
import com.camelight.android.model.CacheBean;
import com.camelight.android.model.DetectDegreeCacheBean;
import com.camelight.android.util.DeviceUtil;
import com.camelight.android.util.OrientationUtil;
import com.camelight.android.view.CameraActivity;
import com.camelight.android.view.util.PropertyAnimation;
import com.camelight.android.view.util.PropertyAnimator;

public class FrontLightGuideInteraction extends Interaction{

	public DetectDegreeCacheBean cacheBean_ = new DetectDegreeCacheBean();
	public float lightSrcOrientation_ = -1.f;
	
	private Interactor detectDegreeInteractor_ = null;
	private Thread detectDegreeThread_ = null;
	private Handler msgHandler_ = null;
	private PropertyAnimator animator_ = new PropertyAnimator();
	private PropertyAnimation degreeAnimation_ = new PropertyAnimation() {
		private final int LEFT = 1;
		private final int RIGHT=2;
		private final float SPEED = 0.002f/** pixels per millsec */;
		
		private boolean isVisible_;
		
		private View degreeView_ = null;
		private View faceLeft_ = null;
		private View faceRight_ = null;
		private View sunLeft_ = null;
		private View sunRight_ = null;
		private View arrowLeft_ = null;
		private View arrowRight_ = null;
		private int curDirection_ = 0;
		
		private TextView degreeText_ = null;
		
		private float dstWidthDip_ = 0.f;
		private float minWidth_ = 0.f;
		private float screenWidth_ = 0.f;
		
		/*
		 * @param dir:1,left; 2,right
		 * */
		private void setDirection(int dir){
			if(degreeView_ == null) {
				return ;
			}
			int vis_dir_left = View.GONE;
			int vis_dir_right = View.GONE;
			if(dir == LEFT) {
				vis_dir_left = View.VISIBLE;
			}else if(dir == RIGHT){
				vis_dir_right = View.VISIBLE;
			}
			faceRight_.setVisibility(vis_dir_left);
			sunLeft_.setVisibility(vis_dir_left);
			arrowLeft_.setVisibility(vis_dir_left);
			
			faceLeft_.setVisibility(vis_dir_right);
			sunRight_.setVisibility(vis_dir_right);
			arrowRight_.setVisibility(vis_dir_right);
			curDirection_ = dir;
		}
		
		private View getArrowView(){
			if(curDirection_ == LEFT){
				return arrowLeft_;
			} else {
				return arrowRight_;
			}
		}
		
		/*
		 * The arrow width(aw) will be divided into 5 levels.
		 * suppose the screen width is w,and the deviation angle is a:
		 * level 1: a in [0,  20 ]; aw = 0
		 * level 2: a in [20, 40 ]; aw = 15%*w
		 * level 3: a in [40, 60 ]; aw = 30%*w
		 * level 4: a in [60, 100]; aw = 50%*w
		 * level 5: a in [100, + ]; aw = 65%*w
		 * */
		private final float levelBounds_[] = new float[]{
			0, 20.f, 40.f, 60.f, 100.f
		};
		private final float levelWeight_[] = new float[]{
			0, /*L1:*/0.f, /*L2:*/0.15f, /*L3:*/0.30f, /*L4:*/0.50f,/*L5:*/0.65f   	
		};
		private int calcLightLevel(float lt_src){
			int delta = (int)(lt_src - OrientationUtil.getOrientation());
			delta = (delta+360)%360;
			delta = Math.min(delta, 360-delta);
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
		private float calcDstArrowWidth(int level) {
			if(level<=0 || level > 5) {
				return 0.f;
			}
			return screenWidth_*levelWeight_[level];
		}
		
		@Override
		public boolean update(long tweenMillsec) {
			if(lightSrcOrientation_ >0) {
				if(isVisible_ == false) {
					degreeView_.setVisibility(View.VISIBLE);
					isVisible_ = true;
				}
				setDirection(calcDstDirection(lightSrcOrientation_));
				View arrow = getArrowView();
				LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams)arrow.getLayoutParams();
				int level = calcLightLevel(lightSrcOrientation_);
				lp.width = (int)calcDstArrowWidth(level);
				arrow.setLayoutParams(lp);
				String text = "目标:"+lightSrcOrientation_+"\n"+"当前:"+OrientationUtil.getOrientation();
				String desc = "\n等级:" + level;
				degreeText_.setText(text+desc);
			}
			return true;
		}
		
		@Override
		public boolean start() {
			screenWidth_ = DeviceUtil.getScreenSize(cacheBean_.context_)[0];
			CameraActivity act = (CameraActivity) cacheBean_.context_;
			LayoutInflater inflater = (LayoutInflater)act.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			degreeView_ = inflater.inflate(R.layout.front_light_show_degree_layout, null);
			faceLeft_ = degreeView_.findViewById(R.id.face_left);
			faceRight_ = degreeView_.findViewById(R.id.face_right);
			sunLeft_ = degreeView_.findViewById(R.id.sun_left);
			sunRight_ = degreeView_.findViewById(R.id.sun_right);
			arrowLeft_ = degreeView_.findViewById(R.id.arrow_left);
			arrowRight_ = degreeView_.findViewById(R.id.arrow_right);
			degreeText_ = (TextView)degreeView_.findViewById(R.id.degree_text);
			minWidth_ = DeviceUtil.getPixelFromDip(cacheBean_.context_, 25.f);
			setDirection(LEFT);
			/** add the view to the outer frame layout*/
			degreeView_.setVisibility(View.GONE);
			isVisible_ = false;
			int wrap_content = ViewGroup.LayoutParams.WRAP_CONTENT;
			ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(wrap_content, wrap_content);
			cacheBean_.layout_.addView(degreeView_, lp);
			return true;
		}
		
		@Override
		public void finish() {
			cacheBean_.layout_.removeView(degreeView_);
		}
	};
	
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
	
		animator_.update();
		return InteractState.CONTINUE;
	}

	@Override
	public void onInteractFinish(CacheBean param) {
		DetectDegreeCacheBean bean = checkParam(param);
		if(bean == null) {
			return ;
		}

		animator_.removeAnimation(this.degreeAnimation_);
		if(detectDegreeInteractor_ != null && detectDegreeInteractor_.isRunning()) {
			detectDegreeInteractor_.stopInteract();
		}
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
				//yw_sun debug
				((CameraActivity)(cacheBean_.context_)).updatePreview(cacheBean_.bitmap_);
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
	
	public void sendMessage(Message msg){
		if(msgHandler_ != null) {
			msgHandler_.sendMessage(msg);
		}
	}
	
}
