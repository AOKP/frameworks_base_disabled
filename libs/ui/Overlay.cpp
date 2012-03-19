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

//#define LOG_NDEBUG 0
#define LOG_TAG "Overlay"

#include <binder/IMemory.h>
#include <binder/Parcel.h>
#include <utils/Errors.h>
#include <binder/MemoryHeapBase.h>
#include <cutils/ashmem.h>
#include <ui/Overlay.h>

namespace android {

int getBppFromOverlayFormat(const OverlayFormats format) {
    int bpp;
    switch(format) {
    case OVERLAY_FORMAT_RGBA8888:
        bpp=32;
        break;
    case OVERLAY_FORMAT_RGB565:
    case OVERLAY_FORMAT_YUV422I:
    case OVERLAY_FORMAT_YUV422SP:
        bpp = 16;
        break;
    case OVERLAY_FORMAT_YUV420SP:
    case OVERLAY_FORMAT_YUV420P:
        bpp = 12;
        break;
    default:
        LOGW("%s: unhandled color format %d", __FUNCTION__, format);
        bpp = 32;
    }
    return bpp;
}

OverlayFormats getOverlayFormatFromString(const char* name) {
    OverlayFormats rv = OVERLAY_FORMAT_UNKNOWN;
    if( strcmp(name, "yuv422sp") == 0 ) {
        rv = OVERLAY_FORMAT_YUV422SP;
    } else if( strcmp(name, "yuv420sp") == 0 ) {
        rv = OVERLAY_FORMAT_YUV420SP;
    } else if( strcmp(name, "yuv422i-yuyv") == 0 ) {
        rv = OVERLAY_FORMAT_YUV422I;
    } else if( strcmp(name, "yuv420p") == 0 ) {
        rv = OVERLAY_FORMAT_YUV420P;
    } else if( strcmp(name, "rgb565") == 0 ) {
        rv = OVERLAY_FORMAT_RGB565;
    } else if( strcmp(name, "rgba8888") == 0 ) {
        rv = OVERLAY_FORMAT_RGBA8888;
    }
    return rv;
}

Overlay::Overlay(uint32_t width, uint32_t height, OverlayFormats format, overlay_queue_buffer_hook queue_buffer, void *hook_data) : mStatus(NO_INIT) {
    LOGD("%s: Init overlay", __FUNCTION__);
    this->queue_buffer_hook = queue_buffer;
    this->hook_data = hook_data;
    this->width = width;
    this->height = height;
    this->numFreeBuffers = 0;

    const int reqd_mem = width * height * getBppFromOverlayFormat(format) >> 3;
    const int BUFFER_SIZE = ((reqd_mem + PAGE_SIZE-1) & ~(PAGE_SIZE-1));
    if (reqd_mem % PAGE_SIZE) {
        // required on tegra2, else only one half of buffers are mapped (atrix)
        LOGD("%s: buffer size %d adjusted to be multiple of %d : %d.", __FUNCTION__, reqd_mem, (int) PAGE_SIZE, BUFFER_SIZE);
    }

    int fd = ashmem_create_region("Overlay_buffer_region", NUM_BUFFERS * BUFFER_SIZE);
    if (fd < 0) {
        LOGE("%s: Cannot create ashmem region", __FUNCTION__);
        return;
    }

    LOGV("%s: allocated ashmem region for %d buffers of size %d", __FUNCTION__, NUM_BUFFERS, BUFFER_SIZE);

    mBuffers = new mapping_data_t[NUM_BUFFERS];
    mQueued = new bool[NUM_BUFFERS];
    for(uint32_t i=0; i<NUM_BUFFERS; i++) {
        mBuffers[i].fd = fd;
        mBuffers[i].length = BUFFER_SIZE;
        mBuffers[i].offset = BUFFER_SIZE * i;
        LOGV("%s: mBuffers[%d].offset = 0x%x", __FUNCTION__, i, mBuffers[i].offset);
        mBuffers[i].ptr = mmap(NULL, BUFFER_SIZE, PROT_READ | PROT_WRITE, MAP_SHARED, fd, BUFFER_SIZE * i);
        if (mBuffers[i].ptr == MAP_FAILED) {
            LOGE("%s: Failed to mmap buffer %d", __FUNCTION__, i);
            mBuffers[i].ptr = NULL;
        }
        mQueued[i]=false;
    }

    pthread_mutex_init(&queue_mutex, NULL);

    LOGD("%s: Init overlay complete", __FUNCTION__);
    mStatus = NO_ERROR;
}

void Overlay::destroy() {
    LOGV("%s", __FUNCTION__);
    if (mBuffers != NULL) {
        for(uint32_t i=0; i<NUM_BUFFERS; i++) {
            if (mBuffers[i].ptr != NULL && munmap(mBuffers[i].ptr, mBuffers[i].length) < 0) {
                LOGW("%s: unmap of buffer %d failed", __FUNCTION__, i);
            }
            mBuffers[i].ptr = NULL;
            if (mBuffers[i].fd > 0) {
                close(mBuffers[i].fd);
            }
        }
        delete[] mBuffers;
        mBuffers=NULL;
    }
    delete[] mQueued;
    pthread_mutex_destroy(&queue_mutex);
}

Overlay::~Overlay() {
    if (mBuffers != NULL) {
        LOGV("%s: Destructor called without freeing buffers, doing it now...", __FUNCTION__);
        destroy();
    }
}

status_t Overlay::dequeueBuffer(overlay_buffer_t* buffer)
{
    LOGV("%s", __FUNCTION__);
    int rv = NO_ERROR;
    pthread_mutex_lock(&queue_mutex);

    if (numFreeBuffers < NUM_MIN_FREE_BUFFERS) {
        LOGV("%s: No free buffers", __FUNCTION__);
        rv = NO_MEMORY;
    } else {
        int index = -1;
        for(uint32_t i = 0; i < NUM_BUFFERS; i++) {
            if (mQueued[i] == true) {
                mQueued[i] = false;
                index = i;
                break;
            }
        }

        if (index >= 0) {
            *((int*)buffer) = index;
            numFreeBuffers--;
            LOGV("%s: dequeued buffer %d", __FUNCTION__, index);
        } else {
            LOGE("%s: inconsistent queue state", __FUNCTION__);
            rv = NO_MEMORY;
        }
    }

    pthread_mutex_unlock(&queue_mutex);
    return rv;
}

status_t Overlay::queueBuffer(overlay_buffer_t buffer)
{
    LOGV("%s: %d", __FUNCTION__, (int)buffer);
    if ((uint32_t)buffer > NUM_BUFFERS) {
        LOGE("%s: invalid buffer index %d", __FUNCTION__, (uint32_t) buffer);
        return INVALID_OPERATION;
    }

    if (queue_buffer_hook) {
        queue_buffer_hook(hook_data, mBuffers[(int)buffer].ptr, mBuffers[(int)buffer].length);
    }

    pthread_mutex_lock(&queue_mutex);

    int rv;
    if (numFreeBuffers < NUM_BUFFERS) {
        numFreeBuffers++;
        mQueued[(int)buffer] = true;
        rv = NO_ERROR;
    } else {
        LOGW("%s: Attempt to queue more buffers than we have", __FUNCTION__);
        rv = INVALID_OPERATION;
    }

    pthread_mutex_unlock(&queue_mutex);
    return mStatus;
}

status_t Overlay::resizeInput(uint32_t width, uint32_t height)
{
    LOGV("%s: %d, %d", __FUNCTION__, width, height);
    return mStatus;
}

status_t Overlay::setParameter(int param, int value)
{
    LOGV("%s: %d, %d", __FUNCTION__, param, value);
    return mStatus;
}

status_t Overlay::setCrop(uint32_t x, uint32_t y, uint32_t w, uint32_t h)
{
    LOGV("%s: x=%d, y=%d, w=%d, h=%d", __FUNCTION__, x, y, w, h);
    return mStatus;
}

status_t Overlay::getCrop(uint32_t* x, uint32_t* y, uint32_t* w, uint32_t* h)
{
    LOGV("%s", __FUNCTION__);
    return mStatus;
}

status_t Overlay::setFd(int fd)
{
    LOGV("%s: fd=%d", __FUNCTION__, fd);
    return mStatus;
}

int32_t Overlay::getBufferCount() const
{
    LOGV("%s: %d", __FUNCTION__, NUM_BUFFERS);
    return NUM_BUFFERS;
}

void* Overlay::getBufferAddress(overlay_buffer_t buffer)
{
    LOGV("%s: %d", __FUNCTION__, (int)buffer);
    if ((uint32_t)buffer >= NUM_BUFFERS) {
        return NULL;
        //buffer = (overlay_buffer_t) ((uint32_t)buffer % NUM_BUFFERS);
    }

    //LOGD("%s: fd=%d, length=%d. offset=%d, ptr=%p", __FUNCTION__, mBuffers[buffer].fd, mBuffers[buffer].length, mBuffers[buffer].offset, mBuffers[buffer].ptr);

    return &mBuffers[(uint32_t)buffer];
}

status_t Overlay::getStatus() const {
    LOGV("%s", __FUNCTION__);
    return mStatus;
}

overlay_handle_t Overlay::getHandleRef() const {
    LOGV("%s", __FUNCTION__);
    return 0;
}

uint32_t Overlay::getWidth() const {
    LOGV("%s", __FUNCTION__);
    return width;
}

uint32_t Overlay::getHeight() const {
    LOGV("%s", __FUNCTION__);
    return height;
}

int32_t Overlay::getFormat() const {
    LOGV("%s", __FUNCTION__);
    return 0;
}

int32_t Overlay::getWidthStride() const {
    LOGV("%s", __FUNCTION__);
    return width;
}

int32_t Overlay::getHeightStride() const {
    LOGV("%s", __FUNCTION__);
    return height;
}

// ----------------------------------------------------------------------------

}; // namespace android
