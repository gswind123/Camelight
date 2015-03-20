package com.camelight.android.business;
import com.camelight.android.model.CacheBean;

public abstract class Interaction {
	public enum InteractState{
		STOP("stop interacting"),
		CONTINUE("continue interacting");
		
		public String description_;
		InteractState(String des){
			description_ = des;
		}
	}
	private Interactor interactor_ = null;
	public void setInteractor(Interactor inter){
		interactor_  = inter;
	}
	public Interactor getInteractor() {
		return interactor_;
	}
	/* 
	 * @return true:start work; false:onInteracting will not work
	 * */
	public abstract boolean onInteractStart(CacheBean param);
	/*
	 * @return return STOP to terminate the interaction
	 * */
	public abstract InteractState onInteracting(CacheBean param);
	public abstract void onInteractFinish(CacheBean param);
}
