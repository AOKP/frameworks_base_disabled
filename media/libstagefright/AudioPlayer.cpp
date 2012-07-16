/*
 * Copyright (C) 2009 The Android Open Source Project
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
#define LOG_TAG "AudioPlayer"
#include <utils/Log.h>
#if defined(OMAP_ENHANCEMENT) && defined(OMAP_TIME_INTERPOLATOR)
#include <time.h>
#endif

#include <binder/IPCThreadState.h>
#include <media/AudioTrack.h>
#include <media/stagefright/foundation/ADebug.h>
#include <media/stagefright/AudioPlayer.h>
#include <media/stagefright/MediaDefs.h>
#include <media/stagefright/MediaErrors.h>
#include <media/stagefright/MediaSource.h>
#include <media/stagefright/MetaData.h>

#include "include/AwesomePlayer.h"

#if defined(OMAP_ENHANCEMENT) && defined(OMAP_TIME_INTERPOLATOR)

/* The audio latency is typically 2x the buffer size set in the
 * AudioHAL.  The value here is only used as a default value in case
 * AudioPlayer::latency() returns 0 or a degenerate value.  A similar
 * value is defined in AwesomePlayer.cpp.  They should match, but they
 * do not need to match.
 *
 * For OMAP4, The AudioHAL defines the hardware buffer at 4 x 40ms.
 */
/* 320 ms */
#define DEFAULT_AUDIO_LATENCY (40000 * 4 * 2)

#endif /* OMAP_ENHANCEMENT */

#if defined(OMAP_ENHANCEMENT) && defined(OMAP_TIME_INTERPOLATOR)
namespace omap_enhancement
{

    /** Implements and audio clock interpolator with elastic time.
     *
     * If you have a FIFO sink (or source) and the average throughput
     * is expected to be a constant (e.g. audio playback), this can be
     * used to turn the buffer writes into a monotonic clock source.
     * It is intended to be used in an audio callback.  post_buffer()
     * should be called at the BEGINNING of the callback.
     *
     * The system monotonic clock is used as the clock source for this
     * class.  The time differences given by this clock are scaled
     * based on a time factor.  This time factor should be nearly 1.0,
     * but is used to "speed up" or "slow down" our clock based on the
     * how the data in the FIFO is flowing.
     *
     * Intended use:
     *
     *     TimeInterpolator ti;
     *     ti.set_latency(2 * FIFO_SIZE);
     *
     *     ti.seek(position);
     *
     *     for each time data is written to (or read from) FIFO:
     *         ti.post_buffer(time_in_usecs)
     *
     *     ti.pause()
     *     ti.resume() // or reset(to_position_in_usecs)
     *
     *     for each time data is written to FIFO:
     *         ti.post_buffer(time_in_usecs)
     *
     * Note that time_in_usecs should be directly proportional to the
     * size of the write (or read).
     *
     * At any time, ti.get_stream_usecs() may be used to query the
     * streams position.  If the stream is rolling, it will be a
     * monotonic clock source.
     *
     * The stability criteria for this mechanism has not been formally
     * determined.  However, the following criteria have been
     * empirically determined:
     *
     *     - The latency value set is greater than or equal to the
     *       size of the fifos between AudioPlayer and the actual
     *       device output.
     *
     *     - All calls to post_buffer() will be less than half of
     *       the latency value.  (This includes "aggregated" calls
     *       to post_buffer().
     *
     *     - In any time-span roughly equal to the latency value,
     *       all calls to post_buffer() sum up to be about the same
     *       value (within about 5%).
     *
     *     - The latency value is the actual latency from the time
     *       the data is written to the buffer to the time that it
     *       comes out the speaker.
     *
     * The following error conditions are handled:
     *
     *     - OVERFLOW: More than 2x of the latency being posted in
     *       a short period of time.  In this case, the time will
     *       be abruptly updated.
     *
     *     - UNDERFLOW: The time reported by TimeInterpolator catches
     *       up to the read pointer for the audio data.  In this case,
     *       time will stop.
     *
     * This device was inspired by the paper "Using a DLL to Filter
     * Time" (F. Adriaensen, 2005).[1]
     *
     * [1]  http://kokkinizita.linuxaudio.org/papers/usingdll.pdf
     */
    class TimeInterpolator
    {
    public:
        TimeInterpolator();
        ~TimeInterpolator();

        static int64_t get_system_usecs();
        int64_t get_stream_usecs();
        void post_buffer(int64_t frame_usecs);
        void reset();
        void seek(int64_t media_time = 0);
        void pause(bool flushing_fifo = false);
        void stop();
        void resume();
        int64_t usecs_queued() {
            return m_queued;
        }
        int64_t read_pointer() {
            return m_read + m_queued;
        }
        void set_latency(int64_t lat_usecs) {
            if (lat_usecs > 0) {
                m_latency = lat_usecs;
            } else {
                m_latency = DEFAULT_AUDIO_LATENCY;
            }
        }

        /* See docs on set_state() for state progression */
        typedef enum {
            STOPPED = 0,
            ROLLING,
            PAUSED,
        } state_t;

        /* These are the inputs (reasons) for doing a state change */
        typedef enum {
            STOP = 0,
            SEEK,
            PAUSE,
            POST_BUFFER,
            ERR_UNDERRUN,
            ERR_OVERRUN,
        } input_t;

    private:
        /* All time variables are in microseconds (usecs). */
        state_t m_state;         /* the current state of this class */
        pthread_mutex_t m_mutex; /* The global class mutex */
        double m_Tf;             /* time scaling factor */
        int64_t m_t0;            /* time measured from here (epoch) */
        int64_t m_pos0;          /* media position at t0 */
        int64_t m_read;          /* read pointer of media at t0 */
        int64_t m_queued;        /* amount of media queued for next callback */
        int64_t m_latency;       /* typ. 1x or 2x the size of the FIFO */

