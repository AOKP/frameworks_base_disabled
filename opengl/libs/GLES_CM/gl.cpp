/* 
 ** Copyright 2007, The Android Open Source Project
 **
 ** Licensed under the Apache License, Version 2.0 (the "License"); 
 ** you may not use this file except in compliance with the License. 
 ** You may obtain a copy of the License at 
 **
 **     http://www.apache.org/licenses/LICENSE-2.0 
 **
 ** Unless required by applicable law or agreed to in writing, software 
 ** distributed under the License is distributed on an "AS IS" BASIS, 
 ** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 ** See the License for the specific language governing permissions and 
 ** limitations under the License.
 */

#include <ctype.h>
#include <string.h>
#include <errno.h>

#include <sys/ioctl.h>

#include <GLES/gl.h>
#include <GLES/glext.h>

#include <cutils/log.h>
#include <cutils/properties.h>

#include "hooks.h"
#include "egl_impl.h"

using namespace android;

// set this to 1 for crude GL debugging
#define CHECK_FOR_GL_ERRORS     0

// ----------------------------------------------------------------------------
// extensions for the framework
// ----------------------------------------------------------------------------

extern "C" {
GL_API void GL_APIENTRY glColorPointerBounds(GLint size, GLenum type, GLsizei stride,
        const GLvoid *ptr, GLsizei count);
GL_API void GL_APIENTRY glNormalPointerBounds(GLenum type, GLsizei stride,
        const GLvoid *pointer, GLsizei count);
GL_API void GL_APIENTRY glTexCoordPointerBounds(GLint size, GLenum type,
        GLsizei stride, const GLvoid *pointer, GLsizei count);
GL_API void GL_APIENTRY glVertexPointerBounds(GLint size, GLenum type,
        GLsizei stride, const GLvoid *pointer, GLsizei count);
GL_API void GL_APIENTRY glPointSizePointerOESBounds(GLenum type,
        GLsizei stride, const GLvoid *pointer, GLsizei count);
GL_API void GL_APIENTRY glMatrixIndexPointerOESBounds(GLint size, GLenum type,
        GLsizei stride, const GLvoid *pointer, GLsizei count);
GL_API void GL_APIENTRY glWeightPointerOESBounds(GLint size, GLenum type,
        GLsizei stride, const GLvoid *pointer, GLsizei count);
}

void glColorPointerBounds(GLint size, GLenum type, GLsizei stride,
        const GLvoid *ptr, GLsizei count) {
    glColorPointer(size, type, stride, ptr);
}
void glNormalPointerBounds(GLenum type, GLsizei stride,
        const GLvoid *pointer, GLsizei count) {
    glNormalPointer(type, stride, pointer);
}
void glTexCoordPointerBounds(GLint size, GLenum type,
        GLsizei stride, const GLvoid *pointer, GLsizei count) {
    glTexCoordPointer(size, type, stride, pointer);
}
void glVertexPointerBounds(GLint size, GLenum type,
        GLsizei stride, const GLvoid *pointer, GLsizei count) {
    glVertexPointer(size, type, stride, pointer);
}

void GL_APIENTRY glPointSizePointerOESBounds(GLenum type,
        GLsizei stride, const GLvoid *pointer, GLsizei count) {
    glPointSizePointerOES(type, stride, pointer);
}

GL_API void GL_APIENTRY glMatrixIndexPointerOESBounds(GLint size, GLenum type,
        GLsizei stride, const GLvoid *pointer, GLsizei count) {
    glMatrixIndexPointerOES(size, type, stride, pointer);
}

GL_API void GL_APIENTRY glWeightPointerOESBounds(GLint size, GLenum type,
        GLsizei stride, const GLvoid *pointer, GLsizei count) {
    glWeightPointerOES(size, type, stride, pointer);
}

// ----------------------------------------------------------------------------
// Actual GL entry-points
// ----------------------------------------------------------------------------

#undef API_ENTRY
#undef CALL_GL_API
#undef CALL_GL_API_RETURN

#if USE_FAST_TLS_KEY && !CHECK_FOR_GL_ERRORS

    #ifdef HAVE_ARM_TLS_REGISTER
        #define GET_TLS(reg) \
            "mrc p15, 0, " #reg ", c13, c0, 3 \n"
    #else
        #define GET_TLS(reg) \
            "mov   " #reg ", #0xFFFF0FFF      \n"  \
            "ldr   " #reg ", [" #reg ", #-15] \n"
    #endif

    #define API_ENTRY(_api) __attribute__((naked)) _api

    #define CALL_GL_API(_api, ...)                              \
         asm volatile(                                          \
            GET_TLS(r12)                                        \
            "ldr   r12, [r12, %[tls]] \n"                       \
            "cmp   r12, #0            \n"                       \
            "ldrne pc,  [r12, %[api]] \n"                       \
            "mov   r0, #0             \n"                       \
            "bx    lr                 \n"                       \
            :                                                   \
            : [tls] "J"(TLS_SLOT_OPENGL_API*4),                 \
              [api] "J"(__builtin_offsetof(gl_hooks_t, gl._api))    \
            :                                                   \
            );

    #define CALL_GL_API_RETURN(_api, ...) \
        CALL_GL_API(_api, __VA_ARGS__) \
        return 0; // placate gcc's warnings. never reached.

