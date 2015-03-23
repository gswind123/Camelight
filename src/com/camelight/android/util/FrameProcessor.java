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
	private static int dataTable_[]= {
			0,		0,
			+0,		+00,
			+0,		+20,
			+0,		+45,
			+0,		+90,
			+0,		-20,
			+0,		-35,
			+5,		+10,
			+5,		-10,
			+10,	+00,
			+10,	-20,
			+15,	+20,
			+20,	+10,
			+20,	-10,
			+20,	-40,
			+25,	+00,
			+35,	+15,
			+35,	+40,
			+35,	+65,
			+35,	-20,
			+50,	+00,
			+50,	-40,
			+60,	+20,
			+60,	-20,
			+70,	+00,
			+70,	+45,
			+70,	-35,
			+85,	+20,
			+85,	-20,
			+95,	+00,
			+110,	+15,
			+110,	+40,
			+110,	+65,
			+110,	-20,
			+120,	+00,
			+130,	+20,
			-5	,	+10,
			-5	,	-10,
			-10	,	+00,
			-10	,	-20,
			-15	,	+20,
			-20	,	+10,
			-20	,	-10,
			-20	,	-40,
			-25	,	+00,
			-35	,	+15,
			-35	,	+40,
			-35	,	+65,
			-35	,	-20,
			-50	,	+00,
			-50	,	-40,
			-60	,	+20,
			-60	,	-20,
			-70	,	+00,
			-70	,	+45,
			-70	,	-35,
			-85	,	+20,
			-85	,	-20,
			-95	,	+00,
			-110	,	+15,
			-110	,	+40,
			-110	,	+65,
			-110	,	-20,
			-120	,	+00,
			-130	,	+20,
			};
	
	static public int PREDICT_WIDTH = 160;
	static public int PREDICT_HEIGHT = 160;
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

	/* determine the best distance between subject and operator. */
	static public float CalculateBestDistance(long add) {
		float bestDistance = nativeCalculateBestDistance(add);
		return bestDistance;
	}
	
	/*
	 * @Warning faceBitmap must be a bitmap with PREDICT_WIDTH and PREDICT_HEIGHT
	 * */
    static public double Predict(Bitmap faceBitmap){
    	if(model_ == null) {
    		return 0.f;
    	}
    	int width = PREDICT_WIDTH;
    	int height = PREDICT_HEIGHT;
    	int w1= width/20;
       	int h1= height/20;
       	int fnum=w1*h1;
       	int []values=new int[fnum];
       	for (int i=0;i<width;i++)
       	{
       		for(int j=0;j<height;j++)
       		{
       			int t1=faceBitmap.getPixel(i, j);       			
    			int t2 = (t1 & 0x000000FF);
    			int t3=i/20;
    			int t4=j/20;
    			if(t2>0)values[t3*h1+t4]++;
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
    	res[0] = dataTable_[index*2];
    	res[1] = dataTable_[index*2+1];
    	return res;
    }

//	static public Rect DetectFace(long add) {
//		Rect faceRect = instance_.nativeDetectFace(add);
//		return faceRect;
//	}

	/* native function declaration: */
	native public static String nativeSayHello();
	native public static void nativeEnhanceImage(long add);
	/*
	 * @return 1=Front-lit; 2=Back-lit; 3=Night Scene; 0=others
	 * */
	native public static int nativeAnalyzeMode(long add, int left, int top, int right, int bottom);
	native public static boolean nativeGetIlluminationMap(long add);
	native public static float nativeCalculateBestDistance(long add);
	native public static void nativeDetectFace(long add);
	
}