        /* These two are for error checking
         */
        int64_t m_last;   /* the last timestamp reported to anyone */
        int64_t m_now_last;

    private:
        void set_state(state_t s, input_t i); /* mutex must already be locked */
        void err_underrun();       /* mutex must already be locked */
        void err_overrun();        /* mutex must already be locked */
    }; // class TimeInterpolator


    TimeInterpolator::TimeInterpolator()
    {
        pthread_mutex_init(&m_mutex, 0);
        /* This value should not be reset on seek() */
        m_state = STOPPED;
        m_latency = DEFAULT_AUDIO_LATENCY;
        seek(0);
    }

    TimeInterpolator::~TimeInterpolator()
    {
    }

    /* TimeInterpolator State
     *
     * These are the states (modes) for this class:
     *
     *     STOPPED - Audio is not moving, the clock is frozen, and the
     *              fifos are flushed.  This is also the initial state.
     *
     *     ROLLING - The buffer pipelines have all reached steady-state
     *              and we are using a feedback loop to control how
     *              time progresses.
     *
     *     PAUSED - Audio is not moving, the clock is frozen, and
     *              the fifos are maintaining state.  When we
     *              leave this state, we will usually go to ROLLING.
     *
     *
     * Here is the state transition chart:
     *
     * +------------------------------------------------------+
     * |                                                      |
     * |              STOPPED (Initial state)                 |<------+
     * |                                                      |       |
     * +------------------------------------------------------+       |
     *   A                                  |                         |
     *   |                             post_buffer()                  |
     *   |                                  |                         |
     *  stop()                              |                         |
     *   or                                 |                         |
     *  seek()                              |                         |
     *   |                                  V                         |
     * +--------+                      +---------+                    |
     * |        |<----pause()----------|         |                    |
     * | PAUSED |                      | ROLLING |--err_underrun()----|
     * |        |---post_buffer()----->|         |   or stop()
     * +--------+                      +---------+
     *                                  | A
     *      +-----------err_overrun()---+ |
     *      |                             |
     *     / \                            |
     *    /   \                           |
     *  Nth time?--yes---(advance time)---+
     *    \   /
     *     \ /
     *      *
     *      |
     *      no
     *      |
     *   (tweak params)
     *      |
     *      V
     *  (to ROLLING)
     *
     */
    void TimeInterpolator::set_state(state_t s, input_t i)
    {
        static const char* state_strings[] = {
            "STOPPED", "ROLLING", "PAUSED",
        };
        static const char* input_strings[] = {
            "STOP", "SEEK", "PAUSE", "POST_BUFFER", "ERR_UNDERRUN",
            "ERR_OVERRUN",
        };

        LOGV("TimeInterpolator state %s -> %s (input: %s)", state_strings[m_state],
             state_strings[s], input_strings[i]);

        if (m_state == s) {
            LOGV("TimeInterpolator calling set_state() should actually change a state.");
            return;
        }

        /* this block is just for error-checking */
        switch (m_state) {
        case STOPPED:
            if (s == ROLLING && i != POST_BUFFER) {
                LOGE("TimeInterpolator state should only change for POST_BUFFER");
            }
            if (s != ROLLING) {
                LOGE("TimeInterpolator this state should not be reachable.");
            }
            break;
        case ROLLING:
            if (s == PAUSED && i != PAUSE) {
                LOGE("TimeInterpolator state should only change for PAUSE");
            }
            if (s == STOPPED && i != STOP && i != ERR_UNDERRUN) {
                LOGE("TimeInterpolator state should only change for STOP or ERR_UNDERRUN");
            }
            if (s != PAUSED && s != STOPPED) {
                LOGE("TimeInterpolator this state should not be reachable.");
            }
            break;
        case PAUSED:
            if (s == ROLLING && i != POST_BUFFER) {
                LOGE("TimeInterpolator state should only change for POST_BUFFER");
            }
            if (s == STOPPED && i != STOP && i != SEEK) {
                LOGE("TimeInterpolator state should only change for STOP or SEEK");
            }
            if (s != ROLLING && s != STOPPED) {
                LOGE("TimeInterpolator this state shoulud not be reachable");
            }
            break;
        };
        m_state = s;
    }

    void TimeInterpolator::reset()
    {
        stop();
        seek(0);
    }

    void TimeInterpolator::stop()
    {
        pause(true);
    }

    void TimeInterpolator::seek(int64_t media_time)
    {
        pthread_mutex_lock(&m_mutex);
        LOGV("TimeInterpolator::seek(media_time=%lld)", media_time);

        if (m_state == STOPPED || m_state == PAUSED) {
            m_pos0 = media_time;
            m_read = media_time;
            m_queued = 0;
            m_t0 = get_system_usecs();
            m_Tf = 0;
            m_last = media_time;
            m_now_last = 0;
        } else {
            LOGE_IF(m_state != ROLLING, "TimeInterpolator logic error: "
                    "state is not rolling in seek()");
            m_read = media_time;
            m_pos0 = m_read - m_latency;
            m_queued = 0;
            m_t0 = get_system_usecs();
            m_Tf = 1.0;
            m_last = m_pos0;
            m_now_last = 0;
        }
    end:
        pthread_mutex_unlock(&m_mutex);
    }

    void TimeInterpolator::pause(bool flushing_fifo)
    {
        int64_t seek_to = -1;

        pthread_mutex_lock(&m_mutex);
        LOGV("%s()", __func__);
        if (flushing_fifo) {
            set_state(STOPPED, STOP);
            seek_to = m_read + m_queued;
        } else if (m_state == ROLLING) {
            set_state(PAUSED, PAUSE);
            m_read += m_queued;
            m_pos0 = m_last;
            m_t0 = get_system_usecs();
            m_queued = 0;
        }
        pthread_mutex_unlock(&m_mutex);
        if (seek_to >= 0) seek(seek_to);
    }
    /* Should only be called when in PAUSED state */
    void TimeInterpolator::resume()
    {
        pthread_mutex_lock(&m_mutex);
        if (m_state != PAUSED) {
            LOGE("Error: calling %s() when not in PAUSED state",
                 __func__);
        }
        m_t0 = get_system_usecs();
        m_Tf = 1.0;
        pthread_mutex_unlock(&m_mutex);
    }

