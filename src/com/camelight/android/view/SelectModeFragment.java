package com.camelight.android.view;

import com.camelight.android.R;
import com.camelight.android.business.BusinessMode;
import com.camelight.android.model.AnimationCacheBean;
import com.camelight.android.model.CacheBean;
import com.camelight.android.model.DetectModeCacheBean;
import com.camelight.android.util.ModeDetector;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.TranslateAnimation;
import android.view.ViewGroup;
import android.widget.ImageView;

public class SelectModeFragment extends Fragment implements OnClickListener{
	static public String TAG = "SelectModeFragment";
	
	private DetectModeCacheBean cacheBean_ = new DetectModeCacheBean();
	private ViewGroup mainView_ = null;
	private ViewGroup contentContainer_ =null;
	private View frontLightView_ = null;
	private View backLightView_ = null;
	private View nightView_ = null;
	private Runnable onFinish_ = null;
	private AnimationCacheBean animCache_ = null;
	private boolean startWithAnimation_ = false;//make sure the animation will only run once
	
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
		contentContainer_ = (ViewGroup)mainView_.findViewById(R.id.content_container);
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
	
	public void setAnimationCache(AnimationCacheBean anim_cache) {
		startWithAnimation_  = true;
		animCache_ = anim_cache;
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
	
	private View getSelectedModelayout() {
		if(cacheBean_.mode_ == BusinessMode.FRONTLIGHT) {
			return frontLightView_;
		} else if(cacheBean_.mode_ == BusinessMode.BACKLIGHT) {
			return backLightView_;
		} else if(cacheBean_.mode_ == BusinessMode.NIGHT) {
			return nightView_;
		} else 
			return null;
	}
	
	private void quit(){
		ModeDetector.setMode(cacheBean_.mode_);
		getActivity().onBackPressed();
	}
	
	private void startStartAnimation() {
		ViewGroup root_view = (ViewGroup) this.getView();
		root_view.measure(0, 0);
		root_view.layout(0, 0, root_view.getMeasuredWidth(), root_view.getMeasuredHeight());
		View dst_layout = getSelectedModelayout();
		if(animCache_ == null || animCache_.viewCache_ == null || dst_layout == null) {
			contentContainer_.setVisibility(View.VISIBLE);
			return ;
		}
		ImageView view_cache = animCache_.viewCache_;

		//calculate start and dst location
		int cur_loc[] = new int[2];
		int dst_loc[] = new int[2];;
		view_cache.getLocationInWindow(cur_loc);
		dst_layout.getLocationInWindow(dst_loc);
		dst_loc[0] -= cur_loc[0];
		dst_loc[1] -= cur_loc[1];
		//move view cache to the dst location
		Animation move = new TranslateAnimation(
				Animation.ABSOLUTE, 0,
				Animation.ABSOLUTE, dst_loc[0],
				Animation.ABSOLUTE, 0,
				Animation.ABSOLUTE, dst_loc[1]);
		move.setDuration(200);
		move.setAnimationListener(new AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {}
			@Override
			public void onAnimationRepeat(Animation animation) {}
			@Override
			public void onAnimationEnd(Animation animation) {
				if(animCache_.parentView_ != null) {
					animCache_.parentView_.removeView(animCache_.viewCache_);
					animCache_.parentView_ = null;
				}
				contentContainer_.setVisibility(View.VISIBLE);
			}
		});
		animCache_.viewCache_.startAnimation(move);
		
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
			cacheBean_.mode_ = BusinessMode.NULL;
		}
		quit();
	}
}