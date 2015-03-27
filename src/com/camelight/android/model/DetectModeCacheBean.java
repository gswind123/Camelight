package com.camelight.android.model;

import android.content.Context;
import android.graphics.Rect;
import android.media.FaceDetector.Face;
import android.widget.FrameLayout;

import com.camelight.android.business.BusinessMode;
import com.camelight.android.view.util.CameraView;

public class DetectModeCacheBean extends CacheBean {
	public Context context_ = null;
	public CameraView camera_ = null;
	public FrameLayout layout_ = null;
	
	public Face faces_[] = null;
	public Rect selectedFace_ = new Rect();
	public CameraFrame selectedFrame_ = null;
	public BusinessMode mode_ = BusinessMode.NULL;
}
