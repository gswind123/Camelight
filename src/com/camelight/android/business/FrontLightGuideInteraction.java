package com.camelight.android.business;

import javax.security.auth.PrivateCredentialPermission;

import android.app.Activity;
import android.content.Context;
import android.graphics.PointF;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.camelight.android.R;
import com.camelight.android.model.CacheBean;
import com.camelight.android.model.DetectDegreeCacheBean;
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
		
		private View degreeView_ = null;
		private View faceLeft_ = null;
		private View faceRight_ = null;
		private View sunLeft_ = null;
		private View sunRight_ = null;
		private View arrowLeft_ = null;
		private View arrowRight_ = null;
		private int curDirection_ = 0;
		
		/*
		 * @param dir:1,left; 2,right
		 * */
		private void setDirection(int dir){
			if(degreeView_ == null) {
				return ;
			}
			int vis_dir_left = View.GONE;
			int vis_dir_right = View.GONE;
			if(dir == 1) {
				vis_dir_left = View.VISIBLE;
			}else if(dir == 2){
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
		
		@Override
		public boolean update(long tweenMillsec) {
			if(degree_ != null) {
			}
			return true;
		}
		
		@Override
		public boolean start() {
			Activity act = (Activity) cacheBean_.context_;
			LayoutInflater inflater = (LayoutInflater)act.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			degreeView_ = inflater.inflate(R.layout.front_light_show_degree_layout, (ViewGroup)act.findViewById(android.R.id.content));
			faceLeft_ = degreeView_.findViewById(R.id.face_left);
			faceRight_ = degreeView_.findViewById(R.id.face_right);
			sunLeft_ = degreeView_.findViewById(R.id.sun_left);
			sunRight_ = degreeView_.findViewById(R.id.sun_right);
			arrowLeft_ = degreeView_.findViewById(R.id.arrow_left);
			arrowRight_ = degreeView_.findViewById(R.id.arrow_right);
			return true;
		}
		
		@Override
		public void finish() {
			
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
		return true;
	}

	@Override
	public InteractState onInteracting(CacheBean param) {
		DetectDegreeCacheBean bean = checkParam(param);
		if(bean == null) {
			return InteractState.CONTINUE;
		}
		if(degree_ != null) {
			String text="获取到了角度："+degree_.x+","+degree_.y;
			Toast toast = Toast.makeText(cacheBean_.context_,text, Toast.LENGTH_SHORT);
			toast.show();
		}
		return InteractState.CONTINUE;
	}

	@Override
	public void onInteractFinish(CacheBean param) {
		DetectDegreeCacheBean bean = checkParam(param);
		if(bean == null) {
			return ;
		}
		if(detectDegreeInteractor_ != null && detectDegreeInteractor_.isRunning()) {
			detectDegreeInteractor_.stopInteract();
		}
	}
	
	private DetectDegreeCacheBean checkParam(CacheBean param) {
		if(param != null && param instanceof DetectDegreeCacheBean) {
			DetectDegreeCacheBean bean = (DetectDegreeCacheBean)param;
			if(bean.context_ == null || bean.camera_ == null) {
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
