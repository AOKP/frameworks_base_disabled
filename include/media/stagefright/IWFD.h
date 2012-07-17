/* ====================================================================
*   Copyright (C) 2012 Texas Instruments Incorporated
*
*   All rights reserved.
* ==================================================================== */

#ifndef ANDROID_IWFD_H
#define ANDROID_IWFD_H

typedef struct {
    int cookie;
    int w;
    int h;
    int fps;
    int secure;
    int mode;
} wfd_config;

enum {
    WFD_DISCONNECTED,
    WFD_CONNECTED,
    WFD_SETUP_COMPLETE,
    WFD_TEARDOWN_COMPLETE,
    WFD_MODE_CHANGED,
};

enum {
    WFD_MODE_DISABLED = 0x0,
    WFD_MODE_CLONING = 0x1,
    WFD_MODE_DOCKING = 0x2,
};



#ifdef __cplusplus

#include <utils/Errors.h>
#include <utils/KeyedVector.h>
#include <utils/RefBase.h>
#include <utils/String8.h>
#include <binder/IInterface.h>
#include <binder/Parcel.h>



namespace android {

class IWFDClient;

class IWFD: public IInterface
{
public:
    DECLARE_META_INTERFACE(WFD);

    virtual status_t         prepare() = 0;
    virtual status_t         release() = 0;
    virtual status_t         setParameter(const String8& key, const String8& value)  = 0;
    virtual status_t         getWFDDescriptor(Parcel *reply) = 0;
    virtual status_t         setupWFD(wfd_config *cfg) = 0;
    virtual status_t         teardownWFD() = 0;
    virtual status_t         setListener(const sp<IWFDClient>& listener) = 0;

};

// ----------------------------------------------------------------------------

class BnWFD: public BnInterface<IWFD>
{
public:
    virtual status_t    onTransact( uint32_t code,
                                    const Parcel& data,
                                    Parcel* reply,
                                    uint32_t flags = 0);
};

}; // namespace android
#else

#endif


#endif // ANDROID_IWFD_H
