package com.camelight.android.view;

import com.camelight.android.business.BusinessMode;
import com.camelight.android.model.CacheBean;
import com.camelight.android.model.DetectModeCacheBean;

import com.camelight.android.R;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

public class ConfirmModeFragment extends Fragment implements OnClickListener{
	static public String TAG = "ConfirmModeFragment";
	
	private DetectModeCacheBean cacheBean_ = new DetectModeCacheBean();
	private View mainView_ = null;
	private View modeLayout_ = null;
	private ImageView modeImage_ = null;
	private TextView modeText_ = null;
	private View confirmLayout_ = null;
	private Runnable onFinish_ = null;
	
	private Runnable onSelectModeFinish_ = new Runnable(){
		@Override
		public void run() {
			setModeLayoutByCacheBean();
			mainView_.setVisibility(View.VISIBLE);
		}
	};
			
			
	static public ConfirmModeFragment createInstance(CacheBean detectModeCacheBean){
		if(detectModeCacheBean != null && detectModeCacheBean instanceof DetectModeCacheBean) {
			ConfirmModeFragment fragment = new ConfirmModeFragment();
			fragment.cacheBean_ = (DetectModeCacheBean)detectModeCacheBean;
			return fragment;
		} else {
			return null;
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstance) {
		mainView_ = inflater.inflate(R.layout.confirm_mode_fragment, null);
		modeLayout_ = mainView_.findViewById(R.id.detected_mode);
		modeImage_ = (ImageView)mainView_.findViewById(R.id.detected_mode_image);
		modeText_ = (TextView)mainView_.findViewById(R.id.detected_mode_text);
		confirmLayout_ = mainView_.findViewById(R.id.confirm_mode);
		mainView_.setOnClickListener(this);
		modeLayout_.setOnClickListener(this);
		confirmLayout_.setOnClickListener(this);
		setModeLayoutByCacheBean();
		return mainView_;
	}
	@Override
	public void onClick(View v) {
		int view_id = v.getId();
		if(view_id == R.id.detected_mode) {
			selectMode();
		} else if(view_id == R.id.confirm_mode) {
			quit();
		} else if(view_id == R.id.main_layout) {
			;
		}
	}
	
	@Override 
	public void onDestroy() {
		if(onFinish_ != null) {
			onFinish_.run();	
		}
		super.onDestroy();
	}
	
	public void setOnFinish(Runnable finish) {
		onFinish_ = finish;
	}
	
	private void quit(){
		getActivity().onBackPressed();
	}
	
	private void selectMode(){
		SelectModeFragment fragment = SelectModeFragment.createInstance(cacheBean_);
		fragment.setOnFinish(this.onSelectModeFinish_);
		FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
		ft.add(android.R.id.content, fragment, SelectModeFragment.TAG);
		ft.addToBackStack(SelectModeFragment.TAG);
		ft.commit();
		mainView_.setVisibility(View.GONE);
	}
	
	private void setModeLayoutByCacheBean(){
		int image_id = 0;
		int text_id = 0;
		BusinessMode mode = cacheBean_.mode_;
		if(mode == BusinessMode.FRONTLIGHT) {
			image_id = R.drawable.pic_front_light_mode;
			text_id = R.string.front_light_mode;
		} else if(mode == BusinessMode.BACKLIGHT) {
			image_id = R.drawable.pic_back_light_mode;
			text_id = R.string.back_light_mode;
		} else if(mode == BusinessMode.NIGHT) {
			image_id = R.drawable.pic_night_mode;
			text_id = R.string.night_mode;
		}
		if(image_id != 0) {
			modeImage_.setBackgroundResource(image_id);
			modeText_.setText(getResources().getString(text_id));
		}
	}
}