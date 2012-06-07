/*
 * Copyright (C) Texas Instruments - http://www.ti.com/
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

#ifndef APE_TAG_H_

#define APE_TAG_H_

#include <utils/RefBase.h>
#include <media/stagefright/DataSource.h>
#include <media/stagefright/MetaData.h>

namespace android {

class APE{
public:
    APE();
    ~APE();
    bool isAPE(uint8_t *apeTag) const;
    bool parceAPE(const sp<DataSource> &source, off64_t offset,
            size_t* headerSize, sp<MetaData> &meta);

private:
    uint32_t itemNumber;
    uint32_t itemFlags;
    size_t lenValue;
};

} //namespace android

#endif //APE_TAG_H_
