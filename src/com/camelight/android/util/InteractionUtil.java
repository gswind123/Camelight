package com.camelight.android.util;

public class InteractionUtil {
	static private long lastTime_ = 0;
	
	static public boolean isDoubleClick() {
		if(lastTime_ == 0) {
			lastTime_ = System.currentTimeMillis();
			return false;
		}
		long cur_time = System.currentTimeMillis();
		long interval = cur_time - lastTime_;
		if(interval > 500) {
			lastTime_ = cur_time;
			return false;
		} else {
			return true;
		}
	}
}
