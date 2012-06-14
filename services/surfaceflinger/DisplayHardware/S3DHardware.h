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

#ifndef _S3DHARDWARE_H_
#define _S3DHARDWARE_H_

#include <stdint.h>
#include <sys/types.h>
#include <utils/Errors.h>

#include <ui/S3DFormat.h>

namespace android {

class S3DHardware
{
public:

    S3DHardware();

    status_t initCheck() const;
    status_t enableS3D(bool state);

    inline S3DLayoutType getType() const { return mType; }
    inline S3DLayoutOrder getOrdering() const { return mOrder; }
    inline bool isSwitcheable() const { return mSwitcheable; }

    inline bool isInterleaved() const { return (mType & (eRowInterleaved | eColInterleaved));}

    void forceConfiguration(S3DLayoutType type, S3DLayoutOrder order, bool switcheable);

private:
    void init();
    S3DLayoutType mType;
    S3DLayoutOrder mOrder;
    bool mSwitcheable;

    bool mCurrentState;
    int32_t s3d_w;
    int32_t s3d_h;
};

}; // namespace android

#endif // _S3DHARDWARE_H_