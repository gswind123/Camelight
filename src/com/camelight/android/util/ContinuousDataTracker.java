package com.camelight.android.util;

import java.util.ArrayList;

public class ContinuousDataTracker {
	/** 
	 * A tool to calc distance of two data;
	 * @NOTE: ambulate(a,b) == ambulate(b,a)
	 * */
	static public class Ambulator {
		public float ambulate(float a, float b) {
			return Math.abs(a - b);
		}
	}
	private Ambulator ambulator_ = new Ambulator();
	private ArrayList<Float> storage_ = new ArrayList<Float>();
	public final int Capacity = 6;
	public ContinuousDataTracker() {}
	public ContinuousDataTracker(Ambulator am) {
		if(am != null) {
			ambulator_ = am;
		}
	}
	
	public void addData(float data) {
		storage_.add(data);
		if(storage_.size() > Capacity) {
			storage_.remove(0);
		}
	}
	
	/** The complexity is (size)^2*/
	public float calcLastestData() {
		int size = storage_.size();
		if(size < 2){
			if(size == 0) {
				return -1.f;
			} else {
				return storage_.get(size-1);
			}
		}
		ArrayList<Float> cluster_1 = new ArrayList<Float>();
		ArrayList<Float> cluster_2 = new ArrayList<Float>();
		float polar_1 = storage_.get(0);
		float polar_2 = storage_.get(0);
		float max_dis = 0;
		for(int i=0;i<size;i++) {
			for(int j=i+1;j<size;j++) {
				float tmp_1 = storage_.get(i);
				float tmp_2 = storage_.get(j);
				float dis = ambulator_.ambulate(tmp_1, tmp_2);
				if(dis > max_dis) {
					max_dis = dis;
					polar_1 = tmp_1;
					polar_2 = tmp_2;
				}
			}
		}
		
		for(int i=0;i<size;i++) {
			float cur = storage_.get(i);
			float dis_1 = ambulator_.ambulate(polar_1, cur);
			float dis_2 = ambulator_.ambulate(cur, polar_2);
			if(dis_1 < dis_2) {
				cluster_1.add(cur);
			} else {
				cluster_2.add(cur);
			}
		}
		if(cluster_1.size() > cluster_2.size()) {
			return cluster_1.get(cluster_1.size()-1);
		} else {
			return cluster_2.get(cluster_2.size()-1);
		}
	}
	public void clear() {
		storage_.clear();
	}
	public int size() {
		return storage_.size();
	}
}
