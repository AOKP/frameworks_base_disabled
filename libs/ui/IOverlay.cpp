/*
 * Copyright (C) 2007 The Android Open Source Project
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

#include <stdio.h>
#include <stdint.h>
#include <sys/types.h>

#include <binder/Parcel.h>
#include <binder/IInterface.h>

#include <ui/IOverlay.h>

namespace android {

enum {
    DESTROY = IBinder::FIRST_CALL_TRANSACTION, // one-way transaction
};

class BpOverlay : public BpInterface<IOverlay>
{
public:
    BpOverlay(const sp<IBinder>& impl)
        : BpInterface<IOverlay>(impl)
    {
    }

    virtual void destroy()
    {
        Parcel data, reply;
        data.writeInterfaceToken(IOverlay::getInterfaceDescriptor());
        remote()->transact(DESTROY, data, &reply); // Make this call synchronous
    }
};

IMPLEMENT_META_INTERFACE(Overlay, "android.ui.IOverlay");

// ----------------------------------------------------------------------

status_t BnOverlay::onTransact(
    uint32_t code, const Parcel& data, Parcel* reply, uint32_t flags)
{
    switch(code) {
        case DESTROY: {
            CHECK_INTERFACE(IOverlay, data, reply);
            destroy();
            return NO_ERROR;
        } break;
        default:
            return BBinder::onTransact(code, data, reply, flags);
    }
}

}; // namespace android
