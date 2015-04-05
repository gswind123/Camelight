package com.camelight.android.util;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v4.app.FragmentActivity;

public class OrientationUtil implements SensorEventListener {
	private float orientation_ = -1;
	private boolean registered_ = false;
	
	static private OrientationUtil instance_ = new OrientationUtil();
	private OrientationUtil() {}
	
	/** 
	 * Register the Util to the activity
	 * @param the activity to register
	 * @return
	 * */
	@SuppressWarnings("deprecation")
	static public boolean register(FragmentActivity activity) {
		do{//while false
			if(activity == null) {
				break;
			}
			SensorManager sm = (SensorManager)activity.getSystemService(Context.SENSOR_SERVICE);
			Sensor sensor = sm.getDefaultSensor(Sensor.TYPE_ORIENTATION);
			sm.registerListener(instance_, sensor, SensorManager.SENSOR_DELAY_NORMAL);
			instance_.registered_ = true;	
		}while(false);
		return instance_.registered_;
	}
	
	/** 
	 * Get the orientation of the device;
	 * @note make sure OrientationUtil is registered when this called
	 * @param
	 * @return 0:NORTH; 90:EAST; 180:SOUTH; 270:WEST*/
	static public float getOrientation() {
		return instance_.orientation_;
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		orientation_ = event.values[0];
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		
	}
}
