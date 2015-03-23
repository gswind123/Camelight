package com.camelight.android.business;

import android.content.Context;
import android.graphics.PointF;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.camelight.android.R;
import com.camelight.android.model.CacheBean;
import com.camelight.android.model.DetectDegreeCacheBean;
import com.camelight.android.util.DeviceUtil;
import com.camelight.android.view.CameraActivity;
import com.camelight.android.view.util.PropertyAnimation;
import com.camelight.android.view.util.PropertyAnimator;

public class FrontLightGuideInteraction extends Interaction{

	public DetectDegreeCacheBean cacheBean_ = new DetectDegreeCacheBean();
	public PointF degree_ = null;
	
	private Interactor detectDegreeInteractor_ = null;
	private Thread detectDegreeThread_ = null;
	private Handler msgHandler_ = null;
	private PropertyAnimator animator_ = new PropertyAnimator();
	private PropertyAnimation degreeAnimation_ = new PropertyAnimation() {
		private final int LEFT = 1;
		private final int RIGHT=2;
		private final float SPEED = 0.008f/** pixels per millsec */;
		
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
		 * Here we map 130 degrees to 400dps.
		 * 5	degrees=> 25dps;
		 * 130	degrees=> 400dps;
		 * */
		@Override
		public boolean update(long tweenMillsec) {
			if(degree_ != null) {
				dstWidthDip_ = 25+Math.abs(degree_.x);
				int direction = (degree_.x<0)?LEFT:RIGHT;
				View arrow = getArrowView();
				ViewGroup.LayoutParams param = arrow.getLayoutParams();
				if(param == null) {
					return false;
				}
				float cur_width = (float)param.width;
				float dst_width_pixel = DeviceUtil.getPixelFromDip(cacheBean_.context_, dstWidthDip_);
				if((int)(cur_width) == (int)(dst_width_pixel)) {
					/** It doesn't need to move*/
					return true;
				}
				float speed_factor = 1.f;
				float sign = 1.f;
				if(direction != curDirection_) {
					sign = -1.f;
					speed_factor = dst_width_pixel + cur_width;
				} else {
					speed_factor = Math.abs(dst_width_pixel - cur_width);
				}
				
				/** Here when the dst value is too close to cur condition,move it directly
				 *  to the dst.Ohterwise put a movement on it*/
				if(speed_factor < 12.f) {
					cur_width = dst_width_pixel;
				} else {
					float dist = sign*SPEED*tweenMillsec;
					cur_width += dist;
				}
				
				/** When the width is too small and still needs to move,
				 *  it means to change the direction*/
				if(cur_width < minWidth_) {
					int new_dir = (curDirection_==LEFT)?RIGHT:LEFT;
					setDirection(new_dir);
					arrow = getArrowView();
					param = arrow.getLayoutParams();
					cur_width = minWidth_;
				}
				
				/** finally set a new width to the arrow*/
				param.width = (int)cur_width;
				arrow.setLayoutParams(param);
				
				String text = degree_.x+","+degree_.y;
				degreeText_.setText(text);
			}
			return true;
		}
		
		@Override
		public boolean start() {
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
//		if(distance_ != null) {
//			String text="获取到了角度："+distance_.x+","+distance_.y;
//			Toast toast = Toast.makeText(cacheBean_.context_,text, Toast.LENGTH_SHORT);
//			toast.show();
//		}
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
				degree_ = cacheBean_.getDegree();
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
