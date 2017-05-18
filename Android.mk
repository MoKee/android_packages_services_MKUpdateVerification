#
# Inline AOSP Makefile for Projekt's ThemeInterfacer
#
LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_PACKAGE_NAME := MKUpdateVerification
LOCAL_CERTIFICATE := platform
LOCAL_PROGUARD_ENABLED := disabled
LOCAL_MODULE_TAGS := optional

LOCAL_MANIFEST_FILE := app/src/main/AndroidManifest.xml
LOCAL_SRC_FILES := $(call all-java-files-under, app/src/main) $(call all-Iaidl-files-under, app/src/main)
LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/app/src/main/res

LOCAL_AAPT_FLAGS := --auto-add-overlay
LOCAL_AAPT_INCLUDE_ALL_RESOURCES := true

LOCAL_STATIC_JAVA_LIBRARIES := \
    android-support-annotations

LOCAL_STATIC_JAVA_AAR_LIBRARIES := \
    mokee-libsuperuser

include $(BUILD_PACKAGE)
