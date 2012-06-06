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
#include <stdio.h>
#include <stdlib.h>

#include "S3DSurfaceFlinger.h"
#include "DisplayHardware/HWComposer.h"

#include <ui/GraphicLog.h>
#include <cutils/properties.h>

namespace android {


StencilMask::StencilMask()
    :   mVboId(0),
        mCount(0),
        mType(NONE)
{
}

StencilMask::~StencilMask()
{
    if (mVboId) {
        glDeleteBuffers(1, &mVboId);
    }
}

void StencilMask::setupCoords(int w, int h, InterleavingType type)
{
    if (mType == type) {
        return;
    }

    size_t i =0, j=0;
    mType = type;
    mCount = type == ROW ? h : w;
    int lineCount = mCount/2;
    GLshort* coords = new GLshort[4*lineCount];

    if (type == ROW) {
        while(lineCount > 0) {
            coords[i] = 0;
            coords[i+1] = j;
            coords[i+2] = w;
            coords[i+3] = j;
            i+=4; j+=2;
            lineCount--;
        }
    }
    else if (type == COLUMN) {
        while(lineCount > 0) {
            coords[i] = j;
            coords[i+1] = 0;
            coords[i+2] = j;
            coords[i+3] = h;
            i+=4; j+=2;
            lineCount--;
        }
    }
    glGenBuffers(1, &mVboId);
    glBindBuffer(GL_ARRAY_BUFFER, mVboId);
    glBufferData(GL_ARRAY_BUFFER, sizeof(GLshort)*mCount*2, coords, GL_STATIC_DRAW);

    delete[] coords;
    glBindBuffer(GL_ARRAY_BUFFER, 0);
}

void StencilMask::drawStencil(int w, int h)
{
    glDisable(GL_SCISSOR_TEST);
    glEnable(GL_STENCIL_TEST);
    glClear(GL_STENCIL_BUFFER_BIT);
    glColorMask(GL_FALSE, GL_FALSE, GL_FALSE, GL_FALSE);
    glStencilMask(1);

    glStencilFunc(GL_NEVER, 0x1, 0x1);
    glStencilOp(GL_REPLACE, GL_REPLACE, GL_REPLACE);

    glBindBuffer(GL_ARRAY_BUFFER, mVboId);
    glVertexPointer(2, GL_SHORT, 0, NULL);
    glDrawArrays(GL_LINES, 0, mCount);
    glBindBuffer(GL_ARRAY_BUFFER, 0);

    glColorMask(GL_TRUE, GL_TRUE, GL_TRUE, GL_TRUE);
    glStencilMask(0);
    glDisable(GL_STENCIL_TEST);
    glEnable(GL_SCISSOR_TEST);
}

void StencilMask::configStencilOp(bool drawEvenLines)
{
    if (drawEvenLines) {
        glStencilFunc(GL_EQUAL, 0x1, 0x1);
    } else {
        glStencilFunc(GL_EQUAL, 0x0, 0x1);
    }
    glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP);
}

// ---------------------------------------------------------------------------
S3DSurfaceFlinger::S3DSurfaceFlinger()
  : mWasS3DLayerVisible(false),
    mDrawState(eDrawingS3DLeft),
    mRenderMode(eRenderDefault),
    mViewPortNeedsReset(false),
    mOutputType(eMono)
{
    char prop[PROPERTY_VALUE_MAX];
    //This should be a system setting, for now use a property and we default
    //to anaglyph rendering.
    property_get("omap.sf.s3d.anaglyph", prop, "1");
    mDefaultRenderMode = atoi(prop) ? eRenderAnaglyph : eRenderMono;
}

sp<OmapLayer> S3DSurfaceFlinger::getLayer_l(const wp<IBinder>& binder)
{
    sp<Layer> layer(mLayerMap.valueFor(binder).promote());
    sp<OmapLayer> layerSub;
    if (layer != 0) {
        layerSub = static_cast<OmapLayer*>(layer.get());
    }
    return layerSub;
}

void S3DSurfaceFlinger::addS3DLayer_l(const sp<LayerBase>& layer)
{
    sp<LayerBaseClient> lbc(layer->getLayerBaseClient());
    if (lbc != 0) {
        mS3DLayers.add(lbc->getSurfaceBinder(), layer);
    }
}

