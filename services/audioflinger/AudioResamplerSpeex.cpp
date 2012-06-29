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

#define LOG_TAG "AudioSpeexSRC"
//#define LOG_NDEBUG 0

#include <stdint.h>
#include <string.h>
#include <sys/types.h>
#include <cutils/log.h>

#include "AudioResamplerSpeex.h"

#define TMPBUFFSIZE 4096

namespace android {

AudioResamplerSpeex::AudioResamplerSpeex(int bitDepth, int inChannelCount, int32_t sampleRate,
                                         int speexQuality) :
        AudioResampler(bitDepth, inChannelCount, sampleRate),
        mSpeexResamplerState(NULL), mQuality(speexQuality),
        mRateNum(0), mRateDen(0) {
}

void AudioResamplerSpeex::init() {
    int err;
    LOGD("AudioResamplerSpeex::init() mChannelCount=%u, mInSampleRate=%u, "
         "mSampleRate=%u, mQuality=%u", mChannelCount, mInSampleRate,
         mSampleRate, mQuality);

    mSpeexResamplerState = speex_resampler_init(mChannelCount,
                                                mInSampleRate,
                                                mSampleRate,
                                                mQuality,
                                                &err);
    if (err != 0) {
        LOGE("speex_resampler_init() failed: %s",
             speex_resampler_strerror(err));
        return;
    }
    speex_resampler_get_ratio(mSpeexResamplerState, &mRateNum, &mRateDen);
}

AudioResamplerSpeex::~AudioResamplerSpeex() {
    LOGD("AudioResamplerSpeex::~AudioResamplerSpeex()");

    if (mSpeexResamplerState) {
        speex_resampler_destroy(mSpeexResamplerState);
        mSpeexResamplerState = NULL;
    }
}

void AudioResamplerSpeex::setSampleRate(int32_t inSampleRate) {
    /* It appears AudioFlinger calls this method between each resample()
       call. So, it is good to filter unnecessary processing if there
       is no change.
    */
    if (inSampleRate == mInSampleRate) {
        return;
    }

    LOGD("AudioResamplerSpeex::setSampleRate() mChannelCount=%u, "
         "mInSampleRate=%u->%u, mSampleRate=%u, mQuality=%u",
         mChannelCount, mInSampleRate, inSampleRate, mSampleRate, mQuality);

    mInSampleRate = inSampleRate;
    int  err = speex_resampler_set_rate(mSpeexResamplerState,
                                        mInSampleRate,
                                        mSampleRate);
    if (err != 0) {
        LOGE("speex_resampler_set_rate() failed: %s",
             speex_resampler_strerror(err));
        return;
    }

    speex_resampler_get_ratio(mSpeexResamplerState, &mRateNum, &mRateDen);
}

void AudioResamplerSpeex::reset() {
    LOGD("AudioResamplerSpeex::reset()");

    AudioResampler::reset();

    int err = speex_resampler_reset_mem(mSpeexResamplerState);

    if (err != 0) {
        LOGE("speex_resampler_reset_mem() failed: %s",
             speex_resampler_strerror(err));
    }
}

void AudioResamplerSpeex::resample(int32_t* out, size_t outFrameCount,
        AudioBufferProvider* provider) {

    switch (mChannelCount) {
    case 1:
        resampleMono16(out, outFrameCount, provider);
        break;
    case 2:
        resampleStereo16(out, outFrameCount, provider);
        break;
    default:
        LOGE("Unsupported number of channels %d", mChannelCount);
        break;
    }
}

void AudioResamplerSpeex::resampleStereo16(int32_t* out, size_t outFrameCount,
        AudioBufferProvider* provider) {

    size_t outFrameIndex = 0;
    size_t inFrameCount = (outFrameCount*mRateNum) / mRateDen;

    while(outFrameIndex < outFrameCount) {
        if (mInputIndex >= mBuffer.frameCount) {
            if (mBuffer.raw != NULL)
                provider->releaseBuffer(&mBuffer);
            mInputIndex = 0;
        }
        if (mBuffer.frameCount == 0) {
            mBuffer.frameCount = inFrameCount;
            provider->getNextBuffer(&mBuffer);
            if (mBuffer.raw == NULL)
                return;
        }
        int16_t *in = &mBuffer.i16[mChannelCount*mInputIndex];
        uint32_t inCount = (mBuffer.frameCount - mInputIndex);
        int16_t outBuffer[TMPBUFFSIZE];
        uint32_t outCount = sizeof(outBuffer)/(sizeof(int16_t)*mChannelCount);
        outCount = outCount < (outFrameCount-outFrameIndex) ?
            outCount : (outFrameCount-outFrameIndex);

        (void)speex_resampler_process_interleaved_int(mSpeexResamplerState,
                                                      in,
                                                      &inCount,
                                                      outBuffer,
                                                          &outCount);
        mInputIndex += inCount;

        for(uint i=0; i < outCount; i++) {
            out[2*(outFrameIndex+i)] += (int32_t)mVolume[0] * outBuffer[2*i];
            out[2*(outFrameIndex+i)+1] += (int32_t)mVolume[1] * outBuffer[2*i+1];
        }

        outFrameIndex += outCount;
    }
}

void AudioResamplerSpeex::resampleMono16(int32_t* out, size_t outFrameCount,
        AudioBufferProvider* provider) {

    size_t outFrameIndex = 0;
    size_t inFrameCount = (outFrameCount*mRateNum) / mRateDen;

    while(outFrameIndex < outFrameCount) {
        if (mInputIndex >= mBuffer.frameCount) {
            if (mBuffer.raw != NULL)
                provider->releaseBuffer(&mBuffer);
            mInputIndex = 0;
        }
        if (mBuffer.frameCount == 0) {
            mBuffer.frameCount = inFrameCount;
            provider->getNextBuffer(&mBuffer);
            if (mBuffer.raw == NULL)
                return;
        }
        int16_t *in = &mBuffer.i16[mInputIndex];
        uint32_t inCount = (mBuffer.frameCount - mInputIndex);
        int16_t outBuffer[TMPBUFFSIZE];
        uint32_t outCount = sizeof(outBuffer)/(sizeof(int16_t));
        outCount = outCount < (outFrameCount-outFrameIndex) ?
            outCount : (outFrameCount-outFrameIndex);

        (void)speex_resampler_process_int(mSpeexResamplerState,
                                          0,
                                          in,
                                          &inCount,
                                          outBuffer,
                                          &outCount);
        mInputIndex += inCount;

        for(uint i=0; i < outCount; i++) {
            out[2*(outFrameIndex+i)] += (int32_t)mVolume[0] * outBuffer[i];
            out[2*(outFrameIndex+i)+1] += (int32_t)mVolume[1] * outBuffer[i];
        }

        outFrameIndex += outCount;
    }
}

}
; // namespace android