    /* static function */
    int64_t TimeInterpolator::get_system_usecs()
    {
        struct timespec tv;
        clock_gettime(CLOCK_MONOTONIC, &tv);
        return ((int64_t)tv.tv_sec * 1000000L + tv.tv_nsec / 1000);
    }

    /* t = m_pos0 + (now - m_t0) * m_Tf
     */
    int64_t TimeInterpolator::get_stream_usecs()
    {
        int64_t now;
        double dt;
        int64_t t_media;
        pthread_mutex_lock(&m_mutex);

        now = get_system_usecs();

        if (m_state == PAUSED) {
            t_media = m_pos0;
            goto end;
        }

        dt = m_Tf * double(now - m_t0);
        if (dt < 0.0) dt = 0.0;
        t_media = m_pos0 + int64_t(dt);
        if (t_media < 0) {
            t_media = 0;
        }
        if (t_media < m_last) {
            LOGW("time is rewinding: %lld Tf=%g t0=%lld pos0=%lld dt=%g "
                 "now=%lld last=%lld now_last=%lld",
                 t_media - m_last, m_Tf, m_t0, m_pos0, dt,
                 now, m_last, m_now_last);
        }
        if (t_media >= read_pointer()) {
            if (m_state == ROLLING) {
                t_media = read_pointer();
                LOGE("UNDERRUN in %s", __func__);
                err_underrun();
            }
        }

        m_last = t_media;
        m_now_last = now;

    end:
        /* t_media += m_latency; */
        pthread_mutex_unlock(&m_mutex);
        LOGV("%s == %lld (t0=%lld, pos0=%lld, Tf=%g, read=%lld, queued=%lld "
             "latency=%lld now=%lld, ",
             __func__, t_media, m_t0, m_pos0, m_Tf, m_read, m_queued,
             m_latency, now);
        return t_media;
    }

    /* This is the TimeInterpolator algorithm.
     *
     * Let:
     *
     *     read := the read position of the audio data.
     *     t0  := the point that we measure time from in the
     *            current cycle (epoch).
     *     t1  := the next point that we will measure time from.
     *     pos0 := the media time corresponding to t0.
     *     pos1 := the media time corresponding to t1.
     *     pos1_desired := the ideal media time corresponding to
     *            t1, based on the current value of read.
     *     e   := the error between t1 and t1_desired.
     *     Tf  := the time factor (usecs per usec)
     *     latency := the size of all the FIFO's between here and
     *            the hardware.
     *
     * The formula we use to give timestamps is as follows:
     *
     *     t = pos0 + Tf * (now - t0)
     *
     * Where Tf is something close to 1.0 and now is the current time.
     *
     * When ROLLING, we want to update our formula parameters every
     * time data is pushed into the buffer (post_buffer() is called).
     * We start by evaluating t1 to the current system time:
     *
     *     t1 = get_system_usecs();
     *
     * ...this will become our next t0.
     *
     * We calculate pos1 based on the current system time.  That way
     * we stay monotonic:
     *
     *     pos1 = pos0 + Tf * (t1 - t0)
     *
     * However, in an ideal world, pos1 would have come out to be:
     *
     *     pos1_desired = read - latency
     *
     * Since we're being asked for more audio data, then we have a
     * pretty good indication of time time when the data pointed to by
     * `read` will be played:
     *
     *     t_read = t1 + latency
     *
     * So, since pos1 is going to be our new pos0... we want to pick a
     * Tf so that our time-lines intersect.  That is:
     *
     *     read = pos1 + Tf * (t_read - t1)
     *
     * Solving for Tf, we get:
     *
     *     (read - pos1) = Tf * (t_read - t1)
     *
     *     Tf = (read - pos1) / (t_read - t1)
     *
     * We know from the above formula that t_read - t1 = latency:
     *
     *     Tf = (read - pos1) / latency
     *
     * We also know that read = pos1_desired + latency:
     *
     *     Tf = (pos1_desired + latency - pos1) / latency
     *
     * So, we define an error value, e:
     *
     *     e := pos1 - pos1_desired
     *
     * And the formula reduces to:
     *
     *     Tf = 1.0 - (e / latency)
     *
     * Then we complete by advancing our time basis.
     *
     *     t0 = t1
     *     pos0 = pos1
     *
     * IMPLEMENTATION DETAILS:
     *
     * The time when post_buffer() is called is a good indication of
     * the timing of the PREVIOUS call to post_buffer().  We do not
     * have any good indication of the timings of the data posted by
     * frame_usecs, except to estimate when they will begin playing.
     * Therefore, the value of frame_usecs is stored in the member
     * variable m_queued and (typically) rendered on the next call to
     * post_buffer().
     *
     * If post_buffer() is called twice in quick succession, then the
     * data is aggregated instead of updating the epoch.
     */
    void TimeInterpolator::post_buffer(int64_t frame_usecs)
    {
        int64_t dt;                     /* The actual time since t0 */
        double e = 0;                   /* error value for our pos0 estimation */
        int64_t t1;                     /* now.  the next value of t0 */
        int64_t pos1;                   /* the next value of pos0 */
        int64_t pos1_desired;           /* the ideal next value of pos0 */
        bool aggregate_buffers = false; /* see below */
        bool set_Tf_to_unity = false;   /* In some state changes, m_Tf needs to be set to 1.0 */
        int64_t posted_this_time;       /* value of m_queued saved for debugging */

        pthread_mutex_lock(&m_mutex);

        /* Special logic for startup sequence/states */
        if (m_state != ROLLING) {
            if (m_state == PAUSED) {
                set_state(ROLLING, POST_BUFFER);
                set_Tf_to_unity = true;
            }

            if (m_state == STOPPED) {
                /* Setting the initial_offset to half the latency
                 * was found (by trial-and-error) to stabilize the
                 * TimeInterpolator within about 2-4 video frames.
                 */
                int64_t initial_offset = m_latency / 2;
                if (m_queued != 0) {
                    LOGW("TimeInterpolator state is PAUSED, but m_queued is "
                         "not 0 (actually %lld)", frame_usecs);
                }
                m_t0 = get_system_usecs();
                set_state(ROLLING, POST_BUFFER);
                m_read += frame_usecs;
                if (initial_offset < 40000) {
                    initial_offset = 40000;
                }
                m_pos0 = m_read - initial_offset;
                m_queued = 0;
                m_Tf = 1.0;
                goto end;
            }
        }

        t1 = get_system_usecs();
        dt = t1 - m_t0;

        if ((m_state == ROLLING) && (dt < (frame_usecs/4))) {
            /* See below for explanation */
            aggregate_buffers = true;
        }

        /* Main fillBuffer() logic */
        if (!aggregate_buffers) {
            /* this is the main algorithm */

            m_read += m_queued;
            pos1 = m_pos0 + m_Tf * dt;
            pos1_desired = m_read - m_latency;
            e = pos1 - pos1_desired;

            if ((pos1 < m_last) && (m_last > 0)) {
                /* This is ignored at the start of playback */
                LOGW("this cycle will cause a rewind pos1=%lld m_last=%lld pos-last=%lld",
                     pos1, m_last, (pos1-m_last));
            }
            if (set_Tf_to_unity) {
                e = pos1 - (m_read - m_latency);
                LOGV("%s set_Tf_to_unity e=%g (resetting to 0)", __func__, e);
                e = 0;
                m_Tf = 1.0;
            } else {
                m_Tf = 1.0 - (e / m_latency);
            }

            m_pos0 = pos1;
            m_t0 = t1;
            posted_this_time = m_queued;
            m_queued = frame_usecs;

            if (m_Tf >= 2.0) {
                m_Tf = 2.0;
                err_overrun();
            } else if (m_Tf < .5) {
                m_Tf = .5;
            }

            if (m_pos0 >= m_read) {
                LOGE("UNDERRUN in %s", __func__);
                err_underrun();
            }

            LOGV("TimeInterpolator updated: t0=%lld dt=%lld, Tf=%g pos0=%lld "
                 "read0=%lld m_queued=%lld posted_this=%lld latency=%lld e=%g "
                 "m_read-m_pos0=%lld t0_prev=%lld\n",
                 m_t0, dt, m_Tf, m_pos0,
                 m_read, m_queued, posted_this_time, m_latency, e,
                 (m_read-m_pos0), (m_t0 - dt));
        } else {
            /* If aggregate_buffers == true, then this call is very
             * close in time to the previous call.  Therefore we will
             * combine the data with the previous call(s) and treat
             * them as if they are one.
             */
            m_queued += frame_usecs;
        }
    end:
        pthread_mutex_unlock(&m_mutex);
    }

