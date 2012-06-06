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

#ifndef _S3DSURFACEFLINGER_H_
#define _S3DSURFACEFLINGER_H_

#include "SurfaceFlinger.h"
#include "OmapLayer.h"
#include "OmapLayerScreenshot.h"
#include "DisplayHardware/S3DHardware.h"

#include <ui/S3DFormat.h>

namespace android {

// ---------------------------------------------------------------------------

class StencilMask
{
public:
    StencilMask();
    ~StencilMask();

    enum InterleavingType {
        NONE,
        COLUMN = eColInterleaved,
        ROW    = eRowInterleaved
    };
    void setupCoords(int w, int h, InterleavingType type);
    void drawStencil(int w, int h);
    void configStencilOp(bool drawEvenLines);
    void reset();

private:
    GLuint mVboId;
    int mCount;
    InterleavingType mType;
};

// ---------------------------------------------------------------------------
class S3DSurfaceFlinger : public SurfaceFlinger
{
public:
    S3DSurfaceFlinger();

    //Custom codes we'll handle, the rest is handled by base class
    enum {
        SET_SURF_CONFIG = 4000,
        GET_PREF_LAYOUT = 4001,
        FORCE_S3D_DISPLAY_CONFIG = 4002,
        SET_WINDOW_CONFIG = 4003,
    };
    //Handles custom codes but delegates all else to base class onTransact
    virtual status_t onTransact(
        uint32_t code, const Parcel& data, Parcel* reply, uint32_t flags);

    inline bool isDefaultRender() const { return mRenderMode == eRenderDefault; }
    inline bool isAnaglyphRender() const { return mRenderMode == eRenderAnaglyph; }
    inline bool isInterleaveRender() const { return (mRenderMode == eRenderInterleaved); }
    inline bool isFramePackingRender() const { return (mRenderMode == eRenderFramePacking); }
    inline bool isMonoRender() const { return (mRenderMode == eRenderMono); }
    inline bool isDrawingLeft() const { return mDrawState == eDrawingS3DLeft; };
    inline bool isDrawingFirstHalf() const {
        return mDrawState == static_cast<DrawViewState>(mS3Dhw.getOrdering());
    };

    enum DrawViewState {
        eDrawingS3DLeft = eLeftViewFirst,
        eDrawingS3DRight = eRightViewFirst
    };

    enum RenderMode {
        //No custom drawing needed. Punts to base class drawing.
        eRenderDefault      = 0x1,
        //Custom drawing needed, but only one view out of a stereo layer is rendered.
        //Mono layers are rendered as they are.
        eRenderMono         = 0x2,
        //Custom drawing with stencil masking will be used.
        //Mono layers are ideally filterd to avoid aliasing artifacts.
        eRenderInterleaved  = 0x4,
        //The visible layer stack has to be drawn twice. Mono layers are replicated
        //for each view.
        eRenderFramePacking = 0x8,
        //Custom drawing by color coding stereo layers. Mono layers are drawn as they are.
        eRenderAnaglyph     = 0x10
    };

private:
    friend class OmapLayer;
    friend class OmapLayerScreenshot;

    //Overriden from base class
    virtual void handlePageFlip();
    virtual void composeSurfaces(const Region& dirty);
    virtual void modifyCoords(GLint& x, GLint& y, GLsizei& rw, GLsizei& rh) const;
    virtual void drawLayersForScreenshotLocked();
    virtual sp<Layer> createNormalSurface(
                            const sp<Client>& client, DisplayID display,
                            uint32_t w, uint32_t h, uint32_t flags,
                            PixelFormat& format);
    virtual sp<LayerScreenshot> createScreenshotSurface(
            const sp<Client>& client, DisplayID display,
            uint32_t w, uint32_t h, uint32_t flags);
    virtual status_t captureScreenImplLocked(DisplayID dpy,
                    sp<IMemoryHeap>* heap,
                    uint32_t* width, uint32_t* height, PixelFormat* format,
                    uint32_t reqWidth, uint32_t reqHeight,
                    uint32_t minLayerZ, uint32_t maxLayerZ);

    //These belong to S3DSurfaceFlinger
    void handleS3DVisibility();
    void determineRenderMode(bool s3dLayerVisible);
    void setupViewport(int w, int h);
    void setDrawState(DrawViewState state);
    // access must be protected by mStateLock
    void addS3DLayer_l(const sp<LayerBase>& layer);
    void removeS3DLayer_l(const sp<LayerBase>& layer);
    sp<OmapLayer> getLayer_l(const wp<IBinder>& binder);
    bool isS3DLayerVisible_l() const;

    // access must be protected by mStateLock
    DefaultKeyedVector< wp<IBinder>, wp<LayerBase> > mS3DLayers;

    // Can only accessed from the main thread, these members
    // don't need synchronization
    bool mWasS3DLayerVisible;

    DrawViewState mDrawState;
    RenderMode mRenderMode;
    //This is the render mode used when there's no S3D hardware
    //and we need to render a stereo layer (can only be analglyph or mono)
    RenderMode mDefaultRenderMode;

    bool mViewPortNeedsReset;
    StencilMask mStencilMask;
    S3DHardware mS3Dhw;

    //This mirrors the s3d hardware output type most of the time
    //except during screenshots
    S3DLayoutType mOutputType;
};
};
#endif//_S3DSURFACEFLINGER_H_
