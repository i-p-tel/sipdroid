LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := OSNetworkSystem
LOCAL_SRC_FILES := OSNetworkSystem.cpp

include $(BUILD_SHARED_LIBRARY)
