LOCAL_PATH := $(call my-dir)    
include $(CLEAR_VARS)    
OPENCV_LIB_TYPE:=STATIC
#OPENCV_INSTALL_MODULES:=on 
ifeq ("$(wildcard $(OPENCV_MK_PATH))","")    
#try to load OpenCV.mk from default install location    
include H:\Android-dev\Oencv4Android\OpenCV-2.4.10-android-sdk\sdk\native\jni\OpenCV.mk

else    
include $(OPENCV_MK_PATH)    
endif    
LOCAL_MODULE    := frame_processor
LOCAL_SRC_FILES := FrameProcessor.cpp FrameTools.cpp
include $(BUILD_SHARED_LIBRARY) 

include $(CLEAR_VARS)
include $(CLEAR_VARS)
LOCAL_SRC_FILES := libopencv_java.so
LOCAL_MODULE    := opencv_java
include $(PREBUILT_SHARED_LIBRARY)

