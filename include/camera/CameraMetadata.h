/*
 * Copyright (C) 2008 The Android Open Source Project
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

#ifndef CAMERA_METADATA_H
#define CAMERA_METADATA_H

#include <camera/CameraParametersBase.h>
#include <utils/Timers.h>

namespace android {

// Class that handles the Camera Frame metadata
class CameraMetadata : public CameraParametersBase
{
public:
    CameraMetadata() : CameraParametersBase() { }
    CameraMetadata(const String8 &params) : CameraParametersBase(params) { }

    void setBool(const char *key, bool value);
    void setPosition(const char *key, int x, int y);
    void setTime(const char *key, nsecs_t timestamp);
    // TODO: replace this to something better
    void set4(const char *key, int v1, int v2, int v3, int v4);

    bool getBool(const char *key) const;
    void getPosition(const char *key, int *x, int *y) const;
    nsecs_t getTime(const char *key);
    // TODO: replace this to something better
    void get4(const char *key, int *v1, int *v2, int *v3, int *v4) const;

    // KEY_FRAME_NUMBER is the metadata key to retrieve the information about frame
    // number in processing sequence (i.e preview)
    static const char KEY_FRAME_NUMBER[];

    // KEY_SHOT_NUMBER is the metadata key to retrieve the information about shot number
    // in a burst sequence.
    static const char KEY_SHOT_NUMBER[];

    // KEY_DIGITAL_ZOOM_FACTOR is the metadata key to retrieve the information about
    // digital zoom factor
    static const char KEY_DIGITAL_ZOOM_FACTOR[];

    // KEY_OPTICAL_ZOOM_FACTOR is the metadata key to retrieve the information about
    // optical zoom factor
    static const char KEY_OPTICAL_ZOOM_FACTOR[];

    // KEY_CROP_CENTER is the metadata key to retrieve the information about crop
    // center co-ordinates. The co-ordinates are returned X first and Y second i.e (X,Y).
    static const char KEY_CROP_CENTER[];

    // KEY_LOW_LIGHT is the metadata key to retrieve the low-light indication.  If the
    // flash is disabled, the app can warn of a disabled flash and/or give a hand-shake
    // warning.  It could also be beneficial for the App to know when the flash will fire
    // in Auto mode. Metadata returned is VAL_FALSE when scene has adequate light and
    // VAL_TRUE when a low-lit scene is detected.
    static const char KEY_LOW_LIGHT[];

    // KEY_FLASH_ENABLED is the metadata key to retrieve the flash status information for
    // current frame. Metadata returned is VAL_FALSE when flash is disabled and
    // VAL_TRUE when flash is enabled.
    static const char KEY_FLASH_ENABLED[];

    // KEY_EXPOSURE_LOCK is the metadata key to retrieve exposure lock information for
    // current frame. Metadata returned is VAL_FALSE when exposure lock is disabled and
    // VAL_TRUE when exposure lock is enabled.
    static const char KEY_EXPOSURE_LOCK[];

    // KEY_FOCUS_LOCK is the metadata key to retrieve focus lock information for
    // current frame. Metadata returned is VAL_FALSE when focus lock is disabled and
    // VAL_TRUE when focus lock is enabled.
    static const char KEY_FOCUS_LOCK[];

    // KEY_WHITEBALANCE_LOCK is the metadata key to retrieve awb lock information for
    // current frame. Metadata returned is VAL_FALSE when awb lock is disabled and
    // VAL_TRUE when awb lock is enabled.
    static const char KEY_WHITEBALANCE_LOCK[];

    // KEY_ANALOG_GAIN is the metadata key to retrieve analog gain information for
    // current frame. Metadata is represented as 100*EV.
    static const char KEY_ANALOG_GAIN[];

    // KEY_ANALOG_GAIN_REQ is the metadata key to retrieve analog gain information
    // requested by application for current frame. Metadata is represented as 100*EV.
    static const char KEY_ANALOG_GAIN_REQ[];

    // KEY_ANALOG_GAIN_MIN is the metadata key to retrieve the analog gain
    // lower limit for current frame. Metadata is represented as 100*EV.
    static const char KEY_ANALOG_GAIN_MIN[];

    // KEY_ANALOG_GAIN_MAX is the metadata key to retrieve the analog gain
    // upper limit for current frame. Metadata is represented as 100*EV.
    static const char KEY_ANALOG_GAIN_MAX[];

    // KEY_ANALOG_GAIN_DEV is the metadata key to retrieve the analog gain
    // deviation after flicker reduction for current frame. Metadata is represented as 100*EV.
    static const char KEY_ANALOG_GAIN_DEV[];

    // KEY_ANALOG_GAIN_ERROR is the metadata key to retrieve analog gain error for
    // current frame. Represents the difference between requested value and actual value.
    static const char KEY_ANALOG_GAIN_ERROR[];

    // KEY_DIGITAL_GAIN is the metadata key to retrieve digital gain information for
    // current frame. Metadata is represented as 10*EV.
    static const char KEY_DIGITAL_GAIN[];

    // KEY_APERTURE_VALUE is the metadata key to retrieve aperture information for
    // current frame. Metadata is represented as 10*Aperture.
    static const char KEY_APERTURE_VALUE[];

    // KEY_EXPOSURE_TIME is the metadata key to retrieve the exposure time for current frame.
    // Metadata is represented in us.
    static const char KEY_EXPOSURE_TIME[];

    // KEY_EXPOSURE_TIME_REQ is the metadata key to retrieve the exposure time requested by
    // application for current frame. Metadata is represented in us.
    static const char KEY_EXPOSURE_TIME_REQ[];

    // KEY_EXPOSURE_TIME_MIN is the metadata key to retrieve the exposure time lower limit for
    // current frame. Metadata is represented in us.
    static const char KEY_EXPOSURE_TIME_MIN[];

    // KEY_EXPOSURE_TIME_MAX is the metadata key to retrieve the exposure time upper limit for
    // current frame. Metadata is represented in us.
    static const char KEY_EXPOSURE_TIME_MAX[];

    // KEY_EXPOSURE_TIME_DEV is the metadata key to retrieve the exposure time
    // deviation after flicker reduction for current frame. Metadata is represented in us.
    static const char KEY_EXPOSURE_TIME_DEV[];

    // KEY_EXPOSURE_TIME_ERROR is the metadata key to retrieve the time difference between
    // requested exposure time and actual exposure time.
    static const char KEY_EXPOSURE_TIME_ERROR[];

    // KEY_EXPOSURE_COMPENSATION_INDEX is the metadata key to retrieve current exposure
    // compensation index information for current frame.
    static const char KEY_EXPOSURE_COMPENSATION_INDEX[];

    // KEY_EXPOSURE_COMPENSATION_STEP is the metadata key to retrieve current exposure
    // compensation step information for current frame.
    static const char KEY_EXPOSURE_COMPENSATION_STEP[];

    // KEY_EXPOSURE_COMPENSATION_REQ is the metadata key to retrieve the current total exposure
    // compensation requested by application for current frame.  Metadata is represented as 100*EV.
    static const char KEY_EXPOSURE_COMPENSATION_REQ[];

    // KEY_EXPOSURE_DEV is the metadata key to retrieve the current total exposure
    // deviation for current frame.  Metadata is represented as 100*EV.
    static const char KEY_EXPOSURE_DEV[];

    // KEY_CURRENT_ISO is the metadata key to retrieve ISO setting for current frame.
    static const char KEY_CURRENT_ISO[];

    // KEY_FOCUS_DISTANCE is the metadata key to retrieve focus distance information for
    // current frame. Metadata returns focus distance in cm.
    static const char KEY_FOCUS_DISTANCE[];

    // KEY_WINDOW_AVERAGE is the metadata key to retrieve window averaging information for
    // current frame. Metadata returns the normalized average value for each window region
    // specified by application. Value is normalized to a range of 0-1023.
    static const char KEY_WINDOW_AVERAGE[];

    // KEY_HISTOGRAM is the metadata key to retrieve histogram information for current frame.
    // This returns the number of bins supported in the histogram.  Metadata returns a list of
    // integer values corresponding to each bin in the histogram. Integer value represents the
    // number of pixels that fall into each bin.
    static const char KEY_HISTOGRAM[];

    // KEY_SHARPNESS is the metadata key to retrieve sharpness information for current frame.
    // Metadata returns the normalized sharpness statistic for each window region specified by
    // application. Value is normalized to a range of 0-1023.
    static const char KEY_SHARPNESS[];

    // Metadata represents the selected saturation level for the current frame.
    // The following values are reported: 0 – Creates Grayscale image,
    // 128 – No adjustment, 255 – 2x saturation increase.
    static const char KEY_SATURATION_LEVEL[];

    // KEY_TONE_MAPPING represents the tone mapping for the current frame
    static const char KEY_TONE_MAPPING[];

    // KEY_GAMMA_CORRECTION represents the selected gamma curve for the current frame.
    static const char KEY_GAMMA_CORRECTION[];

    // KEY_TIMESTAMP represents the timestamp in terms of a reference clock.
    static const char KEY_TIMESTAMP[];

    // KEY_AWB_TEMP represents the temperature of current scene in Kelvin
    static const char KEY_AWB_TEMP[];

    // KEY_AWB_GAINS represents gains applied to each RGGB color channel. Returned as a list
    // integers with each corresponding to gain applied for the channel in this format (R,Gr,Gb,B).
    static const char KEY_AWB_GAINS[];

    // KEY_AWB_GAINS represents offests applied to each RGGB color channel. Returned as a list
    // integers with each corresponding to offset applied for the channel in this format (R,Gr,Gb,B).
    static const char KEY_AWB_OFFSETS[];

    // KEY_LSC_TABLE is the metadata key to retrieve the current
    // lens shading correction table.  The table consists of an
    // N by M array of elements.  Each element has 4 integer values
    // ranging from 0 to 1000, corresponding to a multiplier for
    // each of the Bayer color filter channels (R, Gr, Gb, B).
    // Correction is performed on pixels in a Bayer image by interpolating
    // the corresponding color filter channel in the array, and then
    // multiplying by (value/1000).
    // Values in an array element are delimited by ':', and elements
    // in a row are delimited by ','.  Each row is surrounded by '()'
    // and delimited by ','.
    // Example format for 4x4 lens shading correction table:
    // (1000:992:992:985,986:978:978:972,986:978:978:972,1000:992:992:985),
    // (876:868:868:869,854:831:831:849,854:831:831:849,876:868:868:869),
    // (876:868:868:869,854:831:831:849,854:831:831:849,876:868:868:869),
    // (1000:992:992:985,986:978:978:972,986:978:978:972,1000:992:992:985)
    static const char KEY_LSC_TABLE[];

    // KEY_LSC_TABLE_APPLIED indicates whether LSC table is applied or not
    // The value can be VAL_TRUE or VAL_FALSE
    static const char KEY_LSC_TABLE_APPLIED[];

private:
    static const char VAL_TRUE[];
    static const char VAL_FALSE[];
};

};

#endif //CAMERA_METADATA_H

