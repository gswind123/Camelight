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

int getFaceMeanValue(cv::Mat &src);

int Otsu(cv::Mat &src);

float getPlane(cv::Mat &src, int flag);

cv::Mat calMeanMat(cv::Mat &src_, int &mean);

int* calDerivedMat(cv::Mat &derivedMatx_, cv::Mat &derivedMaty_, cv::Mat &meanMat_);

void calMaskMat(cv::Mat &maskMatx_, cv::Mat &maskMaty_, cv::Mat &derivedMatx_, cv::Mat &derivedMaty_, int threshold[]);

#ifdef __cplusplus
}
#endif
#endif
