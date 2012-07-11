LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:=                     \
        ColorConverter.cpp            \
        SoftwareRenderer.cpp

LOCAL_C_INCLUDES := \
        $(TOP)/frameworks/base/include/media/stagefright/openmax \
        $(TOP)/hardware/msm7k

ifeq ($(BOARD_USES_QCOM_HARDWARE),true)
        LOCAL_C_INCLUDES += $(TOP)/vendor/qcom/opensource/omx/mm-core/omxcore/inc
endif

LOCAL_MODULE:= libstagefright_color_conversion

ifeq ($(OMAP_ENHANCEMENT), true)
        LOCAL_C_INCLUDES += $(TOP)/hardware/ti/omap4xxx/domx/omx_core/inc
endif

include $(BUILD_STATIC_LIBRARY)
