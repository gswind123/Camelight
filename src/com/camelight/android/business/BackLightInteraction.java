package com.camelight.android.business;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.media.FaceDetector.Face;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.camelight.android.R;
import com.camelight.android.model.BackLightCacheBean;
import com.camelight.android.model.CacheBean;
import com.camelight.android.model.CameraFrame;
import com.camelight.android.util.FaceExtractor;
import com.camelight.android.util.ImageProcessor;
import com.camelight.android.util.InteractionUtil;
import com.camelight.android.view.CameraActivity;

public class BackLightInteraction extends Interaction{
	private BackLightCacheBean cacheBean_ = new BackLightCacheBean();
	
	private View mainView_ = null;
	private TextView guideText_ = null;
	private View btnTakePhoto_ = null;
	private View btnClose_ = null;
	private View faceBorder_ = null;
	
	private final int NoneFaceFrameThreshold = 5;
	private int noneFaceFrameNum_ = NoneFaceFrameThreshold;
	
	private OnClickListener onCloseClickListener_ = new OnClickListener() {
		@Override
		public void onClick(View arg0) {
			if(InteractionUtil.isDoubleClick()) {
				return ;
			}
			BackLightCacheBean bean = checkParam(cacheBean_);
			if(bean == null) {
				return ;
			}
			CameraActivity activity = (CameraActivity)cacheBean_.context_;
			activity.stopCurrentInteraction();
		}
	};
	private OnClickListener onTakePhotoClickListener_ = new OnClickListener() {
		@Override
		public void onClick(View arg0) {
			if(InteractionUtil.isDoubleClick()) {
				return ;
			}
			BackLightCacheBean bean = checkParam(cacheBean_);
			if(bean == null) {
				return ;
			}
			bean.camera_.takePicture();
		}
	};
	
	@Override
	public boolean onInteractStart(CacheBean param) {
		cacheBean_ = checkParam(param);
		if(cacheBean_ == null) {
			return false;
		}
		LayoutInflater inflater = (LayoutInflater)cacheBean_.context_.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mainView_ = inflater.inflate(R.layout.back_light_interact_layout, null);
		guideText_ = (TextView)mainView_.findViewById(R.id.guide_text);
		btnTakePhoto_ = mainView_.findViewById(R.id.btn_take_photo);
		btnClose_ = mainView_.findViewById(R.id.btn_close);
		faceBorder_ = mainView_.findViewById(R.id.face_rect_border);
		btnTakePhoto_.setOnClickListener(onTakePhotoClickListener_);
		btnClose_.setOnClickListener(onCloseClickListener_);
		
		cacheBean_.layout_.addView(mainView_);
		return true;
	}

	@Override
	public InteractState onInteracting(CacheBean param) {
		cacheBean_ = checkParam(param);
		if(cacheBean_ == null) {
			return InteractState.STOP;
		}
		CameraFrame cur_frame = cacheBean_.camera_.getLatestFrame();
		byte data[] = cur_frame.getJPEGData();
		Bitmap bm = BitmapFactory.decodeByteArray(data, 0, data.length);
		bm = ImageProcessor.rotate(bm, cur_frame.getRotation());
		FaceExtractor face_detector = new FaceExtractor(bm);
		boolean has_face = face_detector.detectFaces();
		if(has_face) {
			guideText_.setText(cacheBean_.context_.getResources().getString(R.string.desc_auto_focus_on_face));
			Face faces[] = face_detector.getFaces();
			org.opencv.core.Rect face_rect = face_detector.getFaceRect(faces[0]);
			if(cur_frame.isMirror()) {
				face_rect.x = bm.getWidth() - face_rect.x - face_rect.width;
			}
			cacheBean_.camera_.setFocusAt(
					new Rect(face_rect.x, face_rect.y, face_rect.x+face_rect.width, face_rect.y+face_rect.height), 
					bm.getWidth(), bm.getHeight());
			FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams)faceBorder_.getLayoutParams();
			lp.width = face_rect.width;
			lp.height = face_rect.height;
			lp.setMargins(face_rect.x, face_rect.y, 0, 0);
			faceBorder_.setLayoutParams(lp);
			faceBorder_.setVisibility(View.VISIBLE);
			noneFaceFrameNum_ = 0;
		} else {
			noneFaceFrameNum_++;
			if(noneFaceFrameNum_ >= NoneFaceFrameThreshold) {
				guideText_.setText(cacheBean_.context_.getResources().getString(R.string.desc_search_face));
				faceBorder_.setVisibility(View.GONE);			
			}
		}
		return InteractState.CONTINUE;
	}

	@Override
	public void onInteractFinish(CacheBean param) {
		cacheBean_ = checkParam(param);
		if(cacheBean_ == null) {
			return ;
		}
		cacheBean_.layout_.removeView(mainView_);
		CameraActivity activity = (CameraActivity)cacheBean_.context_;
		Message msg = new Message();
		msg.what = BusinessState.BACK_LIGHT_GUIDE_CLOSE;
		activity.getBusinessHandler().sendMessage(msg);
	}
	
	private BackLightCacheBean checkParam(CacheBean param) {
		if(param != null && param instanceof BackLightCacheBean) {
			BackLightCacheBean bean = (BackLightCacheBean) param;
			if(bean.context_ != null && bean.camera_ != null && bean.layout_ != null) {
				return bean;
			}
		}
		return null;
	}

}
