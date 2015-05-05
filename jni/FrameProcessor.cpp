#include <jni.h>
#include <string.h>
#include <iostream>
#include "FrameTools.h"
#include <vector>
#include <android/log.h>
#include <opencv2/contrib/detection_based_tracker.hpp>
using namespace std;
using namespace cv;
#define  LOG_TAG    "FrameProcessor"
#define  LOGE(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)

#ifdef __cplusplus
extern "C" {
#endif

/*
 * Class:     com_camelight_android_util_FrameProcessor
 * Method:    nativeSayHello
 * Signature: ()Ljava/lang/String;
 */JNIEXPORT jstring JNICALL Java_com_camelight_android_util_FrameProcessor_nativeSayHello(
		JNIEnv * env, jclass cls) {
	return (env)->NewStringUTF("Hello from native code!");
}

/*
 * Class:     com_camelight_android_util_FrameProcessor
 * Method:    nativeEnhanceImage
 * Signature: (J)V
 */JNIEXPORT void JNICALL Java_com_camelight_android_util_FrameProcessor_nativeEnhanceImage(
		JNIEnv * env, jclass cls, jlong addRgba) {
	Mat* mRgb = (Mat*) addRgba;

	double alpha = 2;/**< Simple contrast control */
	int beta = 50;/**< Simple brightness control */

	/**
	 * TODO: way to improve the speed: crop a sub image centered around the image center.
	 * */
	for (int y = 0; y < mRgb->rows; ++y) {
		for (int x = 0; x < mRgb->cols; ++x) {
			for (int c = 0; c < 3; ++c) {
				mRgb->at<Vec3b>(y, x)[c] = saturate_cast<uchar>(
						alpha * (mRgb->at<Vec3b>(y, x))[c] + beta);
			}
		}
	}
}

/*
 * Class:     com_camelight_android_util_FrameProcessor
 * Method:    nativeAnalyzeMode
 * @return: default: front-lit=1; back-lit=2; night-scene=3;
 * Signature: (JIIII)I
 */JNIEXPORT jint JNICALL Java_com_camelight_android_util_FrameProcessor_nativeAnalyzeMode(
		JNIEnv * env, jclass cls, jlong addGray, jint x, jint y, jint width,
		jint height) {
	Mat mGray = *(Mat*) addGray;
	/*something may be wrong with this line: */
	//Rect faceRect(x, y, width, height);
	int CONTRAST_LOW = 60; // below this value means front/normal light.
	int CONTRAST_HIGH = 100; //above this value means back light.
	int DARKTHRESHOLD_LOW = 80; //below this value means dark.
	int DARKTHRESHOLD_HIGH = 100; //below this value means dark.
	int NIGHTTHRESHOLD = 80;
	int BRIGHTTHRESHOLD = 200;

	Mat meanMat_ = Mat::zeros(SIZEWIDTH, SIZEHEIGHT, CV_32SC1);
	int mean = calMeanMat(mGray, meanMat_);
	LOGE("mean:%i;", mean);
	if (mean < NIGHTTHRESHOLD) {
		return 3;
	} else if (mean > BRIGHTTHRESHOLD) {
		return 1;
	}

	Mat maskMat_ = Mat::zeros(SIZEWIDTH, SIZEHEIGHT, CV_8UC1);

	//TODO: meanMat_(rectangle).
	for (int y = 0; y < SIZEHEIGHT; y++) {
		for (int x = 0; x < SIZEWIDTH; x++) {
			if (meanMat_.at<int>(y, x) < (mean * 2 / 3)) {
				maskMat_.at<uchar>(y, x) = DARK;
			} else {
				maskMat_.at<uchar>(y, x) = BRIGHT;
			}
		}
	}

	Polymorphy(maskMat_, 0);
	Polymorphy(maskMat_, 1);

	int nSubject = 0;
	int nSurrounding = 0;
	int sumSubject = 0;
	int sumSurrounding = 0;
	int disSubject = 0;
	int disSurrounding = 0;
	for (int y = 0; y < SIZEHEIGHT; y++) {
		for (int x = 0; x < SIZEWIDTH; x++) {
			if (maskMat_.at<uchar>(y, x) == DARK) {
				nSubject++;
				disSubject += sqrt(
						(y - SIZEHEIGHT / 2) * (y - SIZEHEIGHT / 2)
								+ (x - SIZEWIDTH / 2) * (x - SIZEWIDTH / 2));
				sumSubject += meanMat_.at<int>(y, x);
			} else {
				nSurrounding++;
				disSurrounding += sqrt(
						(y - SIZEHEIGHT / 2) * (y - SIZEHEIGHT / 2)
								+ (x - SIZEWIDTH / 2) * (x - SIZEWIDTH / 2));
				sumSurrounding += meanMat_.at<int>(y, x);
			}
		}
	}
	if (nSubject == 0 || nSurrounding == 0) {
		return 1;
	}

	int avgSubject = sumSubject / nSubject;
	int avgSurrounding = sumSurrounding / nSurrounding;
	int contrast = avgSurrounding - avgSubject; // this value is always positive.

	LOGE("avgSubject:%i;   avgSurrounding:%i;   contrast:%i", avgSubject,
			avgSurrounding, contrast);

	if (contrast < CONTRAST_LOW) {
		return 1; // front / normal light
	}

	// in the dark scene, subject is the "surrounding". Hence they need exchanging.
	LOGE("nSubject:%i;nSurrounding:%i", nSubject, nSurrounding);
	if (disSubject / nSubject > disSurrounding / nSurrounding) {
		int temp = avgSurrounding;
		avgSurrounding = avgSubject;
		avgSubject = temp;
	}

	if (avgSurrounding < DARKTHRESHOLD_LOW) {
		return 3; //night scene
	}

	int Cl = 80;
	int Ch = 200;
	float wSubject = 0;
	if (contrast < Cl) {
		wSubject = 0.5f;
	} else if (contrast > Ch) {
		wSubject = 1.0f;
	} else {
		wSubject = 0.5f / (Ch - Cl) * (contrast - Cl);
	}

	float Bl = ((wSubject * avgSubject) + (1 - wSubject) * avgSurrounding)
			/ ((wSubject * nSubject) + (1 - wSubject) * nSurrounding);
	LOGE("Bl:%f;", Bl);
	if (Bl >= 0.5) {
		return 2;
	}

	if ((avgSubject < DARKTHRESHOLD_HIGH) && contrast > CONTRAST_HIGH) {
		return 2;
	}
	return 1;
}

/*
 * Class:     com_camelight_android_util_FrameProcessor
 * Method:    nativeCalculateLightCoordinate
 * Signature: (J)I
 */JNIEXPORT jfloat JNICALL Java_com_camelight_android_util_FrameProcessor_nativeGetLightDegree(
		JNIEnv * env, jclass cls, jlong addGray) {
	Mat mGray = *(Mat*) addGray;
	if (mGray.empty() || mGray.data == NULL)
		return false;
	vector<Mat> planes;
	split(mGray, planes);

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

	/* erosion & dialation: */
	Mat myModel = getStructuringElement(CV_SHAPE_ELLIPSE, Size(11, 11),
			Point(-1, -1));
	dilate(dst, dst, myModel);

	unsigned nLeft = 0;
	unsigned nRight = 0;
	for (int x = 0; x < dst.rows; x++) {
		for (int y1 = 0, y2 = dst.cols / 2; y1 < dst.cols / 2, y2 < dst.cols;
				y1++, y2++) {
			if (dst.at<uchar>(x, y1) == 0) {
				nLeft++;
			}
			if (dst.at<uchar>(x, y2) == 0) {
				nRight++;
			}
		}
	}

	int diff = int(nLeft - nRight);
	if (abs(diff) < dst.rows * dst.cols / 8) {
		return getPlane(dst, dst.cols / 2);
	} else if (diff > 0) {
		return getPlane(dst, 0);
	} else if (diff < 0) {
		return getPlane(dst, dst.cols);
	} else {
		return 0;
	}
}

/*
 * Class:     com_camelight_android_util_FrameProcessor
 * Method:    nativeCalculateBestDistance
 * Signature: (J)F
 */JNIEXPORT jint JNICALL Java_com_camelight_android_util_FrameProcessor_nativeCalculateBestDistance(
		JNIEnv *env, jclass cls, jint faceMeanValue, jint size, jint ISO) {
	 /**
	  * experiment shows: faceMeanValue > 60
	  */
	int slot[] = {25, 29, 35, 47, 77};
		 /*ratio: 50, 40, 30, 20, 10, 0*/
	float middleGray = 128.0;
	int width = 0;
	int Fd = middleGray - faceMeanValue;
	if (Fd <= 0) {
		return 0;
	}else if(Fd < slot[0]){
		Fd = slot[0];
	}else if (Fd < slot[1]){
		Fd = slot[1];
	}else if (Fd < slot[2]){
		Fd = slot[2];
	}else if (Fd < slot[3]){
		Fd = slot[3];
	}else if (Fd < slot[4]){
		Fd = slot[4];
	}else{
		return 0;
	}

	/* provide three ISO options: */
	if (ISO == 200) {
		width = 0;
	} else if (ISO == 400) {
		float e = log10(386 / Fd) / 0.7;
		int ratio = pow(10, e);
		width = sqrt(size / ratio);
//		width = faceMeanValue;
	} else if (ISO == 800) { //not available currently;
		width = 0;
	}

	return Fd;
}

/*
 * Class:     com_camelight_android_util_FrameProcessor
 * Method:		nativeGetMeanValue
 * Signature: (J)F
 */JNIEXPORT jint JNICALL Java_com_camelight_android_util_FrameProcessor_nativeGetMeanValue(
		JNIEnv *env, jclass cls, jlong addGray) {
	int cnt = 0;
	int sum = 0;
	int avg = 0;
	uchar value = 0;
	Mat src = *(Mat*) addGray;

	for (int i = 0; i < src.rows; i++) {
		for (int j = 0; j < src.cols; j++) {
			value = src.at<uchar>(i, j);
			if (value == 0) {
				continue;
			} else {
				cnt++;
				sum += value;
			}
		}
	}

	avg = sum / cnt;
	return avg;
}

/*
 * Class:     com_camelight_android_util_FrameProcessor
 * Method:    nativeDetectFace
 * Signature: (J)Lorg/opencv/core/Rect;
 */JNIEXPORT void JNICALL Java_com_camelight_android_util_FaceExtractor_nativeDetectFaces(
		JNIEnv * jenv, jclass, jlong thiz, jlong imageGray, jlong faces) {
}

#ifdef __cplusplus
}
#endif
