package com.camelight.android.business;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.TextView;
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
	
	private OnClickListener onCloseClickListener_ = new OnClickListener(){
		@Override
		public void onClick(View v) {
			if(InteractionUtil.isDoubleClick()) {
				return ;
			}
			CameDialog dialog = new CameDialog();
			dialog.setDialogType(CameDialog.EXECUTE_DIALOG);
			dialog.setPositiveText(cacheBean_.context_.getResources().getString(R.string.yes));
			dialog.setNegativeText(cacheBean_.context_.getResources().getString(R.string.no));
			dialog.setDialogContent(cacheBean_.context_.getResources().getString(R.string.ask_to_close_guide));
			dialog.setOnPositiveListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if(cacheBean_.context_ instanceof CameraActivity) {
						((CameraActivity)(cacheBean_.context_)).stopCurrentInteraction();
					}
				}
			});
			dialog.show((FragmentActivity)(cacheBean_.context_));
		}
	};
	
	private Interactor detectDegreeInteractor_ = null;
	private Thread detectDegreeThread_ = null;
	private Handler msgHandler_ = null;
	private PropertyAnimator animator_ = new PropertyAnimator();
	private PropertyAnimation degreeAnimation_ = new PropertyAnimation() {
		private final int LEFT = 1;
		private final int RIGHT=2;
		
		private boolean isVisible_;
		
		private View mainView_ = null;
		private View degreeView_ = null;
		private View faceLeft_ = null;
		private View faceRight_ = null;
		private View sunLeft_ = null;
		private View sunRight_ = null;
		private View arrowLeft_ = null;
		private View arrowRight_ = null;
		private int curDirection_ = 0;
		
		private TextView degreeText_ = null;
		private TextView guideText_ = null;
		
		private float screenWidth_ = 0.f;
		
		private int curLevel_ = 0;
		private int dstLevel_ = 0;
		private int startWidth_ = 0;
		private int dstWidth_ = 0;
		private int curDuration_ = 0;
		private final int ChangeDuration = 500;
		
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
			0, 25.f, 50.f, 80.f, 120.f
		};
		private final float levelWeight_[] = new float[]{
			0, /*L1:*/0.f, /*L2:*/0.2f, /*L3:*/0.35f, /*L4:*/0.50f,/*L5:*/0.65f   	
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
		private void setDstLevel(int dir, int level) {
			/** set a dst level and make the animation to move to it*/
			View arrow = getArrowView();
			startWidth_ = arrow.getLayoutParams().width;
			dstWidth_ = (int)calcDstArrowWidth(level);
			if(dir == LEFT) {
				dstWidth_ *= -1;
			}
			if(curDirection_ == LEFT) {
				startWidth_ *= -1;
			}
			dstLevel_ = level;
			curDuration_ = 0;
		}
		private void updateWidthChanges(long tween) {
			if(curLevel_ == dstLevel_) {
				return ;
			}
			View arrow = getArrowView();
			LayoutParams lp = arrow.getLayoutParams();
			int cur_width = lp.width;
			if(curDirection_ == LEFT) {
				cur_width *= -1;
			}
			
			curDuration_ += tween;
			int abs_dis = Math.abs(cur_width - dstWidth_);
			if(abs_dis < 5){
				curLevel_ = dstLevel_;
				cur_width = dstWidth_;
			} else {
				cur_width = (int)(startWidth_ + (dstWidth_ - startWidth_)*Math.min(1.f, (curDuration_*1.f/ChangeDuration)));
			}
			int dir = LEFT;
			if(cur_width > 0){
				dir = RIGHT;
			}
			setDirection(dir);
			arrow = getArrowView();
			lp = arrow.getLayoutParams();
			lp.width = Math.abs(cur_width);
			arrow.setLayoutParams(lp);
		}
		
		@Override
		public boolean update(long tweenMillsec) {
			if(lightSrcOrientation_ >0) {
				int dir = calcDstDirection(lightSrcOrientation_);
				int level = calcLightLevel(lightSrcOrientation_);
				if(level != curLevel_ || dir != curDirection_) {
					setDstLevel(dir, level);
				}
				updateWidthChanges(tweenMillsec);
				String text = "目标:"+lightSrcOrientation_+"\n"+"当前:"+OrientationUtil.getOrientation();
				String desc = "\n等级:" + level;
				desc += "\nduration:" + curDuration_;
				degreeText_.setText(text+desc);
				
				if(isVisible_ == false) {
					degreeView_.setVisibility(View.VISIBLE);
					isVisible_ = true;
					guideText_.setText(cacheBean_.context_.getResources().getString(R.string.desc_front_light_guide));
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
			degreeView_ = mainView_.findViewById(R.id.degree_view);
			faceLeft_ = mainView_.findViewById(R.id.face_left);
			faceRight_ = mainView_.findViewById(R.id.face_right);
			sunLeft_ = mainView_.findViewById(R.id.sun_left);
			sunRight_ = mainView_.findViewById(R.id.sun_right);
			arrowLeft_ = mainView_.findViewById(R.id.arrow_left);
			arrowRight_ = mainView_.findViewById(R.id.arrow_right);
			degreeText_ = (TextView)mainView_.findViewById(R.id.degree_text);
			guideText_ = (TextView)mainView_.findViewById(R.id.guide_text);
			setDirection(LEFT);
			guideText_.setText(cacheBean_.context_.getResources().getString(R.string.desc_search_face));
			/** add the view to the outer frame layout*/
			degreeView_.setVisibility(View.GONE);
			isVisible_ = false;
			cacheBean_.layout_.addView(mainView_);
			/** set close btn*/
			View close_btn = mainView_.findViewById(R.id.close_btn);
			close_btn.setOnClickListener(onCloseClickListener_);
			return true;
		}
		
		@Override
		public void finish() {
			cacheBean_.layout_.removeView(mainView_);
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
