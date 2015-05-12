package com.camelight.android.business;

import org.opencv.core.Mat;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.FaceDetector.Face;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import com.camelight.android.R;
import com.camelight.android.model.CacheBean;
import com.camelight.android.model.CameraFrame;
import com.camelight.android.model.DetectModeCacheBean;
import com.camelight.android.util.FaceExtractor;
import com.camelight.android.util.FrameProcessor;
import com.camelight.android.util.ImageProcessor;
import com.camelight.android.util.InteractionUtil;
import com.camelight.android.view.CameraActivity;
import com.camelight.android.view.util.CameraView;

public class DetectModeInteraction extends Interaction {
	
	private DetectModeCacheBean cacheBean_ = null;
	
	private boolean isCanceled_ = false;
	private View mainView_ = null;
	private OnClickListener onCancelClick_ = new OnClickListener(){
		@Override
		public void onClick(View v) {
			if(InteractionUtil.isDoubleClick()) {
				return ;
			}
			DetectModeCacheBean bean = checkParam(cacheBean_);
			if(bean == null) {
				return ;
			}
			isCanceled_ = true;
			CameraActivity act = (CameraActivity)bean.context_;
			act.cancelCurrentInteraction();
		}
	};
	
	@Override
	public boolean onInteractStart(CacheBean param) {
		DetectModeCacheBean cache_bean = checkParam(param);
		if(cache_bean == null) {
			return false;
		}
		/** add detect mode view*/
		LayoutInflater inflater = (LayoutInflater)cacheBean_.context_.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mainView_ = inflater.inflate(R.layout.detect_mode_view, null);
		View btn_cancel = mainView_.findViewById(R.id.btn_close);
		btn_cancel.setOnClickListener(onCancelClick_);
		cacheBean_.layout_.addView(mainView_);
		return true;
	}

	@Override
	public InteractState onInteracting(CacheBean param) {
		DetectModeCacheBean cache_bean = checkParam(param);
		if(cache_bean == null) {
			return InteractState.CONTINUE;
		}
		CameraView camera = cacheBean_.camera_;
		CameraFrame cur_frame = camera.getLatestFrame();
		byte[] data = cur_frame.getJPEGData();
		Bitmap bm = BitmapFactory.decodeByteArray(data, 0, data.length);
		bm = ImageProcessor.rotate(bm, cur_frame.getRotation());
		FaceExtractor detector = new FaceExtractor(bm);
		detector.detectFaces();
		Face faces[] = detector.getFaces();

		cache_bean.faces_ = faces;
		cache_bean.selectedFrame_ = cur_frame;
		Mat rgba = ImageProcessor.bitmap2Mat(bm);
		//org.opencv.core.Rect cv_rect = detector.getFaceRect(faces[0]);
		org.opencv.core.Rect cv_rect = new org.opencv.core.Rect();
		cache_bean.mode_ = FrameProcessor.AnalyzeMode(rgba.nativeObj, cv_rect);

		
		((CameraActivity)(cache_bean.context_)).updatePreview(cache_bean.mode_.description_);
		Toast toast =Toast.makeText(cache_bean.context_, cache_bean.mode_.description_, Toast.LENGTH_SHORT);
		toast.show();
		
		return InteractState.CONTINUE;
	}

	
	@Override
	public void onInteractFinish(CacheBean param) {
		DetectModeCacheBean cache_bean = checkParam(param);
		if(cache_bean == null) {
			return ;
		}
		if(cache_bean.context_ instanceof CameraActivity) {
			CameraActivity act = (CameraActivity) cache_bean.context_;
			/** remove detect mode view*/
			cache_bean.layout_.removeView(mainView_);
			/** inform the activity the interaction is done*/
			Message result = new Message();
			Handler business_handler = act.getBusinessHandler();
			business_handler.sendMessage(result);
		}
	}
	
	private DetectModeCacheBean checkParam(CacheBean param) {
		if(param instanceof DetectModeCacheBean) {
			DetectModeCacheBean cache_bean = (DetectModeCacheBean) param;
			if(cache_bean.camera_ != null && cache_bean.layout_ != null && cache_bean.context_ != null) {
				cacheBean_ = cache_bean;
				return cache_bean;
			}
		}
		return null;
	}
}
