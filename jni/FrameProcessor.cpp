#include <jni.h>
#include <string.h>
#include <iostream>
#include "FrameTools.h"
#include <vector>
#include <android/log.h>

using namespace std;
using namespace cv;

#ifdef __cplusplus
extern "C" {
#endif

/*
 * Class:     com_camelight_android_util_FrameProcessor
 * Method:    nativeSayHello
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_camelight_android_util_FrameProcessor_nativeSayHello
  (JNIEnv * env, jclass cls){
	return (env)->NewStringUTF("Hello from native code!");
}

/*
 * Class:     com_camelight_android_util_FrameProcessor
 * Method:    nativeEnhanceImage
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_camelight_android_util_FrameProcessor_nativeEnhanceImage
  (JNIEnv * env, jclass cls, jlong addRgba){
	Mat* mRgb = (Mat*) addRgba;

	double 	alpha 	= 2;/**< Simple contrast control */
	int		beta	= 50;/**< Simple brightness control */

	/**
	 * TODO: way to improve the speed: crop a sub image centered around the image center.
	 * */
	 for (int y = 0; y < mRgb->rows; ++y)
	 {
		for (int x = 0; x < mRgb->cols; ++x)
		{
			for (int c = 0; c < 3; ++c)
			{
				mRgb->at<Vec3b>(y,x)[c] =
				saturate_cast<uchar>( alpha*( mRgb->at<Vec3b>(y,x))[c] + beta );
			}
		}
	 }
}

/*
 * Class:     com_camelight_android_util_FrameProcessor
 * Method:    nativeAnalyzeMode
 * Signature: (JIIII)I
 */
JNIEXPORT jint JNICALL Java_com_camelight_android_util_FrameProcessor_nativeAnalyzeMode
  (JNIEnv * env, jclass cls, jlong addGray, jint x, jint y, jint width, jint height){
  	Mat mGray = *(Mat*) addGray;
  	/*something may be wrong with this line: */
  	Rect faceRect(x,y,width,height);

	int thresholdDark = 50;
	int thresholdContrast = 100;
	int thresholdBacklit = 24000;
	unsigned N = mGray.cols * mGray.rows;
	/* step-1: calculate mean value of each block */
	/*
	 ___________
	| 1 | 2 | 3 |
	|___|___|___|
	|   | f |   |
	|_4_|___|_5_|
	|___________|
	*/
	Rect surroundingRect[5];
	std::vector<Point> shift;
	shift.push_back(Point(-faceRect.x, -faceRect.y));
	shift.push_back(Point(0,-faceRect.y));
	shift.push_back(Point(faceRect.width,-faceRect.y));
	shift.push_back(Point(-faceRect.x,0));
	shift.push_back(Point(faceRect.width,0));

	surroundingRect[0].width = faceRect.x;
	surroundingRect[0].height = faceRect.y;
	surroundingRect[1].width = faceRect.width;
	surroundingRect[1].height = faceRect.y;
	surroundingRect[2].width = mGray.cols-(faceRect.x+faceRect.width);
	surroundingRect[2].height = faceRect.y;
	surroundingRect[3].width = faceRect.x;
	surroundingRect[3].height = faceRect.height;
	surroundingRect[4].width = mGray.cols-(faceRect.x+faceRect.width);
	surroundingRect[4].height = faceRect.height;


	/* calculate the faceRect mean value */
	int meanValue[6] = {0};

	Mat surroundingMat = mGray(faceRect);
	unsigned sum = 0;

	for (unsigned index = 0; index < 5; ++index)
	{
		surroundingRect[index].x = faceRect.x + shift[index].x;
		surroundingRect[index].y = faceRect.y + shift[index].y;
		surroundingMat = mGray(surroundingRect[index]);
		meanValue[index] = CalculateMeanValue(&surroundingMat);
		sum += surroundingRect[index].width*surroundingRect[index].height*meanValue[index];
	}
	Mat gray_mat = mGray(faceRect);
	meanValue[5] = CalculateMeanValue(&gray_mat);

	/* step-2: calculate mean value of the whole image: */
	sum += meanValue[5];
	int m = sum / N;
	if (m < thresholdDark)
	{
		return (jint)3;
	}

	/* step-3: calculate stardard deviation: */
	for (int i = 0; i < mGray.rows; ++i)
	{
		for (int j = 0; j < mGray.cols; ++j)
		{
			sum += (mGray.at<uchar>(i,j)-meanValue[5])^2;
		}
	}

	int s = sum / N;
	/* if low contrast: it means front-lit */
	if (s < thresholdContrast)
	{
		return (jint)1;
	}

	/* step-4: calculate deviation of each block: */
	for (int i = 0; i < 5; ++i)
	{
		sum = (meanValue[i]-meanValue[5])^2;
	}
	if (sum > thresholdBacklit)
	{
		return (jint)2;
	}

	return (jint)0;
}

