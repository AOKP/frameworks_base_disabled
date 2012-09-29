BASE_PATH := $(call my-dir)
LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

# our source files
#
LOCAL_SRC_FILES:= \
    asset_manager.cpp \
    configuration.cpp \
    input.cpp \
    looper.cpp \
    native_activity.cpp \
    native_window.cpp \
    obb.cpp \
    sensor.cpp \
    storage_manager.cpp

ifeq ($(OMAP_ENHANCEMENT), true)
LOCAL_SRC_FILES+= \
    camera_buffer_context.cpp
endif

LOCAL_SHARED_LIBRARIES := \
    libcutils \
    libutils \
    libbinder \
    libui \
    libgui \
    libandroid_runtime

LOCAL_STATIC_LIBRARIES := \
    libstorage

LOCAL_C_INCLUDES += \
    frameworks/base/native/include \
    frameworks/base/core/jni/android \
    dalvik/libnativehelper/include/nativehelper

LOCAL_MODULE:= libandroid

include $(BUILD_SHARED_LIBRARY)
