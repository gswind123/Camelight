package com.camelight.android.business;

import java.util.Queue;
import java.util.LinkedList;
import org.opencv.core.Mat;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.media.FaceDetector.Face;
import android.os.Message;
import android.widget.Toast;

import com.camelight.android.model.CacheBean;
import com.camelight.android.model.CalculateDistanceCacheBean;
import com.camelight.android.model.CameraFrame;
import com.camelight.android.util.FaceExtractor;
import com.camelight.android.util.FrameProcessor;
import com.camelight.android.util.ImageProcessor;
import com.camelight.android.util.ModeDetector;

public class CalculateDistanceInteraction extends Interaction{

	public CalculateDistanceCacheBean cacheBean_ = new CalculateDistanceCacheBean();
	
	private long lastTime_ = 0;
	private float drawWidth = 0;
	Queue<Integer> q;
	private int capacity = 5;
	private int detectCtr_ = 0;
	private final int DetectThreshold = 5;
	
	@Override
	public boolean onInteractStart(CacheBean param) {
		q = new LinkedList<Integer>();
		CalculateDistanceCacheBean bean = checkParam(param);
		if(bean == null) {
			return false;
		}
		return true;
	}

	@Override
	public InteractState onInteracting(CacheBean param) {
		int pre_dur = 0;
		long start_time = System.currentTimeMillis();
		
		CalculateDistanceCacheBean bean = checkParam(param);
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
		bean.curFrame_ = frame;
		org.opencv.core.Rect cv_rect = new org.opencv.core.Rect();
		if(faces != null) {
			Face face = faces[0];
			cv_rect = detector.getFaceRect(face);
			Rect rect = new Rect(cv_rect.x, cv_rect.y, cv_rect.x+cv_rect.width, cv_rect.y+cv_rect.height);
			bean.setFaceRect(rect);
			Bitmap face_bitmap = Bitmap.createBitmap(bm, rect.left, rect.right, rect.width(), rect.height());
			Mat face_mat = ImageProcessor.bitmap2Mat(face_bitmap);
			if(q.size() > capacity) {
				q.remove();	
			}
			
			pre_dur = (int)(System.currentTimeMillis() - start_time);
			
			q.add(FrameProcessor.nativeGetMeanValue(face_mat.nativeObj));
			int sum = 0;
			for (Integer i : q) {
				sum += i;
			}
			int avg = sum / q.size();
			
			drawWidth = FrameProcessor.CalculateBestDistance(avg, bm.getWidth()*bm.getHeight(),400);
			bean.setDrawWidth(drawWidth);
			bean.faceRect_ = rect;
		} else {
			bean.setDrawWidth(-1);
		}
		/*detect mode*/
		if(detectCtr_ < DetectThreshold) {
			Mat rgba = ImageProcessor.bitmap2Mat(bm);
			cacheBean_.mode_ = ModeDetector.detectMode(rgba, cv_rect);
		}
		
		Message msg = new Message();
//		long cur_time = System.currentTimeMillis();
//		msg.what = (int)(lastTime_ - cur_time);
//		lastTime_ = cur_time;
		msg.what = pre_dur;
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