#else

    #if CHECK_FOR_GL_ERRORS
    
        #define CHECK_GL_ERRORS(_api) \
            do { GLint err = glGetError(); \
                ALOGE_IF(err != GL_NO_ERROR, "%s failed (0x%04X)", #_api, err); \
            } while(false);

    #else

        #define CHECK_GL_ERRORS(_api) do { } while(false);

    #endif


    #define API_ENTRY(_api) _api

    #define CALL_GL_API(_api, ...)                                      \
        gl_hooks_t::gl_t const * const _c = &getGlThreadSpecific()->gl; \
        _c->_api(__VA_ARGS__);                                          \
        CHECK_GL_ERRORS(_api)

    #define CALL_GL_API_RETURN(_api, ...)                               \
        gl_hooks_t::gl_t const * const _c = &getGlThreadSpecific()->gl; \
        return _c->_api(__VA_ARGS__)

#endif


extern "C" {
#include "gl_api.in"
#include "glext_api.in"
}

#undef API_ENTRY
#undef CALL_GL_API
#undef CALL_GL_API_RETURN


/*
 * These GL calls are special because they need to EGL to retrieve some
 * informations before they can execute.
 */
#ifdef HOOK_MISSING_EGL_EXTERNAL_IMAGE
// extern "C" const GLubyte* __glGetString(GLenum name);
extern "C" void __glEnable(GLenum cap);
extern "C" void __glDisable(GLenum cap);
extern "C" void __glTexParameterx(GLenum target, GLenum pname, GLfixed param);
extern "C" void __glTexParameterxv(GLenum target, GLenum pname, const GLfixed *params);
extern "C" void __glTexParameteri(GLenum target, GLenum pname, GLint param);
extern "C" void __glTexParameteriv(GLenum target, GLenum pname, const GLint* params);
extern "C" void __glBindTexture(GLenum target, GLuint texture);
#endif
extern "C" void __glEGLImageTargetTexture2DOES(GLenum target, GLeglImageOES image);
extern "C" void __glEGLImageTargetRenderbufferStorageOES(GLenum target, GLeglImageOES image);

#ifdef HOOK_MISSING_EGL_EXTERNAL_IMAGE
/*
const GLubyte* glGetString(GLenum name)
{
    if (name == GL_EXTENSIONS) {
        char *exts = (char *)__glGetString(GL_EXTENSIONS);
        char *extensions;
        extensions = new char[850];
        strcpy(extensions, exts);
        strcat(extensions, "GL_OES_EGL_image_external ");
        LOGW("glGetString(GL_EXTENSIONS): GL_OES_EGL_image_external added");
        return (GLubyte*)extensions;
    }
    return __glGetString(name);
}
*/

void glTexParameterx(GLenum target, GLenum pname, GLfixed param)
{
    if (target == GL_TEXTURE_EXTERNAL_OES) {
        target = GL_TEXTURE_2D;
//        LOGW("glTexParameterx: EXTERNAL_OES > 2D");
    }
    __glTexParameterx(target, pname, param);
}

void glTexParameterxv(GLenum target, GLenum pname, const GLfixed *params)
{
    if (target == GL_TEXTURE_EXTERNAL_OES) {
        target = GL_TEXTURE_2D;
//        LOGW("glTexParameterxv: EXTERNAL_OES > 2D");
    }
    __glTexParameterxv(target, pname, params);
}

void glTexParameteri(GLenum target, GLenum pname, GLint param)
{
    if (target == GL_TEXTURE_EXTERNAL_OES) {
        target = GL_TEXTURE_2D;
//        LOGW("glTexParameteri: EXTERNAL_OES > 2D");
    }
    __glTexParameteri(target, pname, param);
}

void glTexParameteriv(GLenum target, GLenum pname, const GLint* params)
{
    if (target == GL_TEXTURE_EXTERNAL_OES) {
        target = GL_TEXTURE_2D;
//        LOGW("glTexParameteriv: EXTERNAL_OES > 2D");
    }
    __glTexParameteriv(target, pname, params);
}

void glEnable(GLenum cap)
{
    if (cap == GL_TEXTURE_EXTERNAL_OES) {
        cap = GL_TEXTURE_2D;
//        LOGW("glEnable: EXTERNAL_OES > 2D");
    }
    __glEnable(cap);
}

void glDisable(GLenum cap)
{
    if (cap == GL_TEXTURE_EXTERNAL_OES) {
        cap = GL_TEXTURE_2D;
//        LOGW("glDisable: EXTERNAL_OES > 2D");
    }
    __glDisable(cap);
}

void glBindTexture(GLenum target, GLuint texture)
{
    if (target == GL_TEXTURE_EXTERNAL_OES) {
        target = GL_TEXTURE_2D;
//        LOGW("glBindTexture: EXTERNAL_OES > 2D");
    }
    __glBindTexture(target, texture);
}
#endif // HOOK_MISSING_EGL_EXTERNAL_IMAGE

void glEGLImageTargetTexture2DOES(GLenum target, GLeglImageOES image)
{
#ifdef HOOK_MISSING_EGL_EXTERNAL_IMAGE
    if (target == GL_TEXTURE_EXTERNAL_OES) {
        target = GL_TEXTURE_2D;
//        LOGW("glEGLImageTargetTexture2DOES: EXTERNAL_OES > 2D");
    }
#endif
    GLeglImageOES implImage = 
        (GLeglImageOES)egl_get_image_for_current_context((EGLImageKHR)image);
    __glEGLImageTargetTexture2DOES(target, implImage);
}

void glEGLImageTargetRenderbufferStorageOES(GLenum target, GLeglImageOES image)
{
    GLeglImageOES implImage = 
        (GLeglImageOES)egl_get_image_for_current_context((EGLImageKHR)image);
    __glEGLImageTargetRenderbufferStorageOES(target, implImage);
}

