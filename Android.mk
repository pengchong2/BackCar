LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, src)



LOCAL_STATIC_JAVA_LIBRARIES := android-support-v13 libsdk
LOCAL_STATIC_JAVA_LIBRARIES += extragovernor
LOCAL_JAVA_LIBRARIES := mediatek-framework


LOCAL_DEX_PREOPT := false

LOCAL_PACKAGE_NAME := Backcar
LOCAL_CERTIFICATE := platform

LOCAL_JACK_ENABLED := disabled


include $(BUILD_PACKAGE)
include $(CLEAR_VARS)
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := libsdk:jar/flysdk_V1.0.jar 

include $(BUILD_MULTI_PREBUILT)

include $(call all-makefiles-under,$(LOCAL_PATH))
					
