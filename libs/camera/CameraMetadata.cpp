/*
**
** Copyright 2008, The Android Open Source Project
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at
**
**     http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** See the License for the specific language governing permissions and
** limitations under the License.
*/

#define LOG_TAG "CameraMetadata"
#include <utils/Log.h>

#include <string.h>
#include <stdlib.h>
#include <camera/CameraMetadata.h>

namespace android {
// Metadata keys to communicate between camera application and driver.
const char CameraMetadata::KEY_FRAME_NUMBER[] = "frame-number";
const char CameraMetadata::KEY_SHOT_NUMBER[] = "shot-number";
const char CameraMetadata::KEY_DIGITAL_ZOOM_FACTOR[] = "digital-zoom-factor";
const char CameraMetadata::KEY_OPTICAL_ZOOM_FACTOR[] = "optical-zoom-factor";
const char CameraMetadata::KEY_CROP_CENTER[] = "crop-center";
const char CameraMetadata::KEY_LOW_LIGHT[] = "low-light";
const char CameraMetadata::KEY_FLASH_ENABLED[] = "flash-enabled";
const char CameraMetadata::KEY_EXPOSURE_LOCK[] = "exposure-lock";
const char CameraMetadata::KEY_FOCUS_LOCK[] = "focus-lock";
const char CameraMetadata::KEY_WHITEBALANCE_LOCK[] = "whitebalance-lock";
const char CameraMetadata::KEY_ANALOG_GAIN[] = "analog-gain";
const char CameraMetadata::KEY_ANALOG_GAIN_REQ[] = "analog-gain-req";
const char CameraMetadata::KEY_ANALOG_GAIN_MIN[] = "analog-gain-min";
const char CameraMetadata::KEY_ANALOG_GAIN_MAX[] = "analog-gain-max";
const char CameraMetadata::KEY_ANALOG_GAIN_DEV[] = "analog-gain-dev";
const char CameraMetadata::KEY_ANALOG_GAIN_ERROR[] = "analog-gain-error";
const char CameraMetadata::KEY_DIGITAL_GAIN[] = "digital-gain";
const char CameraMetadata::KEY_APERTURE_VALUE[] = "aperure-value";
const char CameraMetadata::KEY_EXPOSURE_TIME[] = "exposure-time";
const char CameraMetadata::KEY_EXPOSURE_TIME_REQ[] = "exposure-time-req";
const char CameraMetadata::KEY_EXPOSURE_TIME_MIN[] = "exposure-time-min";
const char CameraMetadata::KEY_EXPOSURE_TIME_MAX[] = "exposure-time-max";
const char CameraMetadata::KEY_EXPOSURE_TIME_DEV[] = "exposure-time-dev";
const char CameraMetadata::KEY_EXPOSURE_TIME_ERROR[] = "exposure-time-error";
const char CameraMetadata::KEY_EXPOSURE_COMPENSATION_INDEX[] = "exposure-compensation-index";
const char CameraMetadata::KEY_EXPOSURE_COMPENSATION_STEP[] = "exposure-compensation-step";
const char CameraMetadata::KEY_EXPOSURE_COMPENSATION_REQ[] = "exposure-compensation-req";
const char CameraMetadata::KEY_EXPOSURE_DEV[] = "exposure-dev";
const char CameraMetadata::KEY_CURRENT_ISO[] = "current-iso";
const char CameraMetadata::KEY_FOCUS_DISTANCE[] = "focus-distance";
const char CameraMetadata::KEY_WINDOW_AVERAGE[] = "window-average";
const char CameraMetadata::KEY_HISTOGRAM[] = "histogram";
const char CameraMetadata::KEY_SHARPNESS[] = "sharpness";
const char CameraMetadata::KEY_SATURATION_LEVEL[] = "saturation-level";
const char CameraMetadata::KEY_TONE_MAPPING[] = "tone-mapping";
const char CameraMetadata::KEY_GAMMA_CORRECTION[] = "gamma-correction";
const char CameraMetadata::KEY_TIMESTAMP[] = "timestamp";

const char CameraMetadata::KEY_AWB_TEMP[] = "awb-temp";
const char CameraMetadata::KEY_AWB_GAINS[] = "awb-gains";
const char CameraMetadata::KEY_AWB_OFFSETS[] = "awb-offsets";

const char CameraMetadata::KEY_LSC_TABLE[] = "lsc-table";
const char CameraMetadata::KEY_LSC_TABLE_APPLIED[] = "lsc-table-applied";

const char CameraMetadata::VAL_TRUE[] = "true";
const char CameraMetadata::VAL_FALSE[] = "false";

// Public methods

void CameraMetadata::setBool(const char *key, bool value)
{
    switch (value) {
        case true:
            set(key, VAL_TRUE);
            break;
        case false:
            set(key, VAL_FALSE);
            break;
        default:
            LOGE("Value not valid");
            break;
    }
}

void CameraMetadata::setPosition(const char *key, int x, int y)
{
    char str[32];
    sprintf(str, "%d,%d", x, y);
    set(key, str);
}

void CameraMetadata::setTime(const char *key, nsecs_t timestamp)
{
    char str[20];
    sprintf(str, "%llu", timestamp);
    set(key, str);
}

void CameraMetadata::set4(const char *key, int v1, int v2, int v3, int v4)
{
    char str[64];
    sprintf(str, "%d,%d,%d,%d", v1, v2, v3, v4);
    set(key, str);
}

bool CameraMetadata::getBool(const char *key) const
{
    const char *v = get(key);
    bool ret = false;

    if (NULL == v) {
        LOGE("Value missing");
    } else if ( 0 == strcmp (v, VAL_TRUE) ) {
        ret = true;
    } else if ( 0 == strcmp (v, VAL_FALSE) ) {
        ret = false;
    } else {
        LOGE("Value not valid");
    }
    return ret;
}

void CameraMetadata::getPosition(const char *key, int *x, int *y) const
{
    *x = *y = -1;
    const char *v = get(key);
    if (NULL != v) {
        sscanf(v, "%d,%d", x, y);
    }
}

nsecs_t CameraMetadata::getTime(const char *key)
{
    const char *v = get(key);
    if (v == 0)
        return 0;
    return strtoull(v, 0, 0);
}

void CameraMetadata::get4(const char *key, int *v1, int *v2, int *v3, int *v4) const
{
    *v1 = *v2 = *v3 = *v4 = -1;
    const char *v = get(key);
    if (NULL != v) {
        sscanf(v, "%d,%d,%d,%d", v1, v2, v3, v4);
    }
}

};

