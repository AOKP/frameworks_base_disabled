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

#define LOG_TAG "ShotParams"
#include <utils/Log.h>

#include <string.h>
#include <stdlib.h>
#include <camera/ShotParameters.h>

namespace android {
// Parameter keys to communicate between camera application and driver.
const char ShotParameters::KEY_BURST[] = "burst-capture";
const char ShotParameters::KEY_EXP_GAIN_PAIRS[] = "exp-gain-pairs";
const char ShotParameters::KEY_EXP_COMPENSATION[] = "exp-compensation";
const char ShotParameters::KEY_FLUSH_CONFIG[] = "flush";
const char ShotParameters::TRUE[] = "true";
const char ShotParameters::FALSE[] = "false";

void ShotParameters::setBurst(int num_shots)
{
    set(KEY_BURST, num_shots);
}

void ShotParameters::setExposureGainPairs(const char *pairs)
{
    set(KEY_EXP_GAIN_PAIRS, pairs);
}

void ShotParameters::setExposureCompensation(const char *comp)
{
    set(KEY_EXP_COMPENSATION, comp);
}

void ShotParameters::setFlushConfig(bool flush)
{
    set(KEY_FLUSH_CONFIG, flush ? TRUE : FALSE);
}

}; // namespace android
