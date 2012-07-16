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
#define LOG_NDEBUG 0
#define LOG_TAG "CAMBUFCTX"
#include <utils/Log.h>
#include <stdint.h>
#include <surfaceflinger/Surface.h>
#include <android_runtime/android_view_Surface.h>
#include <android_runtime/android_graphics_SurfaceTexture.h>
#include <ui/PixelFormat.h>
#include <android/camera_buffer_context.h>


using namespace android;

struct CameraBufferContext : public RefBase {
protected:

  Mutex mLock;
  sp<SurfaceTexture> st;
  sp<GraphicBuffer> gbuffer;
  int width, height, stride;
  void* addr;


public:

  CameraBufferContext(JNIEnv* env, jobject jSurfaceTexture) {
    st = SurfaceTexture_getSurfaceTexture(env, jSurfaceTexture);
    gbuffer = st->takeCurrentBuffer();
    width = gbuffer->getWidth();
    height = gbuffer->getHeight();
    stride = gbuffer->getStride();
  }

  void getBuffer(void** buffer) {
    gbuffer->lock(GRALLOC_USAGE_SW_READ_OFTEN, (void**) buffer);
  }

  void releaseBuffer() {
    gbuffer->unlock();
    st->releaseBuffer(gbuffer);
  }

  int getWidth() {
    return width;
  }

  int getHeight() {
    return height;
  }

  int getStride() {
    return stride;
  }

};

CameraBufferContext* CameraBufferContext_new(JNIEnv* env, jobject jSurfaceTexture) {
  sp<CameraBufferContext> context = new CameraBufferContext(env, jSurfaceTexture);
  if (context == NULL)
    return NULL;

  context->incStrong((void*) CameraBufferContext_new);
  return static_cast<CameraBufferContext*>(context.get());
}

void CameraBufferContext_delete(CameraBufferContext* context) {
  if (context) {
        context->decStrong((void*)CameraBufferContext_new);
    }
}

void CameraBufferContext_getBuffer(CameraBufferContext* context, void** buffer) {
  context->getBuffer(buffer);
}

void CameraBufferContext_releaseBuffer(CameraBufferContext* context) {
  context->releaseBuffer();
}

int CameraBufferContext_getBufferWidth(CameraBufferContext* context) {
  return context->getWidth();
}

int CameraBufferContext_getBufferHeight(CameraBufferContext* context) {
  return context->getHeight();
}

int CameraBufferContext_getBufferStride(CameraBufferContext* context) {
  return context->getStride();
}
