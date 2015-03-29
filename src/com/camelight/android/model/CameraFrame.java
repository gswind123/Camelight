package com.camelight.android.model;

import java.io.ByteArrayOutputStream;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;

public class CameraFrame {
	//rawData_: ImageFormat.NV21 bytes
	private byte[] rawData_ = null;
	private Mat yuvFrameMat_ = null;
	private int width_ = 0;
	private int height_ = 0;
	private int rotation_ = 0;//The rotation of raw image
	
	public CameraFrame(byte[] raw_byte, int w, int h, int rotation) {
		rawData_ = raw_byte;
		yuvFrameMat_ = new Mat(h+h/2, w, CvType.CV_8UC1);
		yuvFrameMat_.put(0, 0, rawData_);
		width_ = w;
		height_ = h;
		rotation_ = rotation;
	}
	public byte[] getRawData(){
		return rawData_;
	}
	public byte[] getJPEGData(){
		if(rawData_ == null ){
			return null;
		}
		YuvImage img = new YuvImage(rawData_, ImageFormat.NV21, width_, height_, null);
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		img.compressToJpeg(new Rect(0,0,width_,height_), 70, stream);
		return stream.toByteArray();
	}
	public int getWidth(){
		return width_;
	}
	public int getHeight(){
		return height_;
	}
	public int getRotation(){
		return rotation_;
	}
    public Mat gray() {
    	return yuvFrameMat_.submat(0, height_, 0, width_);
    }

    public Mat rgba() {
    	Mat rgba_mat = new Mat();
        Imgproc.cvtColor(yuvFrameMat_, rgba_mat, Imgproc.COLOR_YUV2RGBA_NV21, 4);
        return rgba_mat;
    }
    
    public Bitmap bitmap(){
    	byte data[] = this.getJPEGData();
    	if(data == null) {
    		return null;
    	}
    	return BitmapFactory.decodeByteArray(data, 0, data.length);
    }
}