void S3DSurfaceFlinger::removeS3DLayer_l(const sp<LayerBase>& layer)
{
    sp<LayerBaseClient> lbc(layer->getLayerBaseClient());
    if (lbc != 0) {
        mS3DLayers.removeItem( lbc->getSurfaceBinder() );
    }
}

bool S3DSurfaceFlinger::isS3DLayerVisible_l() const {
    bool s3dLayerVisible = false;
    const DefaultKeyedVector< wp<IBinder>, wp<LayerBase> >& layers(mS3DLayers);
    size_t count = layers.size();
    for (size_t i=0 ; i<count ; i++) {
        const sp<LayerBase>& layer(layers.valueAt(i).promote());
        if (layer != 0 && !layer->visibleRegionScreen.isEmpty()) {
            s3dLayerVisible = true;
            break;
        }
    }
    return s3dLayerVisible;
}

void S3DSurfaceFlinger::handleS3DVisibility()
{
    Mutex::Autolock _l(mStateLock);
    bool s3dLayerVisible = isS3DLayerVisible_l();
    const DisplayHardware& hw(graphicPlane(0).displayHardware());

    //Redraw everything if we have transitioned from or to S3D rendering state
    if (s3dLayerVisible != mWasS3DLayerVisible) {
        mDirtyRegion.set(hw.bounds());
    }

    determineRenderMode(s3dLayerVisible);
    mWasS3DLayerVisible = s3dLayerVisible;
}

void S3DSurfaceFlinger::determineRenderMode(bool s3dLayerVisible)
{
    if (s3dLayerVisible) {
        if (mS3Dhw.initCheck() != NO_ERROR) {
            mRenderMode = mDefaultRenderMode;
        } else {
            //S3D hardware is available, we need custom drawing
            mRenderMode = mS3Dhw.isInterleaved() ? eRenderInterleaved : eRenderFramePacking;
        }
    } else {
        //We need custom drawing for hardware that expects S3D data all the time
        //even if there's no S3D layer visible
        if (mS3Dhw.initCheck() == NO_ERROR && !mS3Dhw.isSwitcheable()) {
            mRenderMode = mS3Dhw.isInterleaved() ? eRenderInterleaved : eRenderFramePacking;
        }
    }

    mS3Dhw.enableS3D(isInterleaveRender() || isFramePackingRender());
}

void S3DSurfaceFlinger::setupViewport(int hw, int hh)
{
    GLint x =0, y = 0;
    GLsizei w = hw;
    GLsizei h = hh;
    modifyCoords(x, y, w, h);
    glViewport(x, y, w, h);
    mViewPortNeedsReset = true;
}

void S3DSurfaceFlinger::setDrawState(DrawViewState state)
{
    mDrawState = state;
    if (isInterleaveRender()) {
        mStencilMask.configStencilOp(isDrawingFirstHalf());
    }
}

void S3DSurfaceFlinger::modifyCoords(GLint& x, GLint& y, GLsizei& w, GLsizei& h) const
{
    if (!isFramePackingRender()) {
        return;
    }

    //This is only applicable to frame packing as the viewport is changed
    const DisplayHardware& hw(graphicPlane(0).displayHardware());
    GLsizei dispw = hw.getWidth();
    GLsizei disph = hw.getHeight();
    if (isDrawingFirstHalf()) {
        switch(mOutputType) {
            //top half
            case eTopBottom:
                y /= 2;
                y += disph/2;
                h /= 2;
                break;
            //left half
            case eSideBySide:
                x /= 2;
                w /= 2;
                break;
            default:
                break;
        }
    }else {
        switch(mOutputType) {
            //bottom half
            case eTopBottom:
                y /= 2;
                h /= 2;
                break;
            //right half
            case eSideBySide:
                x /=2;
                x += dispw/2;
                w /= 2;
                break;
                default:
                    break;
        }
    }

}

