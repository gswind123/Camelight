package com.camelight.android.business;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.engine.OpenCVEngineInterface;
import org.opencv.imgproc.Imgproc;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.Bitmap.Config;
import android.media.FaceDetector.Face;
import android.os.Message;

import com.camelight.android.model.CacheBean;
import com.camelight.android.model.CameraFrame;
import com.camelight.android.model.DetectDegreeCacheBean;
import com.camelight.android.util.FaceExtractor;
import com.camelight.android.util.FrameProcessor;
import com.camelight.android.util.ImageProcessor;

public class DetectDegreeInteraction extends Interaction{

	public DetectDegreeCacheBean cacheBean_ = new DetectDegreeCacheBean();
	
	private long lastTime_ = 0;
	
	@Override
	public boolean onInteractStart(CacheBean param) {
		DetectDegreeCacheBean bean = checkParam(param);
		if(bean == null) {
			return false;
		}
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
		bm = ImageProcessor.rotate(bm, 90);
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
			FrameProcessor.GetIlluminationMap(fixed_mat.nativeObj);
			Bitmap face_bm = Bitmap.createBitmap(fixed_mat.cols(), fixed_mat.rows(), Config.RGB_565);
			Utils.matToBitmap(fixed_mat, face_bm);
			int index = (int)FrameProcessor.Predict(face_bm);
			int angle[] = FrameProcessor.getPredictData(index);
			bean.setDegree(new PointF(angle[0], angle[1]));
		} else {
			bean.setDegree(null);
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
