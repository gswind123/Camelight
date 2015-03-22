package com.camelight.android.view.util;


public abstract class PropertyAnimation {
	public abstract boolean start();
	public abstract boolean update(long tweenMillsec);
	public abstract void finish();
}
