package com.camelight.android.business;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.media.FaceDetector.Face;
import android.os.Message;

import com.camelight.android.model.CacheBean;
import com.camelight.android.model.CameraFrame;
import com.camelight.android.model.DetectDegreeCacheBean;
import com.camelight.android.util.ContinuousDataTracker;
import com.camelight.android.util.FaceExtractor;
import com.camelight.android.util.FrameProcessor;
import com.camelight.android.util.ImageProcessor;
import com.camelight.android.util.OrientationUtil;

public class DetectDegreeInteraction extends Interaction{

	public DetectDegreeCacheBean cacheBean_ = new DetectDegreeCacheBean();
	
	/** the tracker is with an ambulator for degrees in [0, 360] */
	private ContinuousDataTracker tracker_ = new ContinuousDataTracker(new ContinuousDataTracker.Ambulator(){
		@Override
		public float ambulate(float a,float b) {
			float dis = (a-b+360)%360;
			return Math.min(dis, 360-dis);
		}
	});
	private long lastTime_ = 0;
	
	@Override
	public boolean onInteractStart(CacheBean param) {
		DetectDegreeCacheBean bean = checkParam(param);
		if(bean == null) {
			return false;
		}
		tracker_.clear();
		return true;
	}

	@Override
	public InteractState onInteracting(CacheBean param) {
		DetectDegreeCacheBean bean = checkParam(param);
		if(bean == null) {
			return InteractState.CONTINUE;
		}
		CameraFrame frame = bean.camera_.getLatestFrame();
		byte[] jpeg_data = frame.getJPEGData();
		Bitmap bm = BitmapFactory.decodeByteArray(jpeg_data, 0, jpeg_data.length);
		bm = ImageProcessor.rotate(bm, frame.getRotation());
		FaceExtractor detector = new FaceExtractor(bm);
		detector.detectFaces();
		Face faces[] = detector.getFaces();
		if(faces != null) {
			/*TODO:添加优化的人脸选择*/
			Face face = faces[0];
			org.opencv.core.Rect rect = detector.getFaceLowRect(face);
			Mat rgba = ImageProcessor.bitmap2Mat(bm);
			Mat face_low_mat = new Mat(rgba, rect);
			Mat fixed_mat = new Mat(FrameProcessor.PREDICT_HEIGHT, FrameProcessor.PREDICT_WIDTH, CvType.CV_8UC4);
			Imgproc.resize(face_low_mat, fixed_mat, fixed_mat.size());
			float degree = FrameProcessor.GetLightDegree(fixed_mat.nativeObj);
			int angle[] = {0, (int)degree};
			int light_otn = (int)OrientationUtil.getOrientation();
			if(frame.isMirror()) {
				light_otn = (light_otn - angle[1] + 360)%360;
			} else {
				light_otn = (light_otn + angle[1] + 360)%360;
			}
			tracker_.addData(light_otn);
			if(tracker_.size() == tracker_.Capacity) {
				light_otn = (int)tracker_.calcLastestData();
			}
			bean.setOrientation(light_otn);
			bean.bitmap_ = Bitmap.createBitmap(bm, rect.x, rect.y, rect.width, rect.height);
			bean.faceRect_ = new Rect(rect.x, rect.y, rect.x+rect.width, rect.y+rect.height);
			bean.adjustedFrame_ = bm;
		} else {
			bean.setOrientation(-1);
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
	
	private DetectDegreeCacheBean checkParam(CacheBean param){
		if(param != null && param instanceof DetectDegreeCacheBean) {
			DetectDegreeCacheBean bean = (DetectDegreeCacheBean)param;
			if(bean.camera_ == null || bean.context_ == null || bean.uiInteraction_ == null) {
				return null;
			}
			cacheBean_ = bean;
			return cacheBean_;
		}
		return null;
	}
	
}
