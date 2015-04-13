package com.camelight.android.util;

import java.io.File;
import java.io.IOException;

import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;

import org.opencv.core.Rect;

import android.graphics.Bitmap;

import com.camelight.android.business.BusinessMode;

public class FrameProcessor {
// Variables:
	private static svm_model model_ = null;
	private static double dataTable_[] = { 33.39, 118.51, 33.39, 125.06, 33.39,
			128.60, 34.77, -22.21, 34.77, -12.34, 34.77, 3.26, 34.77, 17.23,
			34.77, 25.12, 31.43, -119.74, 31.43, -115.66, 31.43, -111.61,
			31.43, -100.09, 31.43, -97.23, 31.43, -96.55, 31.43, -93.18, 31.43,
			-87.01, 31.43, -84.21, 31.43, -81.59, 31.43, -79.14, 31.43, -76.87,
			31.43, -74.74, 31.43, -72.62, 31.43, -67.90, 31.43, -65.28, 31.43,
			-62.48, 31.43, -59.49, 31.43, -56.31, 31.69, -53.49, 31.69, -46.69,
			31.69, -42.84, 31.69, -38.87, 31.69, -34.83, 31.69, 39.60, 31.69,
			43.31, 31.69, 47.16, 31.69, 55.17, 31.69, 59.26, 31.69, 63.34,
			31.69, 67.10, 31.69, 74.20, 31.69, 77.91, 31.69, 81.45, 31.69,
			84.82, 32.37, 96.41, 32.37, 98.86, 32.37, 101.13, 32.37, 103.26,
			32.37, 105.38, };
	
	static public int PREDICT_WIDTH = 160;
	static public int PREDICT_HEIGHT = 80;
// Methods:
	/* constructor with a test native function */

	static public void setSVMModel(String model_file_name){
		try {
			model_ = svm.svm_load_model(model_file_name);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/* enhance face image for better face recognition */
	static public void EnhanceImage(long add) {
		nativeEnhanceImage(add);
	}
	
	/* 
	 * to determine between (3 or more ) different modes 
	 * @return 
	 * */
	static public BusinessMode AnalyzeMode(long add, Rect faceRect) {
		int mode = nativeAnalyzeMode(add, faceRect.x, faceRect.y , faceRect.width, faceRect.height);
		switch (mode) {
		case 1:
			return BusinessMode.FRONTLIGHT;
		case 2:
			return BusinessMode.BACKLIGHT;
		case 3:
			return BusinessMode.NIGHT;
		default:
			return BusinessMode.NULL;
		}
	}

	/* determine the light/sun position */
	static public void GetIlluminationMap(long add) {
		boolean result = nativeGetIlluminationMap(add);
		if (result) {
			//TODO: if false;
		}
	}

	/**
	 * @return current rectangle to draw;
	 */
	static public int CalculateBestDistance(long add, int size, int ISO) {
		int width = nativeCalculateBestDistance(add, size, ISO);
		return width;
	}
	
	/**
	 * @Warning faceBitmap must be a bitmap with PREDICT_WIDTH and PREDICT_HEIGHT
	 * */
    static public double Predict(Bitmap faceBitmap){
    	if(model_ == null) {
    		return 0.f;
    	}
    	int width = PREDICT_WIDTH;
    	int height = PREDICT_HEIGHT;
       	int fnum=48;
       	int []values=new int[fnum];
       	for (int i=0;i<width;i++)
       	{
       		for(int j=0;j<height;j++)
       		{
       			int t1=faceBitmap.getPixel(i, j);       			
    			int t2 = (t1 & 0x00FFFFFF);
    			int t3=i/40;
    			if(t2==0)values[t3]++;
        	}
        }
       	//构建测试集，为test[];    
        svm_node []test=new svm_node[fnum];
        for(int i=0;i<fnum;i++)
        {
         	test[i]=new svm_node();
         	test[i].index=i;
         	test[i].value=values[i];
        }
        
        //返回预测值;
        return svm.svm_predict(model_, test);
    }
    
    public static int[] getPredictData(int index){
    	if((index*2+1)>=dataTable_.length) {
    		return null;
    	}
    	int res[] = new int[2];
    	res[0] = (int)dataTable_[index*2];
    	res[1] = (int)dataTable_[index*2+1];
    	return res;
    }


	/* native function declaration: */
	native public static String nativeSayHello();
	native public static void nativeEnhanceImage(long add);
	
	/**
	 * @return 1=Front-lit; 2=Back-lit; 3=Night Scene; 0=others
	 * */
	native public static int nativeAnalyzeMode(long add, int left, int top, int right, int bottom);
	
	/**
	 * @param add pointing to face map;
	 * @return illumination map pointed by add.
	 */
	native public static boolean nativeGetIlluminationMap(long add);
	
	/**
	 * @param add pointing to face map;
	 * @return current square width to draw
	 */
	native public static int nativeCalculateBestDistance(long add, int size, int ISO);
	
}
