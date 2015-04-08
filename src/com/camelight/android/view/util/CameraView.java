package com.camelight.android.view.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.acl.LastOwnerException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import junit.framework.Test;

import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import com.camelight.android.model.CameraFrame;
import com.camelight.android.util.ImageProcessor;
import com.camelight.android.view.CameraActivity;


import android.R;
import android.view.GestureDetector.OnGestureListener;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.Area;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.media.FaceDetector.Face;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActionBar.LayoutParams;
import android.content.Context;
import android.content.res.ColorStateList;
import android.media.FaceDetector;
import android.os.Build;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Choreographer.FrameCallback;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;


public class CameraView extends SurfaceView 
	implements SurfaceHolder.Callback, PictureCallback, PreviewCallback{
	
	public static String TAG = "CameraView";
	
	public static final int CAMERA_FACE_FRONT = 99;
	public static final int CAMERA_FACE_BACK = 100;
	public static final int CAMERA_FACE_ANY = -1;
	
	protected int cameraFacing_ = -1;
	protected Activity activity_;
	protected Camera camera_ = null;
	protected int cameraIndex_ = -1;
	protected boolean connected_ = false;
	protected int viewWidth_ = 0;
	protected int viewHeight_ = 0;
	
	private CameraFrame latestFrame_;
	
	public CameraView(Context context, AttributeSet attrs) {
		super(context, attrs);
		getHolder().addCallback(this);
		activity_ = (Activity)context;
	}
	
	protected void initCamera(int width, int height) {
		initCamera(CAMERA_FACE_ANY, width, height);
	}
	
	protected void initCamera(int facing ,int width, int height){
		//release the previous camera if any
		disconnectCamera();
		
		switch(facing){
		case CAMERA_FACE_FRONT:
		case CAMERA_FACE_BACK:
		case CAMERA_FACE_ANY: 
			break;
		default:
			return;
		}
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD){
			int came_num = Camera.getNumberOfCameras();
			cameraIndex_ = -1;
			for(int came_id = 0; came_id < came_num ; came_id++) {
				CameraInfo came_info = new CameraInfo();
				Camera.getCameraInfo(came_id, came_info);
				boolean right_one = false;
				if(facing == CAMERA_FACE_FRONT && came_info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT || 
				   facing == CAMERA_FACE_BACK  && came_info.facing == Camera.CameraInfo.CAMERA_FACING_BACK  ||
				   facing == CAMERA_FACE_ANY) {
					right_one = true;
				}
				if(right_one) {
					cameraIndex_ = came_id;
					break;
				}
			}
			if(cameraIndex_ != -1) {
				try{
					camera_ = Camera.open(cameraIndex_);
				}catch(RuntimeException e) {
					Log.e(TAG, e.getMessage());
				}
			}
		} else {
			try{
				camera_ = Camera.open();
			}catch(RuntimeException e) {
				Log.e(TAG, e.getMessage());
			}
		}
		if(camera_ != null) {
			connected_ = true;
			cameraFacing_ = facing;
			camera_.setPreviewCallback(this);
			Camera.Parameters params = camera_.getParameters();     
			params.setPreviewFormat(ImageFormat.NV21);
			params.setPreviewSize(height, width);
			params.setPictureSize(height*2, width*2);
			camera_.setParameters(params);
			camera_.setDisplayOrientation(90);  
			try{
				camera_.setPreviewDisplay(getHolder());
			} catch (IOException e) {
				Log.e(TAG, e.getMessage());
			}
			camera_.startPreview();
		}
	}
	
	protected void disconnectCamera(){
		if(camera_ != null) {
			camera_.stopPreview();
			camera_.setPreviewCallback(null);
			camera_.release();
			connected_  =false;
			camera_ = null;
		}
	}

	@Override
	public void surfaceCreated(SurfaceHolder arg0) {
		
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder arg0) {
		disconnectCamera();
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		viewWidth_ = width;
		viewHeight_ = height;
		initCamera(CAMERA_FACE_FRONT ,width, height);
	}
	
	
	public void takePicture() {
		camera_.setPreviewCallback(null);
		camera_.takePicture(null, null, this);
	}

	@Override
	public void onPictureTaken(byte[] data, Camera camera) {
		camera_.setPreviewCallback(this);
		camera_.startPreview();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        String currentDateandTime = sdf.format(new Date());
        String pathName = Environment.getExternalStorageDirectory().getPath() + "/CameraExample/";
        File path = new File(pathName);
        if(!path.exists() || !path.isDirectory()) {
        	path.mkdir();
        }
        String fileName = pathName + currentDateandTime + ".jpg";
       
        //rotate the image and rite the image in a file (in jpeg format)
        int rotation = this.latestFrame_.getRotation();
        data = ImageProcessor.rotateJPEGData(data, rotation);
        try {
            FileOutputStream fos = new FileOutputStream(fileName);

            fos.write(data);
            fos.close();
            Toast.makeText(activity_, fileName + " saved", Toast.LENGTH_SHORT).show();
        } catch (java.io.IOException e) {
            Log.e("PictureDemo", "Exception in photoCallback", e);
        }
		
	}

	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {
		Size size = camera.getParameters().getPreviewSize();
		int rotation = 0;
		boolean is_mirror = false;
		if(cameraFacing_ == CAMERA_FACE_FRONT) {
			rotation = -90;
			is_mirror = true;
		} else if(cameraFacing_ == CAMERA_FACE_BACK) {
			rotation = 90;
			is_mirror = false;
		}
		CameraFrame cur_frame = new CameraFrame(data, size.width, size.height, rotation, is_mirror);
		synchronized (this) {
			latestFrame_ = cur_frame;
		}
	}
	
	/*
	 * 默认摄像机获取的CameraFrame其数据都是-90度旋转的，请自行处理
	 * */
	public CameraFrame getLatestFrame(){
		CameraFrame ret_frame = null;
		synchronized (this) {
			ret_frame = latestFrame_;
		}
		return ret_frame;
	}
	
	/*
	 * @param:	area:The area to focus at; 
	 *		   	width:width of the current camera frame; 
	 * 			height:height of the current camera frame 
	 * */
	@SuppressLint("NewApi") 
	public void setFocusAt(Rect area, int width, int height) {
		if(camera_ == null || latestFrame_ == null) {
			return ;
		}
		//Map (x,y) to coordination in [-1000, 1000]
		int mapped_x = (int)(-1000 + 2000.f*area.left/width);
		int mapped_y = (int)(-1000 + 2000.f*area.right/height);
		Rect mapped_area = new Rect(mapped_x, mapped_y, mapped_x+area.width(), mapped_y+area.height());
		ArrayList<Area> focus_areas = new ArrayList<Area>();
		focus_areas.add(new Area(mapped_area, 1000));
		Parameters param = camera_.getParameters();
		param.setMeteringAreas(focus_areas);
		param.setFocusAreas(focus_areas);
		camera_.setParameters(param);
		camera_.autoFocus(null);
	}
}
