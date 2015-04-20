#ifndef FRAMETOOLS_H
#define FRAMETOOLS_H
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/features2d/features2d.hpp>
#include <opencv2/objdetect/objdetect.hpp>
#include <opencv2/highgui/highgui.hpp>
#ifdef __cplusplus
extern "C" {
#endif

int CalculateMeanValue(cv::Mat & mat);

void Assign(cv::Mat &output,unsigned i, unsigned h, unsigned v);

void Discard(cv::Mat &dctImg, cv::Mat &coor);

void ConvertMatToAddr(cv::Mat &dst, cv::Mat &mat);

cv::Mat nativeDCTFunction(cv::Mat &img);

cv::Mat Zigzag(cv::Mat &src);

int getFaceMeanValue(cv::Mat &src);

int Otsu(cv::Mat &src);

float getPlane(cv::Mat &src, int flag);

#ifdef __cplusplus
}
#endif
#endif
