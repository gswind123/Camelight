package com.camelight.android.view;

import com.camelight.android.business.BusinessMode;
import com.camelight.android.model.AnimationCacheBean;
import com.camelight.android.model.CacheBean;
import com.camelight.android.model.DetectModeCacheBean;

import com.camelight.android.R;

import android.R.anim;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

public class ConfirmModeFragment extends Fragment implements OnClickListener{
	static public String TAG = "ConfirmModeFragment";
	
	private DetectModeCacheBean cacheBean_ = new DetectModeCacheBean();
	private View mainView_ = null;
	private View contentContainer_ = null;
	private View modeLayout_ = null;
	private ImageView modeImage_ = null;
	private TextView modeText_ = null;
	private View confirmLayout_ = null;
	private Runnable onFinish_ = null;
	private AnimationCacheBean animCache_ = new AnimationCacheBean();
	
	private Runnable onSelectModeFinish_ = new Runnable(){
		@Override
		public void run() {
			setModeLayoutByCacheBean();
			contentContainer_.setVisibility(View.VISIBLE);
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
		contentContainer_ = mainView_.findViewById(R.id.content_container);
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
		startSelectModeAnimation();
	}
	
	private void goSelectModeFragment(){
		SelectModeFragment fragment = SelectModeFragment.createInstance(cacheBean_);
		fragment.setAnimationCache(animCache_);
		fragment.setOnFinish(this.onSelectModeFinish_);
		FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
		ft.add(android.R.id.content, fragment, SelectModeFragment.TAG);
		ft.addToBackStack(SelectModeFragment.TAG);
		ft.commit();
	}
	
	private void startSelectModeAnimation() {
		//build mode layout's cache view
		modeLayout_.buildDrawingCache();
		Bitmap cache_bitmap = modeLayout_.getDrawingCache();
		if(cache_bitmap != null) {
			cache_bitmap = Bitmap.createBitmap(cache_bitmap);
		}
		ImageView mode_cache = new ImageView(getActivity());
		mode_cache.setBackgroundDrawable(new BitmapDrawable(cache_bitmap));
		int location[] = new int[2];
		int buffer[] = new int[2];
		modeLayout_.getLocationInWindow(location);
		mainView_.getLocationInWindow(buffer);
		location[0] -= buffer[0];
		location[1] -= buffer[1];
		FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
				modeLayout_.getMeasuredWidth(), modeLayout_.getMeasuredHeight());
		lp.setMargins(location[0], location[1], 0, 0);
		mode_cache.setLayoutParams(lp);
		animCache_.viewCache_ = mode_cache;
		//add cache view to the root layout
		ViewGroup root_view = (ViewGroup) this.getView();
		if(root_view != null) {
			root_view.addView(mode_cache);
		}
		animCache_.parentView_ = root_view;
		//start to let the content fade out
		Animation fade_out = new AlphaAnimation(1.f, 0.f);
		fade_out.setDuration(200);
		fade_out.setAnimationListener(new AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {}
			@Override
			public void onAnimationRepeat(Animation animation) {}
			@Override
			public void onAnimationEnd(Animation animation) {
				contentContainer_.setVisibility(View.GONE);
				goSelectModeFragment();
			}
		});
		contentContainer_.startAnimation(fade_out);
	}
	
	
	private void setModeLayoutByCacheBean(){
		int image_id = 0;
		int text_id = 0;
		BusinessMode mode = cacheBean_.mode_;
		if(mode == BusinessMode.FRONTLIGHT || mode == BusinessMode.NULL) {
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
			modeImage_.setImageResource(image_id);
			modeText_.setText(getResources().getString(text_id));
		}
	}
}
