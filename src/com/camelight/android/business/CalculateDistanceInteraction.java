package com.camelight.android.business;

import org.opencv.core.Mat;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.media.FaceDetector.Face;
import android.os.Message;

import com.camelight.android.model.CacheBean;
import com.camelight.android.model.CalculateDistanceCacheBean;
import com.camelight.android.model.CameraFrame;
import com.camelight.android.util.FaceExtractor;
import com.camelight.android.util.FrameProcessor;
import com.camelight.android.util.ImageProcessor;

public class CalculateDistanceInteraction extends Interaction{

	public CalculateDistanceCacheBean cacheBean_ = new CalculateDistanceCacheBean();
	
	private long lastTime_ = 0;
	private float distance = 0;
	
	@Override
	public boolean onInteractStart(CacheBean param) {
		CalculateDistanceCacheBean bean = checkParam(param);
		if(bean == null) {
			return false;
		}
		return true;
	}

	@Override
	public InteractState onInteracting(CacheBean param) {
		CalculateDistanceCacheBean bean = checkParam(param);
		if(bean == null) {
			return InteractState.CONTINUE;
		}
		CameraFrame frame = bean.camera_.getLatestFrame();
		byte[] jpeg_data = frame.getJPEGData();
		Bitmap bm = BitmapFactory.decodeByteArray(jpeg_data, 0, jpeg_data.length);
		bm = ImageProcessor.rotate(bm, 90);
		FaceExtractor detector = new FaceExtractor(bm);
		detector.detectFaces();
		Face faces[] = detector.getFaces();
		if(faces != null) {
			/*TODO:添加优化的人脸选择*/
			Face face = faces[0];
			org.opencv.core.Rect cv_rect = detector.getFaceRect(face);
			Rect rect = new Rect(cv_rect.x, cv_rect.y, cv_rect.x+cv_rect.width, cv_rect.y+cv_rect.height);
			bean.setFaceRect(rect);
			Mat rgba = ImageProcessor.bitmap2Mat(bm);
			Mat face_mat = new Mat(rgba, cv_rect);
			distance = FrameProcessor.CalculateBestDistance(face_mat.nativeObj);
			bean.setDistance(distance);
		} else {
			bean.setDistance(0);
		}
		Message msg = new Message();
		long cur_time = System.currentTimeMillis();
		if(lastTime_ == 0) {
			msg.what = -1;
		} else {
			msg.what = (int)(cur_time - lastTime_);
		}
		lastTime_ = cur_time;
		bean.uiInteraction_.sendMessage(msg);
		return InteractState.CONTINUE;
	}

	@Override
	public void onInteractFinish(CacheBean param) {
		getInteractor().stopLooper();
	}
	
	private CalculateDistanceCacheBean checkParam(CacheBean param){
		if(param != null && param instanceof CalculateDistanceCacheBean) {
			CalculateDistanceCacheBean bean = (CalculateDistanceCacheBean)param;
			if(bean.camera_ == null || bean.context_ == null || bean.uiInteraction_ == null) {
				return null;
			}
			cacheBean_ = bean;
			return cacheBean_;
		}
		return null;
	}
	
}
