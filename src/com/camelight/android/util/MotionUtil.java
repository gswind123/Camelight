package com.camelight.android.util;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v4.app.FragmentActivity;

public class MotionUtil implements SensorEventListener{
	private float gravityOld_[] = {0.f, 0.f, 0.f};
	private float gravityNow_[] = {0.f, 0.f, 0.f};
	private float translation_[] = {0.f, 0.f, 0.f};
	static private final float G = 10.f;
	private boolean isInit_ = true;
	
	static private MotionUtil instance_ = new MotionUtil();
	
	private MotionUtil() {}
	
	/** 
	 * Register the Util to the activity
	 * @param the activity to register
	 * @return
	 * */
	static public void register(FragmentActivity activity){
		if(activity == null) {
			return ;
		}
		SensorManager sm = (SensorManager)activity.getSystemService(Context.SENSOR_SERVICE);
		Sensor sensor = sm.getDefaultSensor(Sensor.TYPE_GRAVITY);
		sm.registerListener(instance_, sensor, SensorManager.SENSOR_DELAY_GAME);
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		gravityNow_ = event.values;
		if(isInit_) {
			isInit_ = false;
			gravityOld_ = gravityNow_;
		} 
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}
	
	static public boolean isMoved(){
		float degree = 0;
		float point_multi = 0;
		for(int i=0;i<3;i++) {
			point_multi += instance_.gravityNow_[i] * instance_.gravityOld_[i];
		}
		float cos_theta = (point_multi)/(G*G);
		float theta = (float)(Math.acos(cos_theta) * 180 / Math.PI);
		degree += 3.3 * theta;
		return (degree >= 100.f);
	}
	
	static public void commitMotion(){
		instance_.gravityOld_ = instance_.gravityNow_;
	}
}
