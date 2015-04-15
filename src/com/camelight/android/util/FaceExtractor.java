package com.camelight.android.util;

import java.util.ArrayList;

import org.opencv.core.Core;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.media.FaceDetector;
import android.media.FaceDetector.Face;
import android.util.Log;

/**
 * @author LiXia
 * 1. Boolean detectFaces(Bitmap);
 * 2. void drawFaces();
 * 3. Rect getFaceRect();
 */

public class FaceExtractor {
	private int 	maxFaces = 20;
	private Bitmap 	src_ = null;
	private FaceDetector.Face[] faces_ = null;
	private org.opencv.core.Rect faceRect = new org.opencv.core.Rect();
	
	public FaceExtractor(Bitmap src) {
		super();
		this.src_ = src.copy(Bitmap.Config.RGB_565, true);
	}

	public Boolean detectFaces() {
		this.faces_ = new FaceDetector.Face[maxFaces];
		FaceDetector faceDetector = new FaceDetector(src_.getWidth(), src_.getHeight(), maxFaces);
		//通过调用FaceDetector 的findFaces方法，我们可以找到src中的人脸数据，并存储在faces 数组里。
		faceDetector.findFaces(src_, faces_);
		ArrayList<Face> face_ary = new ArrayList<FaceDetector.Face>();
		for(Face f:faces_) {
			if(f != null && f.eyesDistance() > 0.1){
				face_ary.add(f);
			}
		}
		if(face_ary.size() > 0) {
			this.faces_ = new Face[face_ary.size()];
			int i = 0;
			for(Face f:face_ary) {
				this.faces_[i++] = f;
			}
		} else {
			this.faces_ = null;
		}
		if (this.faces_ == null || this.faces_.length == 0) {
			Log.d("FaceExtractor", "no face detected!");
			return false;
		} else {
			return true;
		}
	}
	
	public void drawFaces() {
		 PointF midPoint = new PointF();//人脸中心点
		 
		 for (int i = 0; i < faces_.length; i++)
         {
             Face f = faces_[i];
             float dis = f.eyesDistance();
             f.getMidPoint(midPoint);
             int dd = (int) (dis);
             Point eyeLeft = new Point((int) (midPoint.x - dis / 2), (int) midPoint.y);
             Point eyeRight = new Point((int) (midPoint.x + dis / 2), (int) midPoint.y);
             Rect faceRect = new Rect((int) (midPoint.x - dd), (int) (midPoint.y - dd),
                     (int) (midPoint.x + dd), (int) (midPoint.y + dd));
             
             Canvas canvas = new Canvas(this.src_);
             
             Paint p = new Paint();
             p.setAntiAlias(true);
             p.setStrokeWidth(8);
             p.setStyle(Paint.Style.STROKE);
             p.setColor(Color.GREEN);
             
             canvas.drawCircle(eyeLeft.x, eyeLeft.y, 20, p);
             canvas.drawCircle(eyeRight.x, eyeRight.y, 20, p);
             canvas.drawRect(faceRect, p);
         }
	}
	
	
	public org.opencv.core.Rect getFaceRect(Face f) {
		if (faces_.length == 0) {
			return null;
		}
		PointF midPoint = new PointF();//人脸中心点
		float a = f.eyesDistance()/2;
		f.getMidPoint(midPoint);
		faceRect.x = Math.max(0, (int) (midPoint.x - 2.5*a));
		faceRect.y = Math.max(0, (int) (midPoint.y - 2.5*a));
		faceRect.width = Math.min((int) (5*a), src_.getWidth()-faceRect.x);
		faceRect.height = Math.min((int) (6*a), src_.getHeight()-faceRect.y);
		
		return faceRect;
	}
	
	public org.opencv.core.Rect getFaceLowRect(Face f){
		if (faces_.length == 0) {
			return null;
		}
		PointF midPoint = new PointF();//人脸中心点
		float a = f.eyesDistance()/2;
		f.getMidPoint(midPoint);
		faceRect.x = Math.max(0, (int) (midPoint.x - 2.5*a));
		faceRect.y = Math.max(0, (int) (midPoint.y + 0.5*a));
		faceRect.width = Math.min((int) (5*a), src_.getWidth()-faceRect.x);
		faceRect.height = Math.min((int) (3*a), src_.getHeight()-faceRect.y);
		
		return faceRect;
	}
	
	/*
	 * @ Desc:
	 * 	get a validate face rect in the src_ bitmap,the face rect will 
	 * 	totally be in the bitmap	
	 * @ param: a face in the src_ bitmap
	 * @ return:a validate rect of the face
	 * */
//	public Rect getFaceRect(Face f){
//		PointF midPoint = new PointF();
//		float dis = f.eyesDistance();
//		f.getMidPoint(midPoint);
//		int dd = (int) (dis);
//		int left = Math.max(0, (int)(midPoint.x - dd));
//		int right = Math.min(this.src_.getWidth(), (int)(midPoint.x + dd));
//		int top = Math.max(0, (int)(midPoint.y - dd));
//		int bottom = Math.min(this.src_.getWidth(), (int)(midPoint.y + dd));
//		Rect faceRect = new Rect(left,top,right,bottom);
//		return faceRect;
//	}
	
	public Face[] getFaces(){
		return faces_;
	}
	
	public Bitmap getFaceImg() {
		return this.src_;
	}

	native static public void nativeDetectFaces(long add, org.opencv.core.Rect facerRect);
}
