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

int getFaceMeanValue(Mat &src){
	int cnt = 0;
	int sum = 0;
	int avg = 0;
	uchar value = 0;

	for (int i=0; i<src.rows; i++)
	{
		for (int j=0; j<src.cols; j++)
		{
			value = src.at<uchar>(i,j);
			if (value == 0)
			{
				continue;
			}else
			{
				cnt++;
				sum += value;
			}
		}
	}

	avg = sum / cnt;
	return avg;
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

Mat calMeanMat(Mat &src_, int &mean)
{
	Mat meanMat_ = Mat::zeros(SIZEWIDTH,SIZEHEIGHT,CV_32SC1);

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

	mean = sum / src_.rows / src_.cols;

	return meanMat_;
}

int* calDerivedMat(Mat &derivedMatx_, Mat &derivedMaty_, Mat &meanMat_)
{
	int sumx = 0;
	int sumy = 0;
	int *threshold = new int[2];
	for (int y = 1; y < SIZEHEIGHT; y++)
	{
		for (int x = 1; x < SIZEWIDTH; x++)
		{
			derivedMatx_.at<int>(y, x) = meanMat_.at<int>(y, x) - meanMat_.at<int>(y, x-1);
			sumx += abs(derivedMatx_.at<int>(y, x));
			derivedMaty_.at<int>(y, x) = meanMat_.at<int>(y, x) - meanMat_.at<int>(y-1, x);
			sumy += abs(derivedMaty_.at<int>(y, x));
		}
	}
	threshold[0] = sumx / SIZEWIDTH / SIZEHEIGHT;
	threshold[1] = sumy / SIZEWIDTH / SIZEHEIGHT;
	return threshold;
}

void calMaskMat(Mat &maskMatx_, Mat &maskMaty_, Mat &derivedMatx_, Mat &derivedMaty_, int threshold[])
{
	int thresholdx = threshold[0];
	int thresholdy = threshold[1];

	bool flag_bright = false;
	bool flag_dark = false;

	//horizontal:
	for (int y = 1; y < SIZEHEIGHT-1; y++)
	{
		for (int x = 1; x < SIZEWIDTH-1; x++)
		{
			int Cij = derivedMatx_.at<int>(y, x);
			int C_ij = derivedMatx_.at<int>(y, x+1);
			if (Cij > thresholdx)
			{
				if (abs(C_ij) < thresholdx)
				{
					flag_bright = true;
					maskMatx_.at<uchar>(y,x) = POTENTIAL;
				}
				else if (C_ij < -thresholdx)
				{
					maskMatx_.at<uchar>(y,x) = TODO;
				}
			}else if (Cij < -thresholdx)
			{
				if (abs(C_ij) < thresholdx)
				{
					flag_dark = true;
					maskMatx_.at<uchar>(y,x) = POTENTIAL;
				}
				else if (C_ij > thresholdx)
				{
					maskMatx_.at<uchar>(y,x) = TODO;
				}
			}else if (abs(Cij) < thresholdx)
			{
				if ((C_ij > thresholdx) && flag_dark)
				{
					for (; (x>=0)&&(maskMatx_.at<uchar>(y,x)!=POTENTIAL) ; x--)
						maskMatx_.at<uchar>(y,x) = DARK;
					maskMatx_.at<uchar>(y,x) = DARK;
					flag_dark = false;
				}else if ((C_ij < -thresholdx) && flag_bright)
				{
					for (; (x >=0)&&(maskMatx_.at<uchar>(y,x)!= POTENTIAL) ; x--)
						maskMatx_.at<uchar>(y,x) = BRIGHT;
					maskMatx_.at<uchar>(y,x) = BRIGHT;
					flag_bright = false;
				}
			}
		}

		//at the end of each line,
		if (flag_bright)
		{
			int i = SIZEWIDTH-2;
			for (; (i >0)&&(maskMatx_.at<uchar>(y,i) != POTENTIAL) ; i--)
				maskMatx_.at<uchar>(y,i) = BRIGHT;
			maskMatx_.at<uchar>(y,i) = BRIGHT;
			flag_bright = false;
		}
		if (flag_dark)
		{
			int i = SIZEWIDTH-2;
			for ( ;(i>0)&&(maskMatx_.at<uchar>(y,i) !=POTENTIAL) ; i--)
				maskMatx_.at<uchar>(y,i) = DARK;
			maskMatx_.at<uchar>(y,i) = DARK;
			flag_dark = false;
		}
	}

	//vertical:
	for (int x = 1; x < SIZEWIDTH-1; x++)
	{
		for (int y = 1; y < SIZEHEIGHT-1; y++)
		{
			int Cij = derivedMaty_.at<int>(y, x);
			int C_ij = derivedMaty_.at<int>(y+1, x);
			if (Cij > thresholdy)
			{
				if (abs(C_ij) < thresholdy)
				{
					flag_bright = true;
					maskMaty_.at<uchar>(y,x) = POTENTIAL;
				}
				else if (C_ij < -thresholdy)
				{
					maskMaty_.at<uchar>(y,x) = TODO;
				}
			}else if (Cij < -thresholdy)
			{
				if (abs(C_ij) < thresholdy)
				{
					flag_dark = true;
					maskMaty_.at<uchar>(y,x) = POTENTIAL;
				}
				else if (C_ij > thresholdy)
				{
					maskMaty_.at<uchar>(y,x) = TODO;
				}
			}else if (abs(Cij) < thresholdy)
			{
				if ((C_ij > thresholdy) && flag_dark)
				{

					for (; (y>0)&&(maskMaty_.at<uchar>(y,x)!=POTENTIAL); y--)
						maskMaty_.at<uchar>(y,x) = DARK;
					maskMaty_.at<uchar>(y,x) = DARK;
					flag_dark = false;
				}else if ((C_ij < -thresholdy) && flag_bright)
				{
					for (; (y>0)&&(maskMaty_.at<uchar>(y,x) != POTENTIAL); y--)
						maskMaty_.at<uchar>(y,x) = BRIGHT;
					maskMaty_.at<uchar>(y,x) = BRIGHT;
					flag_bright = false;
				}
			}
		}

		//at the end of each line,
		if (flag_bright)
		{
			for (int i = SIZEHEIGHT-2; (i>=0)&&(maskMaty_.at<uchar>(i,x) != POTENTIAL) ; i--)
				maskMaty_.at<uchar>(i,x) = BRIGHT;
			flag_bright = false;
		}
		if (flag_dark)
		{
			for (int i = SIZEHEIGHT-2; (i>=0)&&(maskMaty_.at<uchar>(i,x) !=POTENTIAL) ; i--)
				maskMaty_.at<uchar>(i,x) = DARK;
			flag_dark = false;
		}
	}
}


#ifdef __cplusplus
}
#endif