status_t S3DSurfaceFlinger::onTransact(uint32_t code, const Parcel& data,
                                        Parcel* reply, uint32_t flags)
{
    switch(code) {
        case SET_SURF_CONFIG:
        case GET_PREF_LAYOUT:
        case FORCE_S3D_DISPLAY_CONFIG:
        case SET_WINDOW_CONFIG:
            //We don't check specific permissions to access surface flinger
            //as we are not returning an interface to surfaceflinger or internal
            //layers and we are not providing any capture services
            CHECK_INTERFACE(ISurfaceComposer, data, reply);
            break;
        default:
            return SurfaceFlinger::onTransact(code, data, reply, flags);
    }

    status_t err = NO_ERROR;
    switch(code) {
        case SET_SURF_CONFIG: {
            Mutex::Autolock _l(mStateLock);
            wp<IBinder> binder = data.readWeakBinder();
            sp<OmapLayer> layerSub = getLayer_l(binder);
            if (layerSub != 0) {
                S3DLayoutType type = static_cast<S3DLayoutType>(data.readInt32());
                S3DLayoutOrder order = static_cast<S3DLayoutOrder>(data.readInt32());
                S3DRenderMode mode = static_cast<S3DRenderMode>(data.readInt32());
                layerSub->setConfig(type, order, mode);
                if (layerSub->isS3D()) {
                    mS3DLayers.add(binder,layerSub);
                } else {
                    mS3DLayers.removeItem(binder);
                }
                invalidateHwcGeometry();
                repaintEverything();
            }
        } break;
        case SET_WINDOW_CONFIG: {
            Mutex::Autolock _l(mStateLock);
            String8 windowName(data.readString8());
            sp<OmapLayer> layerSub;
            for (size_t i = 0; i < mLayerMap.size(); i++) {
                sp<Layer> layer(mLayerMap.valueAt(i).promote());
                if (layer.get() && windowName == layer->getName()) {
                    layerSub = static_cast<OmapLayer*>(layer.get());
                    break;
                }
            }
            if (layerSub != 0) {
                S3DLayoutType type = static_cast<S3DLayoutType>(data.readInt32());
                S3DLayoutOrder order = static_cast<S3DLayoutOrder>(data.readInt32());
                S3DRenderMode mode = static_cast<S3DRenderMode>(data.readInt32());
                layerSub->setConfig(type, order, mode);
                if (layerSub->isS3D()) {
                    mS3DLayers.add(layerSub->getSurfaceBinder(),layerSub);
                } else {
                    mS3DLayers.removeItem(layerSub->getSurfaceBinder());
                }
                invalidateHwcGeometry();
                repaintEverything();
            }
        } break;
        case GET_PREF_LAYOUT: {
            if (mS3Dhw.getType() == eRowInterleaved) {
                reply->writeInt32(eTopBottom);
            } else if (mS3Dhw.getType() == eColInterleaved) {
                 reply->writeInt32(eSideBySide);
            } else {
                reply->writeInt32(mS3Dhw.getType());
            }
            reply->writeInt32(mS3Dhw.getOrdering());
        } break;
        case FORCE_S3D_DISPLAY_CONFIG: {
            S3DLayoutType type = static_cast<S3DLayoutType>(data.readInt32());
            S3DLayoutOrder order = static_cast<S3DLayoutOrder>(data.readInt32());
            bool switcheable = data.readInt32();
            Mutex::Autolock _l(mStateLock);
            mS3Dhw.forceConfiguration(type, order, switcheable);
            invalidateHwcGeometry();
            repaintEverything();
        } break;
        default:
            err = PERMISSION_DENIED;
            break;
    }
    return err;
}

//This method is overriden for the only purpose of instantiating
//OmapLayer objects (instead of Layer) so we can do custom drawing if needed
sp<Layer> S3DSurfaceFlinger::createNormalSurface(
        const sp<Client>& client, DisplayID display,
        uint32_t w, uint32_t h, uint32_t flags,
        PixelFormat& format)
{
    // initialize the surfaces
    switch (format) { // TODO: take h/w into account
    case PIXEL_FORMAT_TRANSPARENT:
    case PIXEL_FORMAT_TRANSLUCENT:
        format = PIXEL_FORMAT_RGBA_8888;
        break;
    case PIXEL_FORMAT_OPAQUE:
#ifdef NO_RGBX_8888
        format = PIXEL_FORMAT_RGB_565;
#else
        format = PIXEL_FORMAT_RGBX_8888;
#endif
        break;
    }

#ifdef NO_RGBX_8888
    if (format == PIXEL_FORMAT_RGBX_8888) {
        format = PIXEL_FORMAT_RGBA_8888;
    }
#endif

    sp<OmapLayer> layerSub = new OmapLayer(this, display, client);
    status_t err = layerSub->setBuffers(w, h, format, flags);
    if (LIKELY(err != NO_ERROR)) {
        LOGE("createNormalSurfaceLocked() failed (%s)", strerror(-err));
        layerSub.clear();
    }
    return layerSub;
}