    /* mutex must already be locked */
    void TimeInterpolator::err_underrun()
    {
        LOGE("TimeInterpolator UNDERRUN detected");
        m_Tf = 0.0;
        m_read += m_queued;
        m_pos0 = m_read;
        m_queued = 0;
        set_state(STOPPED, ERR_UNDERRUN);
    }

    /* mutex must already be locked */
    void TimeInterpolator::err_overrun()
    {
        LOGE("TimeInterpolator OVERRUN detected");
        int64_t now = get_system_usecs();
        if (m_state == ROLLING) {
            /* abruptly advance time */
            m_pos0 = m_read - m_latency;
            m_t0 = get_system_usecs();
        }
    }

} /* namespace omap_enhancement */
#endif /*  defined(OMAP_ENHANCEMENT) && defined(OMAP_TIME_INTERPOLATOR) */

namespace android {

#ifdef OMAP_ENHANCEMENT //DOLBY_DDPDEC51_MULTICHANNEL

//map channel count to Android channel map descriptors
//This is not flexible enough to fully describe the complete set
//of channel mappings that we might see.
//The mapping chosen here will only be correct in the most oft used
//cases such as 5.1 and stereo. More obscure mappings such as 3.1
//will be misreprented here.
const int nChanMapTable[8] = {
                                 (AUDIO_CHANNEL_OUT_MONO), //1
                                 (AUDIO_CHANNEL_OUT_STEREO), //2
                                 (AUDIO_CHANNEL_OUT_STEREO | AUDIO_CHANNEL_OUT_FRONT_CENTER), //3
                                 (AUDIO_CHANNEL_OUT_QUAD), //4
                                 (AUDIO_CHANNEL_OUT_5POINT1 & (~AUDIO_CHANNEL_OUT_LOW_FREQUENCY)), //5
                                 (AUDIO_CHANNEL_OUT_5POINT1),//6
                                 (AUDIO_CHANNEL_OUT_7POINT1 & (~AUDIO_CHANNEL_OUT_LOW_FREQUENCY)), //7
                                 (AUDIO_CHANNEL_OUT_7POINT1) //8
                             };

struct AudioPlayerEvent : public TimedEventQueue::Event {
    AudioPlayerEvent(
            AudioPlayer *player,
            void (AudioPlayer::*method)())
        : mAudioPlayer(player),
          mMethod(method) {
    }

protected:
    virtual ~AudioPlayerEvent() {}

    virtual void fire(TimedEventQueue *queue, int64_t /* now_us */) {
        (mAudioPlayer->*mMethod)();
    }

private:
    AudioPlayer *mAudioPlayer;
    void (AudioPlayer::*mMethod)();

