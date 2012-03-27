LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
	system_init.cpp

base = $(LOCAL_PATH)/../../..

LOCAL_C_INCLUDES := \
	$(base)/services/camera/libcameraservice \
	$(base)/services/audioflinger \
	$(base)/services/surfaceflinger \
	$(base)/services/sensorservice \
	$(base)/media/libmediaplayerservice \
	$(JNI_H_INCLUDE)

ifeq ($(BOARD_USES_QCOM_HARDWARE),true)
LOCAL_C_INCLUDES +=  hardware/qcom/display/libqcomui
LOCAL_CFLAGS += -DQCOM_HARDWARE
endif

LOCAL_SHARED_LIBRARIES := \
	libandroid_runtime \
	libsensorservice \
	libsurfaceflinger \
	libaudioflinger \
    libcameraservice \
    libmediaplayerservice \
    libinput \
	libutils \
	libbinder \
	libcutils

LOCAL_MODULE:= libsystem_server

include $(BUILD_SHARED_LIBRARY)
