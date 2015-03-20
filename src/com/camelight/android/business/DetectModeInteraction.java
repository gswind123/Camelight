package com.camelight.android.business;

import org.opencv.core.Mat;
import org.opencv.engine.OpenCVEngineInterface;
import org.opencv.imgproc.Imgproc;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.graphics.Rect;
import android.media.FaceDetector.Face;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.FrameLayout;

import com.camelight.android.R;
import com.camelight.android.model.CacheBean;
import com.camelight.android.model.CameraFrame;
import com.camelight.android.model.DetectModeCacheBean;
import com.camelight.android.util.FaceExtractor;
import com.camelight.android.util.FrameProcessor;
import com.camelight.android.util.ImageProcessor;
import com.camelight.android.view.CameraActivity;
import com.camelight.android.view.util.CameraView;

public class DetectModeInteraction extends Interaction {
	
	private DetectModeCacheBean cacheBean_ = null;
	private boolean isFaceDetected = false;
	
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean onInteractStart(CacheBean param) {
		DetectModeCacheBean cache_bean = checkParam(param);
		if(cache_bean == null) {
			return false;
		}
		isFaceDetected  = false;
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
		bm = ImageProcessor.rotate(bm, 90);
		FaceExtractor detector = new FaceExtractor(bm);
		detector.detectFaces();
		Face faces[] = detector.getFaces();
		if(faces != null) {
			cache_bean.faces_ = faces;
			cache_bean.selectedFrame_ = cur_frame;
			Mat gray = ImageProcessor.bitmap2GrayMat(bm);
			Rect android_rect = detector.getFaceRect(faces[0]);
			org.opencv.core.Rect face_rect = new org.opencv.core.Rect(
					android_rect.left,android_rect.top,android_rect.width(),android_rect.height());
			cache_bean.mode_ = FrameProcessor.AnalyzeMode(gray.nativeObj, face_rect);
			isFaceDetected = true;
			return InteractState.STOP;
		}
		return InteractState.CONTINUE;
	}

	@Override
	public void onInteractFinish(CacheBean param) {
		DetectModeCacheBean cache_bean = checkParam(param);
		if(cache_bean == null) {
			return ;
		}
		if(cache_bean.context_ instanceof CameraActivity) {
			Message result = new Message();
			result.what = BusinessState.DETECT_FACE_FINISH;
			CameraActivity act = (CameraActivity) cache_bean.context_;
			Handler business_handler = act.getBusinessHandler();
			business_handler.sendMessage(result);
		}
	}
	
	private DetectModeCacheBean checkParam(CacheBean param) {
		if(param instanceof DetectModeCacheBean) {
			DetectModeCacheBean cache_bean = (DetectModeCacheBean) param;
			if(cache_bean.camera_ != null && cache_bean.frame_ != null && cache_bean.context_ != null) {
				cacheBean_ = cache_bean;
				return cache_bean;
			}
		}
		return null;
	}
}
