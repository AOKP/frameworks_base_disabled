/* ====================================================================
*   Copyright (C) 2012 Texas Instruments Incorporated
*
*   All rights reserved.
* ==================================================================== */


#ifndef MPEG2TS_RTP_WRITER_H_

#define MPEG2TS_RTP_WRITER_H_

#include <stdio.h>
#include <sys/types.h>
#include <sys/time.h>


#include <media/stagefright/foundation/ABase.h>
#include <media/stagefright/foundation/AHandlerReflector.h>
#include <media/stagefright/foundation/ALooper.h>
#include <media/stagefright/foundation/base64.h>
#include <media/stagefright/MediaWriter.h>
#include <media/stagefright/MediaBuffer.h>

#include <arpa/inet.h>
#include <sys/socket.h>


namespace android {

struct ABuffer;

struct MPEG2TSRTPWriter : public MediaWriter {
    MPEG2TSRTPWriter(int fd);
    MPEG2TSRTPWriter(const char *filename);

    virtual status_t addSource(const sp<MediaSource> &source);
    virtual status_t start(MetaData *param = NULL);
    virtual status_t stop();
    virtual status_t pause();
    virtual bool reachedEOS();
    virtual status_t dump(int fd, const Vector<String16>& args);

    void onMessageReceived(const sp<AMessage> &msg);

protected:
    virtual ~MPEG2TSRTPWriter();

private:
    enum {
        kWhatSourceNotify = 'noti' ,
	  kWhatSendSR = 'sr  ',
    };

    struct SourceInfo;

    FILE *mFile;
    sp<ALooper> mLooper;
    sp<AHandlerReflector<MPEG2TSRTPWriter> > mReflector;

    bool mStarted;
    bool mPaused;
    bool mDiscontinuityIndicator;
    bool mIgnoreFirstPacket;

    Vector<sp<SourceInfo> > mSources;
    size_t mNumSourcesDone;

    int64_t mNumTSPacketsWritten;
    int64_t mNumTSPacketsBeforeMeta;

#if LOG_TO_FILES
		int mRTPFd;
		int mRTCPFd;
#endif


    int mSocket;
    struct sockaddr_in mRTPAddr;
    struct sockaddr_in mRTCPAddr;

    AString mProfileLevel;
    AString mSeqParamSet;
    AString mPicParamSet;

    uint32_t mSourceID;
    uint32_t mSeqNo;
    uint32_t mRTPTimeBase;
    uint32_t mNumRTPSent;
    uint32_t mNumRTPOctetsSent;
    uint32_t mLastRTPTime;
    uint64_t mLastNTPTime;

    int32_t mNumSRsSent;

    int64_t mStartTime;
    int32_t mBitRate;
    uint32_t mPATContinuityCounter;
    uint32_t mPMTContinuityCounter;
    uint32_t mPCRContinuityCounter;

    int64_t mAudioDrift;

    char *mSinkIPAddr;
    int32_t mSinkAVRtpPort;

    int64_t mStartTimestampUs;

    Mutex mLock;

    enum {
		   INVALID,
		   H264,
		   AAC,
		   LPCM,
    } mMode;


    void init();

    void writeTS();
    void writeProgramAssociationTable();
    void writeProgramMap();
    void writePCRAccessUnit(int64_t pcr_clock);
    uint8_t addPCRAdaptationField(uint8_t *data, int64_t PCR_base, int64_t PCR_ext, uint8_t adaptationStuffBytes);
    uint8_t addDummyAdaptationField(uint8_t *data, uint8_t stuffBytes);
    size_t addPESHeader(uint8_t *data, unsigned int stream_id, size_t PES_packet_length, int64_t PTS);
    void writeAccessUnit(int32_t sourceIndex, const sp<ABuffer> &buffer);

    void writeMPEG2TSToFile(uint8_t *data, size_t size);

    static uint64_t GetNowNTP();
    void onRead(const sp<AMessage> &msg);
    void onSendSR(const sp<AMessage> &msg);

    void addSR(const sp<ABuffer> &buffer);
    void addSDES(const sp<ABuffer> &buffer);

    void makeH264SPropParamSets(MediaBuffer *buffer);
    void dumpSessionDesc();

    void sendBye();

    void send(const sp<ABuffer> &buffer, bool isRTCP);

    void sendMPEG2TSData(sp<ABuffer> &rtpBuffer,  int64_t timestamp = 0);


    void initializeCRCTable();

    unsigned int calculateCRC(unsigned char *ptrSection, unsigned int length );

    DISALLOW_EVIL_CONSTRUCTORS(MPEG2TSRTPWriter);
};

}  // namespace android

#endif  // MPEG2TS_RTP_WRITER_H_
