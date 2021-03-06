#include "FrameTools.h"
using namespace cv;


#ifdef __cplusplus
extern "C" {
#endif

int CalculateMeanValue(Mat & src)
{
	unsigned sum = 0;
	for (unsigned i = 0; i < src.rows; ++i)
	{
		for (unsigned j = 0; j < src.cols; ++j)
		{
			sum += src.at<uchar>(i,j);
		}
	}

	return sum/(src.rows*src.cols);
}

void Assign(Mat &output,unsigned i, unsigned h, unsigned v)
{
	output.at<uchar>(0,i) = v;
	output.at<uchar>(1,i) = h;
}

void Discard(Mat &dctImg, Mat &coor){

	int NUM = 1000;
	for (int i = NUM; i < coor.cols; i++)
	{
		float ky = coor.at<uchar>(0,i);
		float kx = coor.at<uchar>(1,i);
		dctImg.at<float>(ky,kx) = 0.0;
	}
}

void ConvertMatToAddr(Mat &dst, Mat &mat){
	mat = dst;
}

Mat nativeDCTFunction(Mat &img)
{
	Mat img2;
    int w = img.cols;
    int h = img.rows;
    int w2,h2;
    if (w % 2 == 0)
        w2 = w;
    else
        w2 = w+1;
    if (h % 2 == 0)
        h2 = h;
    else
        h2 = h+1;
    copyMakeBorder(img, img2, 0, h2-h, 0, w2-w, IPL_BORDER_REPLICATE);//BORDER_CONSTANT

    // Grayscale image is 8bits per pixel,
    // but dct() method wants float values!
    Mat img3 = Mat( img2.rows, img2.cols, CV_32FC1);
    img2.convertTo(img3, CV_32FC1);

    // Let's do the DCT now: image => frequencies
    Mat freq;
	img3.copyTo(freq);
    dct(img3, freq);

	return freq;//CV_64F
}


Mat Zigzag(Mat &src)
{
//init operations
unsigned h = 0;
unsigned v = 0;
unsigned vmin = 0;
unsigned hmin = 0;
unsigned vmax = src.rows-1;
unsigned hmax = src.cols-1;
unsigned i = 0;

Mat output = Mat::zeros(2, src.rows * src.cols,CV_8U);

	while ((v <= vmax) && (h <= hmax))
	{
		if ((h + v)% 2 == 0)
		{
			if (v == vmin)
			{
				Assign(output,i,h,v);
				if (h == hmax)	v = v + 1;
				else			h = h + 1;
				i = i + 1;
			}
			else if ((h == hmax) && (v < vmax))
			{
				Assign(output,i,h,v);
				v = v + 1;
				i = i + 1;
			}
			else if ((v > vmin) && (h < hmax))
			{
				Assign(output,i,h,v);
				v = v - 1;
				h = h + 1;
				i = i + 1;
			}
		}
		else
		{
		   if ((v == vmax) && (h <= hmax))
		   {
				Assign(output,i,h,v);
				h = h + 1;
				i = i + 1;
		   }
		   else if (h == hmin)
		   {
				Assign(output,i,h,v);
				if (v == vmax)	h = h + 1;
				else            v = v + 1;
				i = i + 1;
		   }
		   else if ((v < vmax) && (h > hmin))
		   {
				Assign(output,i,h,v);
				v = v + 1;
				h = h - 1;
				i = i + 1;
		   }
		}

		if ((v == vmax) && (h == hmax))
		{
			Assign(output,i,h,v);
			break;
		}
	}
	return output;

}

int Otsu(Mat &src)
{
    int height=src.rows;
    int width=src.cols;

    //histogram
    float histogram[256] = {0};
    for(int i=0; i < height; i++)
    {
		unsigned char* p=(unsigned char*)src.data + src.step * i;
        for(int j = 0; j < width; j++)
        {
            histogram[*p++]++;
        }
    }
    //normalize histogram
    int size = height * width;
    for(int i = 0; i < 256; i++)
    {
        histogram[i] = histogram[i] / size;
    }

    //average pixel value
    float avgValue=0;
    for(int i=0; i < 256; i++)
    {
        avgValue += i * histogram[i];  //整幅图像的平均灰度
    }

    int threshold;
    float maxVariance=0;
    float w = 0, u = 0;
    for(int i = 0; i < 256; i++)
    {
        w += histogram[i];  //假设当前灰度i为阈值, 0~i 灰度的像素(假设像素值在此范围的像素叫做前景像素) 所占整幅图像的比例
        u += i * histogram[i];  // 灰度i 之前的像素(0~i)的平均灰度值： 前景像素的平均灰度值

        float t = avgValue * w - u;
        float variance = t * t / (w * (1 - w) );
        if(variance > maxVariance)
        {
            maxVariance = variance;
            threshold = i;
        }
    }

    return threshold;
}

float getPlane(Mat &src, int flag)
{
	int aveY = 0;
	int sqrY = 0;

	for (int x = 0; x < src.rows; x++)
	{
		for (int y = 0; y < src.cols; y++)
		{
			aveY += (255-src.at<uchar>(x,y)) * (y - flag);
			sqrY += (y - src.cols) * (y - src.cols);
		}
	}

 	float beta = (aveY + 0.0) / (sqrY + 0.0);
	return beta*100;
}

int calMeanMat(Mat &src_, Mat &meanMat_)
{
	Mat cnt = Mat::zeros(SIZEWIDTH,SIZEHEIGHT,CV_32SC1);
	int width = (src_.cols%SIZEWIDTH) ? (src_.cols/SIZEWIDTH+1) : (src_.cols/SIZEWIDTH);
	int height = (src_.rows%SIZEHEIGHT) ? (src_.rows/SIZEHEIGHT+1) : (src_.rows/SIZEHEIGHT);

	for (int y = 0; y < src_.rows; y++)
	{
		for (int x = 0; x < src_.cols; x++)
		{
			meanMat_.at<int>(y/height, x/width) += src_.at<uchar>(y, x);
			cnt.at<int>(y/height, x/width) ++;
		}
	}
	int sum = 0;
	for (int y = 0; y < SIZEHEIGHT; y++)
	{
		for (int x = 0; x < SIZEWIDTH; x++)
		{
			sum += meanMat_.at<int>(y, x);
			meanMat_.at<int>(y, x) /= cnt.at<int>(y, x);
		}
	}
	int meanValue = sum / src_.rows / src_.cols;

	return meanValue;
}


/* TYPE: 0 = dilate; 1 = erosion
 *
 */
void Polymorphy(Mat &src, int TYPE)
{
	TYPE = TYPE ? BRIGHT : DARK;

	for (int i=0; i<src.rows; i++){
        for (int j=0; j<src.cols; j++){
            if (src.at<uchar>(i,j) == 255-TYPE){
                if (i>0 && src.at<uchar>(i-1,j)==TYPE) src.at<uchar>(i-1,j) = 2;
                if (j>0 && src.at<uchar>(i,j-1)==TYPE) src.at<uchar>(i,j-1) = 2;
                if (i+1<src.rows && src.at<uchar>(i+1,j)==TYPE) src.at<uchar>(i+1,j) = 2;
                if (j+1<src.cols && src.at<uchar>(i,j+1)==TYPE) src.at<uchar>(i,j+1) = 2;
            }
        }
    }
    for (int i=0; i<src.rows; i++){
        for (int j=0; j<src.cols; j++){
            if (src.at<uchar>(i,j) == 2){
                src.at<uchar>(i,j) = 255-TYPE;
            }
        }
    }
}

#ifdef __cplusplus
}
#endif