/*
 * Class:     com_camelight_android_util_FrameProcessor
 * Method:    nativeCalculateLightCoordinate
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_camelight_android_util_FrameProcessor_nativeCalculateLightCoordinate
  (JNIEnv * env, jclass cls, jlong addGray){
	Mat mGray = *(Mat*) addGray;

	Mat dctImg = nativeDCTFunction(mGray);
	Mat coor = Zigzag(mGray);
	Discard(dctImg, coor);
	mGray.release();

	Mat dst;
	idct(dctImg, dst);
	dst.convertTo(dst, CV_8U);
	coor.release();

	int thd = Otsu(dst);
	threshold(dst, dst, thd, 255, CV_THRESH_BINARY);

	ConvertMatToAddr(dst, *(Mat*)addGray);
	return 1;
}

/*
 * Class:     com_camelight_android_util_FrameProcessor
 * Method:    nativeCalculateBestDistance
 * Signature: (J)F
 */
JNIEXPORT jfloat JNICALL Java_com_camelight_android_util_FrameProcessor_nativeCalculateBestDistance
  (JNIEnv *env, jclass cls, jlong addGray) {
  	Mat mGray = *(Mat*)addGray;
  	int faceMeanValue = getFaceMeanValue(mGray);

  	double a = 30.82;
  	double b = -173.39;
	double c = 253.15;
	double distance = 0.0;

	distance = (-b-sqrt(b*b-4*a*(c+faceMeanValue-128)))/(2*a);

	return (jfloat)distance;
}

/*
 * Class:     com_camelight_android_util_FrameProcessor
 * Method:    nativeDetectFace
 * Signature: (J)Lorg/opencv/core/Rect;
 */
JNIEXPORT void JNICALL Java_com_camelight_android_util_FrameProcessor_nativeDetectFace
  (JNIEnv * env, jclass cls, jlong addGray){
	return ;
}
//==============================================================================


/*
 * Class:     com_camelight_android_util_FrameProcessor
 * Method:    nativeGetIlluminationMap
 * Signature: (J)Z
 */
//JNIEXPORT jboolean JNICALL Java_com_camelight_android_util_FrameProcessor_nativeGetIlluminationMap
//  (JNIEnv *, jobject, jlong addGray)
//  {
//  	Mat mGray = *(Mat*) addGray;
//
//  	Mat dctImg = nativeDCTFunction(mGray);
//  	Mat coor = Zigzag(mGray);
//  	Discard(dctImg, coor);
//  	mGray.release();
//
//  	Mat dst;
//  	idct(dctImg, dst);
//  	dst.convertTo(dst, CV_8U);
//  	coor.release();
//
//  	int thd = Otsu(dst);
//  	threshold(dst, dst, thd, 255, CV_THRESH_BINARY);
//
//  	ConvertMatToAddr(dst, *(Mat*)addGray);
//  	return true;
//  	/*TODO: other conditions return false;
//  	*/
//  }

#ifdef __cplusplus
}
#endif
