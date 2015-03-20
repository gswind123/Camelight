package com.camelight.android.business;


import com.camelight.android.business.Interaction.InteractState;
import com.camelight.android.model.CacheBean;

import android.os.Handler;

public class Interactor {
	private Interaction action_ = null;
	private Handler handler_;
	private boolean bRunning_ = false;
	private CacheBean param_;
	
	//handler must not be null
	public Interactor(Handler handler){
		if(handler == null) {
			throw new NullPointerException();
		}
		this.handler_ = handler; 
	}
	public void setParam(CacheBean param){
		if(bRunning_) {
			return ;
		}
		param_ = param;
	}
	public void setInteraction(Interaction action){
		if(bRunning_) {
			return ;
		}
		action_ = action;
		if(action_ != null) {
			action_.setInteractor(this);
		}
	}
	public void startInteract(){
		startInteract(100);
	}
	public void startInteract(int interval_milisec){
		if(action_ == null || bRunning_) {
			return ;
		}
		final int interval = interval_milisec;
		bRunning_ = true;
		Thread thread = new Thread(){
			@Override
			public void run(){
				boolean conti = false;
				if(bRunning_ && action_ != null) {
					if(action_.onInteracting(param_) == InteractState.CONTINUE) {
						conti = true;
					}
				}
				if(conti) {
					handler_.postDelayed(this, interval);
				} else {
					bRunning_ = false;
					action_.onInteractFinish(param_);
				}
			}
		};
		boolean start = false;
		if(action_ != null) {
			start = action_.onInteractStart(param_);
		}
		if(start) {
			handler_.postDelayed(thread, interval_milisec);
		}
		
	}
	public void stopInteract(){
		bRunning_ = false;
	}
	public boolean isRunning(){
		return bRunning_;
	}
	/*
	 * called when the handler belongs to a child thread,
	 * and it's time to terminate this thread in a soft way
	 * @param
	 * @return
	 * */
	public void stopLooper(){
		handler_.getLooper().quit();
	}
}
