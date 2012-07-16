/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef ANDROID_CAMERA_BUFFER_CONTEXT_JNI_H
#define ANDROID_CAMERA_BUFFER_CONTEXT_JNI_H


#include <jni.h>


#ifdef __cplusplus
extern "C" {
#endif

struct CameraBufferContext;
typedef struct CameraBufferContext CameraBufferContext;

/**
 * TODO: comment interface
 */
CameraBufferContext* CameraBufferContext_new(JNIEnv* env, jobject jSurfaceTexture);
void CameraBufferContext_delete(CameraBufferContext* context);

void CameraBufferContext_getBuffer(CameraBufferContext* context, void** buffer);
void CameraBufferContext_releaseBuffer(CameraBufferContext* context);

int CameraBufferContext_getBufferWidth(CameraBufferContext* context);
int CameraBufferContext_getBufferHeight(CameraBufferContext* context);
int CameraBufferContext_getBufferStride(CameraBufferContext* context);

#ifdef __cplusplus
};
#endif

#endif // ANDROID_CAMERA_BUFFER_CONTEXT_JNI_H