    AudioPlayerEvent(const AudioPlayerEvent &);
    AudioPlayerEvent &operator=(const AudioPlayerEvent &);
};
#endif

AudioPlayer::AudioPlayer(
        const sp<MediaPlayerBase::AudioSink> &audioSink,
        AwesomePlayer *observer)
    : mAudioTrack(NULL),
      mInputBuffer(NULL),
      mSampleRate(0),
      mLatencyUs(0),
      mFrameSize(0),
      mNumFramesPlayed(0),
      mPositionTimeMediaUs(-1),
      mPositionTimeRealUs(-1),
      mSeeking(false),
      mReachedEOS(false),
      mFinalStatus(OK),
      mStarted(false),
      mIsFirstBuffer(false),
      mFirstBufferResult(OK),
      mFirstBuffer(NULL),
      mAudioSink(audioSink),
#ifdef QCOM_HARDWARE
      mSourcePaused(false),
#endif
      mObserver(observer) {
#if defined(OMAP_ENHANCEMENT) && defined(OMAP_TIME_INTERPOLATOR)
    mRealTimeInterpolator = new omap_enhancement::TimeInterpolator;
#endif

#ifdef OMAP_ENHANCEMENT //DOLBY_DDPDEC51_MULTICHANNEL
    mPortSettingsChangedEvent = new AudioPlayerEvent(this, &AudioPlayer::onPortSettingsChangedEvent);
    mPortSettingsChangedEventPending = false;
    mQueueStarted = false;
#endif
}

AudioPlayer::~AudioPlayer() {
    if (mStarted) {
        reset();
    }
#if defined(OMAP_ENHANCEMENT) && defined(OMAP_TIME_INTERPOLATOR)
    delete mRealTimeInterpolator;
#endif

#ifdef OMAP_ENHANCEMENT //DOLBY_DDPDEC51_MULTICHANNEL
    if (mQueueStarted) {
        mQueue.stop();
    }
#endif
}

void AudioPlayer::setSource(const sp<MediaSource> &source) {
    CHECK(mSource == NULL);
    mSource = source;
}

status_t AudioPlayer::start(bool sourceAlreadyStarted) {
    CHECK(!mStarted);
    CHECK(mSource != NULL);

    status_t err;
    if (!sourceAlreadyStarted) {
#ifdef QCOM_HARDWARE
        mSourcePaused = false;
#endif
        err = mSource->start();

        if (err != OK) {
            return err;
        }
    }

#ifdef OMAP_ENHANCEMENT //DOLBY_DDPDEC51_MULTICHANNEL
    if (!mQueueStarted) {
        mQueue.start();
        mQueueStarted = true;
    }
#endif

    // We allow an optional INFO_FORMAT_CHANGED at the very beginning
    // of playback, if there is one, getFormat below will retrieve the
    // updated format, if there isn't, we'll stash away the valid buffer
    // of data to be used on the first audio callback.

    CHECK(mFirstBuffer == NULL);

    MediaSource::ReadOptions options;
    if (mSeeking) {
        options.setSeekTo(mSeekTimeUs);
        mSeeking = false;
    }

    mFirstBufferResult = mSource->read(&mFirstBuffer, &options);
    if (mFirstBufferResult == INFO_FORMAT_CHANGED) {
        LOGV("INFO_FORMAT_CHANGED!!!");

        CHECK(mFirstBuffer == NULL);
        mFirstBufferResult = OK;
        mIsFirstBuffer = false;
    } else {
        mIsFirstBuffer = true;
    }

    sp<MetaData> format = mSource->getFormat();
    const char *mime;
    bool success = format->findCString(kKeyMIMEType, &mime);
    CHECK(success);
    CHECK(!strcasecmp(mime, MEDIA_MIMETYPE_AUDIO_RAW));

    success = format->findInt32(kKeySampleRate, &mSampleRate);
    CHECK(success);

    int32_t numChannels;
    success = format->findInt32(kKeyChannelCount, &numChannels);
    CHECK(success);

    if (mAudioSink.get() != NULL) {
        status_t err = mAudioSink->open(
                mSampleRate, numChannels, AUDIO_FORMAT_PCM_16_BIT,
                DEFAULT_AUDIOSINK_BUFFERCOUNT,
                &AudioPlayer::AudioSinkCallback, this);
        if (err != OK) {
            if (mFirstBuffer != NULL) {
                mFirstBuffer->release();
                mFirstBuffer = NULL;
            }

            if (!sourceAlreadyStarted) {
                mSource->stop();
            }

            return err;
        }

        mLatencyUs = (int64_t)mAudioSink->latency() * 1000;
        mFrameSize = mAudioSink->frameSize();

        mAudioSink->start();
    } else {
#ifdef OMAP_ENHANCEMENT //DOLBY_DDPDEC51_MULTICHANNEL
        int channels = 0;
        int outputflag = 0;

        channels |= nChanMapTable[numChannels-1];
        LOGI("Channel Map:%x", channels);

        // Direct output enabled for more than 2 channels
        // RJ - We could use some other rule here, such as, if endpoint=HDMI
        // as we might want to use directout for 2 channels over HDMI for example
        if (numChannels > 2) {
            outputflag |= AUDIO_POLICY_OUTPUT_FLAG_DIRECT;
            LOGI("channelCount >2 -> AUDIO_POLICY_OUTPUT_FLAG_DIRECT");
        }
        mAudioTrack = new AudioTrack(
               AUDIO_STREAM_MUSIC, mSampleRate, AUDIO_FORMAT_PCM_16_BIT,
                channels, 0, outputflag, &AudioCallback, this, 0);
#else
        mAudioTrack = new AudioTrack(
                AUDIO_STREAM_MUSIC, mSampleRate, AUDIO_FORMAT_PCM_16_BIT,
                (numChannels == 2)
                    ? AUDIO_CHANNEL_OUT_STEREO
                    : AUDIO_CHANNEL_OUT_MONO,
                0, 0, &AudioCallback, this, 0);
#endif

        if ((err = mAudioTrack->initCheck()) != OK) {
            delete mAudioTrack;
            mAudioTrack = NULL;

            if (mFirstBuffer != NULL) {
                mFirstBuffer->release();
                mFirstBuffer = NULL;
            }

            if (!sourceAlreadyStarted) {
                mSource->stop();
            }

            return err;
        }

        mLatencyUs = (int64_t)mAudioTrack->latency() * 1000;
        mFrameSize = mAudioTrack->frameSize();

        mAudioTrack->start();
    }

#if defined(OMAP_ENHANCEMENT) && defined(OMAP_TIME_INTERPOLATOR)
    LOGI("mLatencyUs = %lld", mLatencyUs);
    mRealTimeInterpolator->set_latency(mLatencyUs);
#endif
    mStarted = true;

    return OK;
}

void AudioPlayer::pause(bool playPendingSamples) {
    CHECK(mStarted);

    if (playPendingSamples) {
        if (mAudioSink.get() != NULL) {
            mAudioSink->stop();
        } else {
            mAudioTrack->stop();
        }

        mNumFramesPlayed = 0;
    } else {
        if (mAudioSink.get() != NULL) {
            mAudioSink->pause();
        } else {
            mAudioTrack->pause();
        }
    }
#ifdef QCOM_HARDWARE
    CHECK(mSource != NULL);
    if (mSource->pause() == OK) {
        mSourcePaused = true;
    }
#endif
#if defined(OMAP_ENHANCEMENT) && defined(OMAP_TIME_INTERPOLATOR)
    if (playPendingSamples) {
        mRealTimeInterpolator->stop();
    } else {
        mRealTimeInterpolator->pause();
    }
#endif
}

void AudioPlayer::resume() {
    CHECK(mStarted);
#ifdef QCOM_HARDWARE
    CHECK(mSource != NULL);
    if (mSourcePaused == true) {
        mSourcePaused = false;
        mSource->start();
    }
#endif

#if defined(OMAP_ENHANCEMENT) && defined(OMAP_TIME_INTERPOLATOR)
    mRealTimeInterpolator->resume();
#endif
    if (mAudioSink.get() != NULL) {
        mAudioSink->start();
    } else {
        mAudioTrack->start();
    }
}

void AudioPlayer::reset() {
    CHECK(mStarted);

    if (mAudioSink.get() != NULL) {
        mAudioSink->stop();
        mAudioSink->close();
    } else {
        mAudioTrack->stop();

        delete mAudioTrack;
        mAudioTrack = NULL;
    }

    // Make sure to release any buffer we hold onto so that the
    // source is able to stop().

    if (mFirstBuffer != NULL) {
        mFirstBuffer->release();
        mFirstBuffer = NULL;
    }

    if (mInputBuffer != NULL) {
        LOGV("AudioPlayer releasing input buffer.");

        mInputBuffer->release();
        mInputBuffer = NULL;
    }
#ifdef QCOM_HARDWARE
    mSourcePaused = false;
#endif
    mSource->stop();

    // The following hack is necessary to ensure that the OMX
    // component is completely released by the time we may try
    // to instantiate it again.
    wp<MediaSource> tmp = mSource;
    mSource.clear();
    while (tmp.promote() != NULL) {
        usleep(1000);
    }
    IPCThreadState::self()->flushCommands();

    mNumFramesPlayed = 0;
    mPositionTimeMediaUs = -1;
    mPositionTimeRealUs = -1;
    mSeeking = false;
    mReachedEOS = false;
    mFinalStatus = OK;
    mStarted = false;
#if defined(OMAP_ENHANCEMENT) && defined(OMAP_TIME_INTERPOLATOR)
    mRealTimeInterpolator->reset();
#endif
}

// static
void AudioPlayer::AudioCallback(int event, void *user, void *info) {
    static_cast<AudioPlayer *>(user)->AudioCallback(event, info);
}

bool AudioPlayer::isSeeking() {
    Mutex::Autolock autoLock(mLock);
    return mSeeking;
}

bool AudioPlayer::reachedEOS(status_t *finalStatus) {
    *finalStatus = OK;

    Mutex::Autolock autoLock(mLock);
    *finalStatus = mFinalStatus;
    return mReachedEOS;
}

// static
size_t AudioPlayer::AudioSinkCallback(
        MediaPlayerBase::AudioSink *audioSink,
        void *buffer, size_t size, void *cookie) {
    AudioPlayer *me = (AudioPlayer *)cookie;

    return me->fillBuffer(buffer, size);
}

void AudioPlayer::AudioCallback(int event, void *info) {
    if (event != AudioTrack::EVENT_MORE_DATA) {
        return;
    }

    AudioTrack::Buffer *buffer = (AudioTrack::Buffer *)info;
    size_t numBytesWritten = fillBuffer(buffer->raw, buffer->size);

    buffer->size = numBytesWritten;
}

#ifdef OMAP_ENHANCEMENT //DOLBY_DDPDEC51_MULTICHANNEL
void AudioPlayer::onPortSettingsChangedEvent() {
    LOGV("Port Settings Changed!");
    status_t err = OK;
    sp<MetaData> format;
    bool success;

    Mutex::Autolock autoLock(mLock);

    if (!mPortSettingsChangedEventPending) {
        goto onPortSettingsChangedEvent_exit;
    }

    // close exisiting playback
    if (mAudioSink.get() != NULL) {
        mAudioSink->stop();
        mAudioSink->close();
    } else {
        mAudioTrack->stop();
        delete mAudioTrack;
        mAudioTrack = NULL;
    }

    // open new
    format = mSource->getFormat();
    const char *mime;
    success = format->findCString(kKeyMIMEType, &mime);
    CHECK(success);
    CHECK(!strcasecmp(mime, MEDIA_MIMETYPE_AUDIO_RAW));

    success = format->findInt32(kKeySampleRate, &mSampleRate);
    CHECK(success);

    int32_t numChannels;
    success = format->findInt32(kKeyChannelCount, &numChannels);
    CHECK(success);

    LOGV("Updated sample rate: %d, channels: %d", mSampleRate, numChannels);

    if (mAudioSink.get() != NULL) {
        err = mAudioSink->open(
                mSampleRate, numChannels, AUDIO_FORMAT_PCM_16_BIT,
                DEFAULT_AUDIOSINK_BUFFERCOUNT,
                &AudioPlayer::AudioSinkCallback, this);
        if (err != OK) {
            LOGE("mAudioSink->open error : %d", err);
            goto onPortSettingsChangedEvent_exit;
        }

        mLatencyUs = (int64_t)mAudioSink->latency() * 1000;
        mFrameSize = mAudioSink->frameSize();

        mAudioSink->start();
    } else {
        int channels = 0;
        int outputflag = 0;

        channels |= nChanMapTable[numChannels-1];
        LOGV("Channel Map: %x", channels);

        // Direct output enabled for more than 2 channels
        // RJ - We could use some other rule here, such as, if endpoint=HDMI
        // as we might want to use directout for 2 channels over HDMI for example
        if (numChannels > 2) {
            outputflag |= AUDIO_POLICY_OUTPUT_FLAG_DIRECT;
            LOGV("channelCount >2 -> AUDIO_POLICY_OUTPUT_FLAG_DIRECT");
        }

        mAudioTrack = new AudioTrack(
                AUDIO_STREAM_MUSIC, mSampleRate, AUDIO_FORMAT_PCM_16_BIT,
                channels, 0, outputflag, &AudioCallback, this, 0);

        if ((err = mAudioTrack->initCheck()) != OK) {
            LOGE("AudioTrack error : %d", err);
            delete mAudioTrack;
            mAudioTrack = NULL;

            goto onPortSettingsChangedEvent_exit;
        }

        mLatencyUs = (int64_t)mAudioTrack->latency() * 1000;
        mFrameSize = mAudioTrack->frameSize();
        mAudioTrack->start();
    }

    mPortSettingsChangedEventPending = false;

onPortSettingsChangedEvent_exit:

    if (err != OK) {
        if (mObserver && !mReachedEOS)
            mObserver->postAudioEOS();

        mReachedEOS = true;
        mFinalStatus = err;
    }

    return;
}
#endif

uint32_t AudioPlayer::getNumFramesPendingPlayout() const {
    uint32_t numFramesPlayedOut;
    status_t err;

    if (mAudioSink != NULL) {
        err = mAudioSink->getPosition(&numFramesPlayedOut);
    } else {
        err = mAudioTrack->getPosition(&numFramesPlayedOut);
    }

    if (err != OK || mNumFramesPlayed < numFramesPlayedOut) {
        return 0;
    }

    // mNumFramesPlayed is the number of frames submitted
    // to the audio sink for playback, but not all of them
    // may have played out by now.
    return mNumFramesPlayed - numFramesPlayedOut;
}

size_t AudioPlayer::fillBuffer(void *data, size_t size) {
    if (mNumFramesPlayed == 0) {
        LOGV("AudioCallback");
    }

    if (mReachedEOS) {
        return 0;
    }

    bool postSeekComplete = false;
    bool postEOS = false;
    int64_t postEOSDelayUs = 0;

#if defined(OMAP_ENHANCEMENT) && defined(OMAP_TIME_INTERPOLATOR)
    mRealTimeInterpolator->post_buffer( ((size / mFrameSize) * 1000000) / mSampleRate );
    mPositionTimeRealUs = mRealTimeInterpolator->get_stream_usecs();
#endif // defined(OMAP_ENHANCEMENT) && defined(OMAP_TIME_INTERPOLATOR)

#ifdef OMAP_ENHANCEMENT //DOLBY_DDPDEC51_MULTICHANNEL
    if (true == mPortSettingsChangedEventPending) {
        LOGV("Waiting for reconfig to finish... filling zeros");
        memset(data, 0, size);
        return size;
    }
#endif

    size_t size_done = 0;
    size_t size_remaining = size;
    while (size_remaining > 0) {
        MediaSource::ReadOptions options;

        {
            Mutex::Autolock autoLock(mLock);

            if (mSeeking) {
                if (mIsFirstBuffer) {
                    if (mFirstBuffer != NULL) {
                        mFirstBuffer->release();
                        mFirstBuffer = NULL;
                    }
                    mIsFirstBuffer = false;
                }

                options.setSeekTo(mSeekTimeUs);

                if (mInputBuffer != NULL) {
                    mInputBuffer->release();
                    mInputBuffer = NULL;
                }

                mSeeking = false;
                if (mObserver) {
                    postSeekComplete = true;
                }
            }
        }

        if (mInputBuffer == NULL) {
            status_t err;

            if (mIsFirstBuffer) {
                mInputBuffer = mFirstBuffer;
                mFirstBuffer = NULL;
                err = mFirstBufferResult;

                mIsFirstBuffer = false;
            } else {
                err = mSource->read(&mInputBuffer, &options);
            }

#ifdef OMAP_ENHANCEMENT //DOLBY_DDPDEC51_MULTICHANNEL
            if (err == INFO_FORMAT_CHANGED) {
                LOGI("INFO_FORMAT_CHANGED - pending port settings changed = true");
                mPortSettingsChangedEventPending = true;

                break;
            }
#endif

            CHECK((err == OK && mInputBuffer != NULL)
                   || (err != OK && mInputBuffer == NULL));

            Mutex::Autolock autoLock(mLock);

            if (err != OK) {
                if (mObserver && !mReachedEOS) {
                    // We don't want to post EOS right away but only
                    // after all frames have actually been played out.

                    // These are the number of frames submitted to the
                    // AudioTrack that you haven't heard yet.
                    uint32_t numFramesPendingPlayout =
                        getNumFramesPendingPlayout();

                    // These are the number of frames we're going to
                    // submit to the AudioTrack by returning from this
                    // callback.
                    uint32_t numAdditionalFrames = size_done / mFrameSize;

                    numFramesPendingPlayout += numAdditionalFrames;

                    int64_t timeToCompletionUs =
                        (1000000ll * numFramesPendingPlayout) / mSampleRate;

                    LOGV("total number of frames played: %lld (%lld us)",
                            (mNumFramesPlayed + numAdditionalFrames),
                            1000000ll * (mNumFramesPlayed + numAdditionalFrames)
                                / mSampleRate);

                    LOGV("%d frames left to play, %lld us (%.2f secs)",
                         numFramesPendingPlayout,
                         timeToCompletionUs, timeToCompletionUs / 1E6);

                    postEOS = true;
                    postEOSDelayUs = timeToCompletionUs + mLatencyUs;
                }

                mReachedEOS = true;
                mFinalStatus = err;
                break;
            }

            if (mAudioSink != NULL) {
                mLatencyUs = (int64_t)mAudioSink->latency() * 1000;
            } else {
                mLatencyUs = (int64_t)mAudioTrack->latency() * 1000;
            }

            CHECK(mInputBuffer->meta_data()->findInt64(
                        kKeyTime, &mPositionTimeMediaUs));

#if !(defined(OMAP_ENHANCEMENT) && defined(OMAP_TIME_INTERPOLATOR))
            mPositionTimeRealUs =
                ((mNumFramesPlayed + size_done / mFrameSize) * 1000000)
                    / mSampleRate;
#endif

            LOGV("buffer->size() = %d, "
                 "mPositionTimeMediaUs=%.2f mPositionTimeRealUs=%.2f",
                 mInputBuffer->range_length(),
                 mPositionTimeMediaUs / 1E6, mPositionTimeRealUs / 1E6);
        }

        if (mInputBuffer->range_length() == 0) {
            mInputBuffer->release();
            mInputBuffer = NULL;

            continue;
        }

        size_t copy = size_remaining;
        if (copy > mInputBuffer->range_length()) {
            copy = mInputBuffer->range_length();
        }

        memcpy((char *)data + size_done,
               (const char *)mInputBuffer->data() + mInputBuffer->range_offset(),
               copy);

        mInputBuffer->set_range(mInputBuffer->range_offset() + copy,
                                mInputBuffer->range_length() - copy);

        size_done += copy;
        size_remaining -= copy;
    }

    {
        Mutex::Autolock autoLock(mLock);
        mNumFramesPlayed += size_done / mFrameSize;
    }

#ifdef OMAP_ENHANCEMENT //DOLBY_DDPDEC51_MULTICHANNEL
        if (mPortSettingsChangedEventPending) {
            LOGV("portsettingschangedevent received");
            mQueue.postEvent(mPortSettingsChangedEvent);
            return size_done;
        }
#endif

    if (postEOS) {
        mObserver->postAudioEOS(postEOSDelayUs);
    }

    if (postSeekComplete) {
        mObserver->postAudioSeekComplete();
    }

    return size_done;
}

int64_t AudioPlayer::getRealTimeUs() {
    Mutex::Autolock autoLock(mLock);
#if defined(OMAP_ENHANCEMENT) && defined(OMAP_TIME_INTERPOLATOR)
    return mRealTimeInterpolator->get_stream_usecs();
#else
    return getRealTimeUsLocked();
#endif
}

int64_t AudioPlayer::getRealTimeUsLocked() const {
    CHECK(mStarted);
    CHECK_NE(mSampleRate, 0);
    return -mLatencyUs + (mNumFramesPlayed * 1000000) / mSampleRate;
}

int64_t AudioPlayer::getMediaTimeUs() {
    Mutex::Autolock autoLock(mLock);

    if (mPositionTimeMediaUs < 0 || mPositionTimeRealUs < 0) {
        if (mSeeking) {
            return mSeekTimeUs;
        }

        return 0;
    }

    int64_t realTimeOffset = getRealTimeUsLocked() - mPositionTimeRealUs;
    if (realTimeOffset < 0) {
        realTimeOffset = 0;
    }

    return mPositionTimeMediaUs + realTimeOffset;
}

#if defined(OMAP_ENHANCEMENT) && defined(OMAP_TIME_INTERPOLATOR)
int64_t AudioPlayer::latency() const
{
    return mLatencyUs;
}
#endif

bool AudioPlayer::getMediaTimeMapping(
        int64_t *realtime_us, int64_t *mediatime_us) {
    Mutex::Autolock autoLock(mLock);

#if defined(OMAP_ENHANCEMENT) && defined(OMAP_TIME_INTERPOLATOR)
    /* AwesomePlayer wants to make sure that the read pointer in
     * the codec is close to what the buffer "read pointer" is.
     */
    *realtime_us = mRealTimeInterpolator->read_pointer();
#else
    *realtime_us = mPositionTimeRealUs;
#endif
    *mediatime_us = mPositionTimeMediaUs;

    return mPositionTimeRealUs != -1 && mPositionTimeMediaUs != -1;
}

status_t AudioPlayer::seekTo(int64_t time_us) {
    Mutex::Autolock autoLock(mLock);

    mSeeking = true;
    mPositionTimeRealUs = mPositionTimeMediaUs = -1;
    mReachedEOS = false;
    mSeekTimeUs = time_us;
#if defined(OMAP_ENHANCEMENT) && defined(OMAP_TIME_INTERPOLATOR)
    mRealTimeInterpolator->seek(time_us);
#endif

    // Flush resets the number of played frames
    mNumFramesPlayed = 0;

    if (mAudioSink != NULL) {
        mAudioSink->flush();
    } else {
        mAudioTrack->flush();
    }

    return OK;
}

}
