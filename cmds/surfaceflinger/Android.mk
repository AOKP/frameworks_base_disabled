LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

ifeq ($(MAKECMDGOALS), sdk_addon)
ifeq ($(TARGET_PRODUCT), s3d)
LOCAL_CPPFLAGS += -DOMAP_ENHANCEMENT_S3D
endif
endif

LOCAL_SRC_FILES:= \
	main_surfaceflinger.cpp 

LOCAL_SHARED_LIBRARIES := \
	libsurfaceflinger \
	libbinder \
	libutils

LOCAL_C_INCLUDES := \
	$(LOCAL_PATH)/../../services/surfaceflinger

ifeq ($(BOARD_USES_QCOM_HARDWARE),true)
LOCAL_C_INCLUDES +=  hardware/qcom/display/libqcomui
endif

LOCAL_MODULE:= surfaceflinger

include $(BUILD_EXECUTABLE)
