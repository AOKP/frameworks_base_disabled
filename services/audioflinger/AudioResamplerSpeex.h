/*
 * Copyright (C) 2012 Texas Instruments
 *
 * Contact: Jyri Sarha <jsarha@ti.com>
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

#ifndef ANDROID_AUDIO_RESAMPLER_SPEEX_H
#define ANDROID_AUDIO_RESAMPLER_SPEEX_H

#ifndef OMAP_ENHANCEMENT
#error This file should only be used with the OMAP_ENHANCEMENT macro.
#endif

#include <stdint.h>
#include <sys/types.h>
#include <cutils/log.h>

#include <speex/speex_resampler.h>

#include "AudioResampler.h"

namespace android {

class AudioResamplerSpeex : public AudioResampler {
public:
    AudioResamplerSpeex(int bitDepth, int inChannelCount, int32_t sampleRate,
                        int speexQuality = 2);
    virtual ~AudioResamplerSpeex();

    virtual void setSampleRate(int32_t inSampleRate);
    virtual void resample(int32_t* out, size_t outFrameCount,
            AudioBufferProvider* provider);
    virtual void reset();

    virtual int32_t checkCRate(int32_t outRate, int32_t inRate) const {
        return 0;
    }

private:
    void init();
    void resampleMono16(int32_t* out, size_t outFrameCount,
            AudioBufferProvider* provider);
    void resampleStereo16(int32_t* out, size_t outFrameCount,
            AudioBufferProvider* provider);

    SpeexResamplerState *mSpeexResamplerState;
    int mQuality;
    uint32_t mRateNum;
    uint32_t mRateDen;
};

}; // namespace android

#endif /*ANDROID_AUDIO_RESAMPLER_SPEEX_H*/
