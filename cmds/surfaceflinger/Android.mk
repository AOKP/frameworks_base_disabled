LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

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
LOCAL_CFLAGS += -DQCOM_HARDWARE
endif

LOCAL_MODULE:= surfaceflinger

include $(BUILD_EXECUTABLE)
