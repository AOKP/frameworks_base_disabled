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


#ifndef _OMAP_LAYERSCREENSHOT_H_
#define _OMAP_LAYERSCREENSHOT_H_

#include "LayerScreenshot.h"
#include <ui/S3DFormat.h>

namespace android {

// ---------------------------------------------------------------------------
class S3DSurfaceFlinger;

// ---------------------------------------------------------------------------
class OmapLayerScreenshot : public LayerScreenshot
{
public:
    OmapLayerScreenshot(S3DSurfaceFlinger* flinger, DisplayID display,
                    const sp<Client>& client, S3DLayoutType type, S3DLayoutOrder ordering);

    //Overriden from LayerScreenshot
    virtual const char* getTypeId() const { return "OmapLayerScreenshot"; }
    virtual uint32_t doTransaction(uint32_t flags);
    virtual void onRemoved();
    virtual void drawRegion(const Region& clip, int hw_w, int hw_h) const;

    inline bool isS3D() const { return mType != eMono; }

private:
    void drawS3DRegion(const Region& clip, int hw_w, int hw_h) const;

    S3DSurfaceFlinger *mFlingerS3D;
    S3DLayoutType mType;
    S3DLayoutOrder mViewOrder;
};

};

#endif //_OMAP_LAYERSCREENSHOT_H_