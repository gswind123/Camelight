package com.camelight.android.business;

public enum BusinessMode {
	NULL("no_mode"),
	FRONTLIGHT("front_light"),
	BACKLIGHT("back_light"),
	NIGHT("night");
	
	public String description_ = "";
	private BusinessMode(String des) {
		description_ = des;
	}
}
