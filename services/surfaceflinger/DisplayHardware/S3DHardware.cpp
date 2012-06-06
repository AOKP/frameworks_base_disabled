/*
 * Copyright (C) 2011 Texas Instruments Inc.
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

#include "S3DHardware.h"

#include <fcntl.h>
#include <utils/Log.h>

namespace android {

//Assuming display0 is the default display
static const char kS3DCapableFileName[] = "/sys/devices/platform/omapdss/display0/s3d_capable";
static const char kS3DEnableFileName[] = "/sys/devices/platform/omapdss/display0/s3d_enable";
static const char kS3DTypeFileName[] = "/sys/devices/platform/omapdss/display0/s3d_type";
static const char kS3DOrderFileName[] = "/sys/devices/platform/omapdss/display0/s3d_ordering";
static const char kS3DSwitchFileName[] = "/sys/devices/platform/omapdss/display0/s3d_switchable";

static const char kTopBottomName[] = "topbottom";
static const char kSbSName[] = "sidebyside";
static const char kRowInterleavedName[] = "rowInterleaved";
static const char kColInterleavedName[] = "columnInterleaved";

static const char kZeroString[] = "0";
static const char kOneString[] = "1";

static const int kMaxSysfsEntrySize = 256;

static int read_sysentry(const char* sysPath, char* data, size_t size) {
    int fd = open(sysPath, O_RDONLY);
    if (fd == -1) {
        LOGI("Could not open sysfs entry(%s)", sysPath);
        return -1;
    }
    size_t bytesread = read(fd, data, size);
    if (read(fd, data, size) < 0) {
        LOGI("Could not read sysfs entry(%s)", sysPath);
        close(fd);
        return -1;
    }
    close(fd);
    return bytesread;
}

S3DHardware::S3DHardware()
    : mType(eMono),
      mOrder(eLeftViewFirst),
      mSwitcheable(true)
{
    init();
}

status_t S3DHardware::initCheck() const {
    return mType != eMono ? NO_ERROR : NO_INIT;
}

void S3DHardware::init()
{
    char data[kMaxSysfsEntrySize];

    if (read_sysentry(kS3DCapableFileName, data, kMaxSysfsEntrySize) <= 0) {
        return;
    }

    //Is display S3D capable?
    if (!strncmp(data, kZeroString, sizeof(kZeroString)-1)) {
        return;
    }

    if (read_sysentry(kS3DSwitchFileName, data, kMaxSysfsEntrySize) > 0) {
        mSwitcheable = !strncmp(data, kOneString, sizeof(kOneString)-1);
    }

    if (read_sysentry(kS3DOrderFileName, data, kMaxSysfsEntrySize) > 0) {
        if (strncmp(data, kOneString, sizeof(kOneString)-1)) {
            mOrder = eLeftViewFirst;
        } else {
            mOrder = eRightViewFirst;
        }
    }

    if (read_sysentry(kS3DTypeFileName, data, kMaxSysfsEntrySize) > 0) {
        if(!strncmp(data, kTopBottomName, sizeof(kTopBottomName)-1)) {
            mType = eTopBottom;
        } else if(!strncmp(data, kSbSName, sizeof(kSbSName)-1)) {
            mType = eSideBySide;
        } else if(!strncmp(data, kColInterleavedName, sizeof(kColInterleavedName)-1)) {
            mType = eColInterleaved;
        } else if(!strncmp(data, kRowInterleavedName, sizeof(kRowInterleavedName)-1)) {
            mType = eRowInterleaved;
        } else {
            LOGE("Unsupported s3d hw type%s", data);
        }
    }
}

void S3DHardware::forceConfiguration(S3DLayoutType type, S3DLayoutOrder order, bool switcheable)
{
    mType = type;
    mOrder = order;
    mSwitcheable = switcheable;
}

status_t S3DHardware::enableS3D(bool state)
{
    if (mType == eMono || mCurrentState == state) {
        return NO_ERROR;
    }

    int fd = open(kS3DEnableFileName, O_WRONLY);
    if (fd == -1) {
        return UNKNOWN_ERROR;
    }

    char data = state ? '1': '0';
    size_t bytesWritten = write(fd, &data, 1);
    if (bytesWritten <= 0) {
        close(fd);
        return UNKNOWN_ERROR;
    }

    mCurrentState = state;
    close(fd);
    return NO_ERROR;
}

}; // namespace android
