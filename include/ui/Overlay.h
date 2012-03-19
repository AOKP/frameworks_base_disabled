/*
 * Copyright (C) 2012 The Android Open Source Project
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

#ifndef ANDROID_OVERLAY_H
#define ANDROID_OVERLAY_H

#include <stdint.h>
#include <sys/types.h>

#include <utils/Errors.h>
#include <binder/IInterface.h>
#include <utils/RefBase.h>
#include <utils/threads.h>
#include <hardware/gralloc.h>

#include <ui/PixelFormat.h>

typedef void (*overlay_queue_buffer_hook)(void *data,
        void* buffer, size_t size);

namespace android {

struct mapping_data_t {
    int fd;
    size_t length;
    uint32_t offset;
    void *ptr;
};

enum OverlayFormats {
    OVERLAY_FORMAT_YUV422SP,
    OVERLAY_FORMAT_YUV420SP,
    OVERLAY_FORMAT_YUV422I,
    OVERLAY_FORMAT_YUV420P,
    OVERLAY_FORMAT_RGB565,
    OVERLAY_FORMAT_RGBA8888,
    OVERLAY_FORMAT_UNKNOWN
};

int getBppFromOverlayFormat(OverlayFormats format);
OverlayFormats getOverlayFormatFromString(const char* name);

typedef void* overlay_buffer_t;
typedef uint32_t overlay_handle_t;

class Overlay : public virtual RefBase
{
public:
    Overlay(uint32_t width, uint32_t height, OverlayFormats format, overlay_queue_buffer_hook queue_buffer, void* hook_data);

    /* destroys this overlay */
    void destroy();

    /* get the HAL handle for this overlay */
    overlay_handle_t getHandleRef() const;

    /* blocks until an overlay buffer is available and return that buffer. */
    status_t dequeueBuffer(overlay_buffer_t* buffer);

    /* release the overlay buffer and post it */
    status_t queueBuffer(overlay_buffer_t buffer);

    /* change the width and height of the overlay */
    status_t resizeInput(uint32_t width, uint32_t height);

    status_t setCrop(uint32_t x, uint32_t y, uint32_t w, uint32_t h) ;

    status_t getCrop(uint32_t* x, uint32_t* y, uint32_t* w, uint32_t* h) ;

    /* set the buffer attributes */
    status_t setParameter(int param, int value);
    status_t setFd(int fd);

    /* returns the address of a given buffer if supported, NULL otherwise. */
    void* getBufferAddress(overlay_buffer_t buffer);

    /* get physical informations about the overlay */
    uint32_t getWidth() const;
    uint32_t getHeight() const;
    int32_t getFormat() const;
    int32_t getWidthStride() const;
    int32_t getHeightStride() const;
    int32_t getBufferCount() const;
    status_t getStatus() const;

private:
    virtual ~Overlay();

    // C style hook
    overlay_queue_buffer_hook queue_buffer_hook;
    void* hook_data;

    // overlay data
    static const uint32_t NUM_BUFFERS = 8;
    static const uint32_t NUM_MIN_FREE_BUFFERS = 2;
    uint32_t numFreeBuffers;

    status_t mStatus;
    uint32_t width, height;

    // ashmem region
    mapping_data_t *mBuffers;
    bool *mQueued; // true if buffer is currently queued

    // queue/dequeue mutex
    pthread_mutex_t queue_mutex;
};

// ----------------------------------------------------------------------------

}; // namespace android

#endif // ANDROID_OVERLAY_H
