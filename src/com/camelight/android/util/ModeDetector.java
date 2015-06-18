package com.camelight.android.util;

import java.util.ArrayList;

import org.opencv.core.Mat;
import org.opencv.core.Rect;

import android.provider.CalendarContract.Instances;

import com.camelight.android.business.BusinessMode;

public class ModeDetector {
	private class ModeWithTime{
		public BusinessMode mode_ = BusinessMode.NULL;
		public long time_ = -1;
		public ModeWithTime(BusinessMode mode, long time) {
			mode_ = mode;
			time_ = time;
		}
	}
	private class ModeCounter{
		private int ary_[] = new int[3];
		private int mode2Index(BusinessMode mode){
			if(mode == BusinessMode.FRONTLIGHT) {
				return 0;
			} else if(mode == BusinessMode.BACKLIGHT) {
				return 1;
			} else if(mode == BusinessMode.NIGHT) {
				return 2;
			} else {
				return -1;
			}
		}
		private BusinessMode index2Mode(int num){
			if(num == 0) {
				return BusinessMode.FRONTLIGHT;
			} else if(num == 1) {
				return BusinessMode.BACKLIGHT;
			} else if(num == 2) {
				return BusinessMode.NIGHT;
			} else {
				return BusinessMode.NULL;
			}
		}
		public ModeCounter(){
			for(int i=0;i<ary_.length;i++) {
				ary_[i] = 0;
			}
		}
		public void add(BusinessMode mode) {
			int i = mode2Index(mode);
			if(i != -1) {
				ary_[i]++;
			}
		}
		public BusinessMode getMaxMode(){
			int max_i = 0;
			for(int i=1;i<ary_.length;i++) {
				if(ary_[i] > ary_[max_i]) {
					max_i = i;
				}
			}
			return index2Mode(max_i);
		}
	}
	
	static private final int ListCapacity = 10;
	static private final int TimeThreshold = 3000;
	
	static private ModeDetector instance_ = new ModeDetector();
	
	private ArrayList<ModeWithTime> list_ = new ArrayList<ModeWithTime>();
	private BusinessMode settedMode_ = BusinessMode.NULL;
	
	private BusinessMode privateDetectMode(Mat mat , Rect face_rect) {
		BusinessMode mode = FrameProcessor.AnalyzeMode(mat.nativeObj, face_rect);
		if(list_.size() >= ListCapacity) {
			list_.remove(0);
		}
		long cur_time = System.currentTimeMillis();
		list_.add(new ModeWithTime(mode, cur_time));
		
		ModeCounter counter = new ModeCounter();
		for(int i=list_.size()-1;i>=0;i--) {
			ModeWithTime tmp = list_.get(i);
			int interval = (int)(cur_time - tmp.time_);
			if(interval <= TimeThreshold) {
				counter.add(tmp.mode_);
			} else break;
		}
		mode = counter.getMaxMode();
		if(settedMode_ != BusinessMode.NULL) {
			mode = settedMode_;
		}
		return mode;
	}
	
	static public BusinessMode detectMode(Mat mat, Rect face_rect) {
		return instance_.privateDetectMode(mat, face_rect);
	}
	static public void setMode(BusinessMode mode){
		instance_.settedMode_ = mode;
	}
}
