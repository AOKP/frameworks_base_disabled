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


#ifndef _OMAPLAYER_H_
#define _OMAPLAYER_H_

#include "Layer.h"
#include <ui/S3DFormat.h>

namespace android {

// ---------------------------------------------------------------------------
class S3DSurfaceFlinger;

// ---------------------------------------------------------------------------
class OmapLayer : public Layer
{
public:
    OmapLayer(S3DSurfaceFlinger* flinger, DisplayID display,
                    const sp<Client>& client);

    //Overriden from Layer
    virtual const char* getTypeId() const { return "OmapLayer"; }
    virtual void onRemoved();
    virtual void setGeometry(hwc_layer_t* hwcl);
    virtual void lockPageFlip(bool& recomputeVisibleRegions);

    void setConfig(S3DLayoutType type, S3DLayoutOrder order, S3DRenderMode mode);
    inline bool isS3D() const { return mType != eMono; }

    inline bool isDrawingFirstHalf(S3DRenderMode mode) const {
        return mViewOrder == static_cast<S3DLayoutOrder>(mode);
    }

private:
    //This is overriden from LayerBase
    virtual void drawWithOpenGL(const Region& clip) const;
    //We need to do our own custom drawing here
    void drawWithOpenGL(const Region& clip, bool drawFirstHalf) const;

    S3DSurfaceFlinger *mFlingerS3D;
    S3DLayoutType mType;
    S3DLayoutOrder mViewOrder;
    S3DRenderMode mRenderMode;
};

};

#endif //_OMAPLAYER_H_