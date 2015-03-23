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
 * @return: default: front-lit=1; back-lit=2; night-scene=3;
 * Signature: (JIIII)I
 */
JNIEXPORT jint JNICALL Java_com_camelight_android_util_FrameProcessor_nativeAnalyzeMode(
		JNIEnv * env, jclass cls, jlong addGray, jint x, jint y, jint width,
		jint height) {
	Mat mGray = *(Mat*) addGray;
	/*something may be wrong with this line: */
	Rect faceRect(x, y, width, height);

	/* if background mean value < dark threshold, then it's dark mode */
	int thresholdDark = 50;
	/* if block deviation > backlit threshold, then it's backlit mode */
	int thresholdBacklit = 130;

	/* step-1: calculate mean value of each block */
	/*
	 ___________
	 | 1 | 2 | 3 |
	 |___|___|___|
	 |   | f |   |
	 |_4_|___|_5_|
	 |___________|
	 */

	std::vector<Point> shift;
	shift.push_back(Point(-faceRect.width, -faceRect.height));
	shift.push_back(Point(0, -faceRect.height));
	shift.push_back(Point(faceRect.width, -faceRect.height));
	shift.push_back(Point(-faceRect.width, 0));
	shift.push_back(Point(faceRect.width, 0));

	/* calculate the faceRect mean value */
	Rect surroundingRect;
	Mat surroundingMat;
	Rect mask = Rect(0, 0, mGray.cols, mGray.rows); //in case out of border;
	int meanValue[6] = { 0 };

	for (unsigned index = 0; index < 5; ++index) {
		surroundingRect.x = faceRect.x + shift[index].x;
		surroundingRect.y = faceRect.y + shift[index].y;
		surroundingRect.width = faceRect.width;
		surroundingRect.height = faceRect.height;
		surroundingRect &= mask;
		surroundingMat = mGray(surroundingRect);
		meanValue[index] = CalculateMeanValue(surroundingMat);
	}
	Mat tmp_mat = mGray(faceRect);
	meanValue[5] = CalculateMeanValue(tmp_mat);

	/* step-1: calculate deviation of each block: */
	unsigned blockDeviation = 0;
	for (int i = 0; i < 5; ++i) {
		blockDeviation = (meanValue[i] - meanValue[5]) ^ 2;
	}

	/* step-2: calculate mean value of the whole above chin image: */
	unsigned sum = 0;
	for (int i = 0; i < faceRect.y + faceRect.height; i++) {
		for (int j = 0; j < mGray.cols; j++) {
			sum += mGray.at<uchar>(i, j);
		}
	}
	//analyze the background, without the face rect;
	sum -= meanValue[5] * faceRect.width * faceRect.height;
	unsigned bgdAvg = sum / (mGray.cols * (faceRect.y + faceRect.height));

	if (blockDeviation > thresholdBacklit && bgdAvg > thresholdDark) {
//		return (jint)2;
	}

	if (blockDeviation < thresholdBacklit && bgdAvg < thresholdDark) {
		return (jint)3;
	}

	return (jint)1;
}

/*
 * Class:     com_camelight_android_util_FrameProcessor
 * Method:    nativeCalculateLightCoordinate
 * Signature: (J)I
 */
JNIEXPORT jboolean JNICALL Java_com_camelight_android_util_FrameProcessor_nativeGetIlluminationMap
  (JNIEnv * env, jclass cls, jlong addGray){
	Mat mGray = *(Mat*) addGray;
	if(mGray.empty() || mGray.data == NULL)
		return false;
	vector<Mat> planes;
	split(mGray,planes);

	Mat dctImg = nativeDCTFunction(planes[0]);
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
	return true;
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


#ifdef __cplusplus
}
#endif
