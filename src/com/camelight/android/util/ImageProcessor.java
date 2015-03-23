package com.camelight.android.util;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import com.camelight.android.model.CameraFrame;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;

public class ImageProcessor {
	static public Bitmap rotate(Bitmap bitmap, int rotation){
		if(bitmap == null) {
			return null;
		}
		Matrix rot_mat = new Matrix();
		rot_mat.setRotate(rotation, bitmap.getWidth()/2, bitmap.getHeight()/2);
		Bitmap rot_bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), rot_mat, false);
		return rot_bitmap;
	}
	
	/*TODO:这个方法rotate后会有偏移，暂时不用*/
	static public Mat rotate(Mat src, int rotation) {
		if(src == null || src.cols() == 0 || src.rows() == 0) {
			return new Mat();
		}
		Point center = new Point(src.cols()/2-1, src.rows()/2-1);
		Mat rot_mat = Imgproc.getRotationMatrix2D(center, -rotation, 1.f);
		Mat res = new Mat(src.cols(), src.rows(), src.type());
		Size dsize = new Size(src.rows(), src.cols());
		Imgproc.warpAffine(src, res, rot_mat, dsize);
		return res;
	}
	
	static public byte[] rotateJPEGData(byte[] data, int rotation){
		Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
		bitmap = rotate(bitmap, rotation);
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		bitmap.compress(CompressFormat.JPEG, 100, stream);
		return stream.toByteArray();
	}
	
	static public Mat bitmap2Mat(Bitmap bm){
		if(bm == null) {
			return new Mat();
		}
		Mat rgba = new Mat();
		Utils.bitmapToMat(bm, rgba);
		return rgba;
	}
}