sp<LayerScreenshot> S3DSurfaceFlinger::createScreenshotSurface(
        const sp<Client>& client, DisplayID display,
        uint32_t w, uint32_t h, uint32_t flags)
{
    S3DLayoutType type = eMono;
    sp<LayerScreenshot> layer;

    {
        Mutex::Autolock _l(mStateLock);
        if (isS3DLayerVisible_l()) {
            type = eSideBySide;
        }
        layer = new OmapLayerScreenshot(this, display, client, type, eLeftViewFirst);
    }

    return layer;
}

status_t S3DSurfaceFlinger::captureScreenImplLocked(DisplayID dpy,
        sp<IMemoryHeap>* heap,
        uint32_t* w, uint32_t* h, PixelFormat* f,
        uint32_t sw, uint32_t sh,
        uint32_t minLayerZ, uint32_t maxLayerZ)
{
    status_t ret;
    if (isS3DLayerVisible_l()) {
        mRenderMode = eRenderMono;
    }
    ret = SurfaceFlinger::captureScreenImplLocked(dpy, heap, w, h, f, sw, sh,
                                                    minLayerZ, maxLayerZ);
    mRenderMode = eRenderDefault;
    return ret;
}

void S3DSurfaceFlinger::drawLayersForScreenshotLocked()
{
    if (!isS3DLayerVisible_l()) {
        return SurfaceFlinger::drawLayersForScreenshotLocked();
    }

    const DisplayHardware& hw(graphicPlane(0).displayHardware());
    int w = hw.getWidth();
    int h = hw.getHeight();

    //If an S3D layer is visible, an S3D screenshot will be generated
    //in side-by-side format.
    //This is done so that transitions while drawing S3D content occur
    //without artifacts
    mRenderMode = eRenderFramePacking;
    mOutputType = eSideBySide;
    mDrawState = eDrawingS3DLeft;
    setupViewport(w, h);
    SurfaceFlinger::drawLayersForScreenshotLocked();

    mDrawState = eDrawingS3DRight;
    setupViewport(w, h);
    SurfaceFlinger::drawLayersForScreenshotLocked();

    glViewport(0, 0, w, h);
    mRenderMode = eRenderDefault;
    mOutputType = mS3Dhw.getType();
}

void  S3DSurfaceFlinger::handlePageFlip()
{
    SurfaceFlinger::handlePageFlip();
    handleS3DVisibility();
}

void S3DSurfaceFlinger::composeSurfaces(const Region& dirty)
{
    if (mRenderMode == eRenderDefault) {
        //No custom drawing neeeded, punt to base class
        SurfaceFlinger::composeSurfaces(dirty);
        return;
    }

    const DisplayHardware& hw(graphicPlane(0).displayHardware());
    int w = hw.getWidth();
    int h = hw.getHeight();

    mDrawState = eDrawingS3DLeft;
    mOutputType = mS3Dhw.getType();

    if (isInterleaveRender()) {
        mStencilMask.setupCoords(w, h, (StencilMask::InterleavingType)mS3Dhw.getType());
        mStencilMask.drawStencil(w, h);
        mStencilMask.configStencilOp(isDrawingFirstHalf());
    } else if (isFramePackingRender()) {
        setupViewport(w, h);
    }
    SurfaceFlinger::composeSurfaces(dirty);

    //For frame packing rendering, the visible stack of layers has to drawn twice
    //as the Mono layers need to be replicated.
    if (isFramePackingRender()) {
        mDrawState = eDrawingS3DRight;
        setupViewport(w, h);
        SurfaceFlinger::composeSurfaces(dirty);
    }

    if (mViewPortNeedsReset) {
        glViewport(0, 0, w, h);
    }

    mRenderMode = eRenderDefault;
}

};
