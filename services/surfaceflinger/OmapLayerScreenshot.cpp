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


#include "OmapLayerScreenshot.h"
#include "S3DSurfaceFlinger.h"

namespace android {

OmapLayerScreenshot::OmapLayerScreenshot(S3DSurfaceFlinger* flinger,
            DisplayID display,const sp<Client>& client,
            S3DLayoutType type,
            S3DLayoutOrder ordering)
         :  LayerScreenshot(flinger, display, client),
            mFlingerS3D(flinger),
            mType(type),
            mViewOrder(ordering)
{
}

uint32_t OmapLayerScreenshot::doTransaction(uint32_t f)
{
    uint32_t flags = LayerScreenshot::doTransaction(f);
    const Layer::State& draw(drawingState());
    const Layer::State& curr(currentState());

    if (draw.flags & ISurfaceComposer::eLayerHidden) {
        if (!(curr.flags & ISurfaceComposer::eLayerHidden) && mType != eMono) {
            mFlingerS3D->addS3DLayer_l(this);
        }
    }
    return flags;
}

void OmapLayerScreenshot::onRemoved()
{
    mFlingerS3D->removeS3DLayer_l(this);
    LayerScreenshot::onRemoved();
}

void OmapLayerScreenshot::drawS3DRegion(const Region& clip, int hw_w, int hw_h) const
{
    GLfloat u = mTexCoords[4];
    GLfloat v = mTexCoords[1];

    struct TexCoords {
        GLfloat u;
        GLfloat v;
    };

    TexCoords texCoords[4];
    texCoords[0].u = 0;
    texCoords[0].v = v;
    texCoords[1].u = 0;
    texCoords[1].v = 0;
    texCoords[2].u = u;
    texCoords[2].v = 0;
    texCoords[3].u = u;
    texCoords[3].v = v;

    if(mFlingerS3D->isDrawingLeft()) {
        texCoords[2].u = 0.5f*u;
        texCoords[3].u = 0.5f*u;
    }
    else {
        texCoords[0].u = 0.5f*u;
        texCoords[1].u = 0.5f*u;
    }

    glTexCoordPointer(2, GL_FLOAT, 0, texCoords);

    Region::const_iterator it = clip.begin();
    Region::const_iterator const end = clip.end();
    while (it != end) {
        const Rect& r = *it++;
        GLint sy = hw_h - (r.top + r.height());
        GLint x = r.left;
        GLsizei w = r.width();
        GLsizei h = r.height();
        mFlingerS3D->modifyCoords(x, sy, w, h);
        glScissor(x, sy, w, h);
        glDrawArrays(GL_TRIANGLE_FAN, 0, 4);
    }
}

void OmapLayerScreenshot::drawRegion(const Region& clip, int hw_w, int hw_h) const
{
    if (mFlingerS3D->isDefaultRender() || (!isS3D() && !mFlingerS3D->isFramePackingRender())) {
        LayerScreenshot::drawRegion(clip, hw_w, hw_h);
        return;
    }

    if (isS3D() && mFlingerS3D->isInterleaveRender()) {
        glEnable(GL_STENCIL_TEST);
    } else if (isS3D() && mFlingerS3D->isAnaglyphRender()) {
        //Left view = RED
        glColorMask(GL_TRUE, GL_FALSE, GL_FALSE, GL_TRUE);
    }

    drawS3DRegion(clip, hw_w, hw_h);

    //This layer draws its right view here as the viewport is not changed.
    //This is done so that blending of any higher z layers with this one is correct.
    if(isS3D() && !mFlingerS3D->isFramePackingRender()) {
        mFlingerS3D->setDrawState(S3DSurfaceFlinger::eDrawingS3DRight);
        if (mFlingerS3D->isAnaglyphRender()) {
            //right view = cyan
            glColorMask(GL_FALSE, GL_TRUE, GL_TRUE, GL_TRUE);
        }
        drawS3DRegion(clip, hw_w, hw_h);
        mFlingerS3D->setDrawState(S3DSurfaceFlinger::eDrawingS3DLeft);
    }

    glColorMask(GL_TRUE, GL_TRUE, GL_TRUE, GL_TRUE);
    glDisable(GL_STENCIL_TEST);
}

};
