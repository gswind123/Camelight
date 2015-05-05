#ifndef FRAMETOOLS_H
#define FRAMETOOLS_H
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/features2d/features2d.hpp>
#include <opencv2/objdetect/objdetect.hpp>
#include <opencv2/highgui/highgui.hpp>
#include <android/log.h>
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "FrameProcessor", __VA_ARGS__)
#ifdef __cplusplus
extern "C" {
#endif

#define SIZEWIDTH 20
#define SIZEHEIGHT 20
#define DARK 255
#define BRIGHT 0
#define POTENTIAL 128
#define TODO 0
#define MARGIN 2


int CalculateMeanValue(cv::Mat & mat);

void Assign(cv::Mat &output,unsigned i, unsigned h, unsigned v);

void Discard(cv::Mat &dctImg, cv::Mat &coor);

void ConvertMatToAddr(cv::Mat &dst, cv::Mat &mat);

cv::Mat nativeDCTFunction(cv::Mat &img);

cv::Mat Zigzag(cv::Mat &src);

int Otsu(cv::Mat &src);

float getPlane(cv::Mat &src, int flag);

int calMeanMat(cv::Mat &src_, cv::Mat &);

void Polymorphy(cv::Mat &src, int TYPE);

#ifdef __cplusplus
}
#endif
#endif
