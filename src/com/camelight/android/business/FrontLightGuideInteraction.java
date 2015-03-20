package com.camelight.android.business;

import android.graphics.PointF;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.Toast;

import com.camelight.android.model.CacheBean;
import com.camelight.android.model.DetectDegreeCacheBean;

public class FrontLightGuideInteraction extends Interaction{

	public DetectDegreeCacheBean cacheBean_ = new DetectDegreeCacheBean();
	public PointF degree_ = null;
	
	private Interactor detectDegreeInteractor_ = null;
	private Thread detectDegreeThread_ = null;
	private Handler msgHandler_ = null;
	
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
