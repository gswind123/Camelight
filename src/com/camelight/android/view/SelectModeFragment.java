package com.camelight.android.view;

import com.camelight.android.R;
import com.camelight.android.business.BusinessMode;
import com.camelight.android.model.CacheBean;
import com.camelight.android.model.DetectModeCacheBean;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

public class SelectModeFragment extends Fragment implements OnClickListener{
	static public String TAG = "SelectModeFragment";
	
	private DetectModeCacheBean cacheBean_ = new DetectModeCacheBean();
	private ViewGroup mainView_ = null;
	private View frontLightView_ = null;
	private View backLightView_ = null;
	private View nightView_ = null;
	
	static public SelectModeFragment createInstance(CacheBean detectModeCacheBean) {
		if(detectModeCacheBean != null && detectModeCacheBean instanceof DetectModeCacheBean) {
			SelectModeFragment fragment = new SelectModeFragment();
			fragment.cacheBean_ = (DetectModeCacheBean)detectModeCacheBean;
			return fragment;
		} else {
			return null;
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstance) {
		mainView_ = (ViewGroup)inflater.inflate(R.layout.select_model_fragment, null);
		frontLightView_ = mainView_.findViewById(R.id.front_light_mode);
		backLightView_ = mainView_.findViewById(R.id.back_light_mode);
		nightView_ = mainView_.findViewById(R.id.night_mode);
		frontLightView_.setOnClickListener(this);
		backLightView_.setOnClickListener(this);
		nightView_.setOnClickListener(this);
		mainView_.setOnClickListener(this);
		updateModeView();
		return mainView_;
	}
	
	private void updateModeView(){
		BusinessMode mode = cacheBean_.mode_;
		boolean select_front_light = false;
		boolean select_back_light = false;
		boolean select_night = false;
		if(mode == BusinessMode.FRONTLIGHT) {
			select_front_light = true;
		} else if(mode == BusinessMode.BACKLIGHT) {
			select_back_light = true;
		} else if(mode == BusinessMode.NIGHT) {
			select_night = true;
		}
		frontLightView_.setSelected(select_front_light);
		backLightView_.setSelected(select_back_light);
		nightView_.setSelected(select_night);
	}
	
	private void quit(){
		getActivity().onBackPressed();
	}

	@Override
	public void onClick(View v) {
		int view_id  = v.getId();
		if(view_id == R.id.front_light_mode) {
			cacheBean_.mode_ = BusinessMode.FRONTLIGHT;
		} else if(view_id == R.id.back_light_mode) {
			cacheBean_.mode_ = BusinessMode.BACKLIGHT;
		} else if(view_id == R.id.night_mode) {
			cacheBean_.mode_ = BusinessMode.NIGHT;
		} else if(view_id == R.id.main_layout) {
			;
		}
		quit();
	}
}
