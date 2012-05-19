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

#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>

#include <cutils/log.h>
#include <cutils/properties.h>

#include "hooks.h"
#include "egl_impl.h"

using namespace android;

// ----------------------------------------------------------------------------
// Actual GL entry-points
// ----------------------------------------------------------------------------

#undef API_ENTRY
#undef CALL_GL_API
#undef CALL_GL_API_RETURN

#define DEBUG_CALL_GL_API 0

#if USE_FAST_TLS_KEY

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

    #define API_ENTRY(_api) _api

#if DEBUG_CALL_GL_API

    #define CALL_GL_API(_api, ...)                                       \
        gl_hooks_t::gl_t const * const _c = &getGlThreadSpecific()->gl;  \
        _c->_api(__VA_ARGS__); \
        GLenum status = GL_NO_ERROR; \
        while ((status = glGetError()) != GL_NO_ERROR) { \
            ALOGD("[" #_api "] 0x%x", status); \
        }

#else

    #define CALL_GL_API(_api, ...)                                       \
        gl_hooks_t::gl_t const * const _c = &getGlThreadSpecific()->gl;  \
        _c->_api(__VA_ARGS__);

#endif

    #define CALL_GL_API_RETURN(_api, ...)                                \
        gl_hooks_t::gl_t const * const _c = &getGlThreadSpecific()->gl;  \
        return _c->_api(__VA_ARGS__)

#endif


extern "C" {
#include "gl2_api.in"
#include "gl2ext_api.in"
}

#undef API_ENTRY
#undef CALL_GL_API
#undef CALL_GL_API_RETURN


/*
 * These GL calls are special because they need to EGL to retrieve some
 * informations before they can execute.
 */

#ifdef HOOK_MISSING_EGL_EXTERNAL_IMAGE
//extern "C" const GLubyte* __glGetString(GLenum name);
extern "C" void __glShaderSource(GLuint shader, GLsizei count, const GLchar** string, const GLint* length);
extern "C" void __glEnable(GLenum cap);
extern "C" void __glDisable(GLenum cap);
extern "C" void __glTexParameterf(GLenum target, GLenum pname, GLfloat param);
extern "C" void __glTexParameterfv(GLenum target, GLenum pname, const GLfloat* params);
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
        LOGD("glGetString(GL_EXTENSIONS): GL_OES_EGL_image_external added");
        return (GLubyte*)extensions;
    }
    return __glGetString(name);
}
*/

void glShaderSource(GLuint shader, GLsizei count, const GLchar** string, const GLint* length)
{
    bool needRewrite = false;

    LOGD("Shader source dump:");
    for (GLsizei i = 0; i < count; i++) {
        LOGD("%s", string[i]);
        if (strstr(string[i], "GL_OES_EGL_image_external")) {
            needRewrite = true;
            break;
        }
    }

/* TODO
The following code (made to replace "samplerExternalOES" with "sampler2D")
is currently very dumb:
- it assumes that "#extension GL_OES_EGL_image_external : require"
  is present and appears before the first newline in the shader source
  (the first line is then replaced with an empty one)
- it assumes only one appearence of "samplerExternalOES" in the shader code
  (and replaces this first appearence with "sampler2D")

Despite the mentioned limitations, it seems to capture
all real cases encountered so far.
*/

    if (!needRewrite) {
        __glShaderSource(shader, count, string, length);
        return;
    }

    LOGD("Shader source rewrite:");

    GLchar **newStrings = new GLchar*[count];
    const GLchar *start, *pos;

    for (GLsizei i = 0; i < count; i++) {
        start = strstr(string[i], "\n");
        if (!start) {
            start = string[i];
        } else {
            /* skip '\n' */
            start++;
        }

        newStrings[i] = new GLchar[strlen(start) + 1];
        pos = strstr(start, "samplerExternalOES");
        if (pos) {
            /* copy part before 'samplerExternalOES' */
            strncpy(newStrings[i], start, pos - start);
            /* ensure NUL termination */
            newStrings[i][pos - start] = '\0';
            strcat(newStrings[i], "sampler2D");
            /* copy remainder */
            pos += strlen("samplerExternalOES");
            strcat(newStrings[i], pos);
        } else {
            strcpy(newStrings[i], start);
        }
        LOGD("%s", newStrings[i]);
    }

    __glShaderSource(shader, count, const_cast<const GLchar **>(newStrings), length);

    for (GLsizei i = 0; i < count; i++) {
        delete [] newStrings[i];
    }
    delete [] newStrings;
}

void glTexParameterf(GLenum target, GLenum pname, GLfloat param)
{
    if (target == GL_TEXTURE_EXTERNAL_OES) {
        target = GL_TEXTURE_2D;
        LOGD("glTexParameterf: EXTERNAL_OES -> 2D");
    }
    __glTexParameterf(target, pname, param);
}

void glTexParameterfv(GLenum target, GLenum pname, const GLfloat* params)
{
    if (target == GL_TEXTURE_EXTERNAL_OES) {
        target = GL_TEXTURE_2D;
        LOGD("glTexParameterfv: EXTERNAL_OES -> 2D");
    }
    __glTexParameterfv(target, pname, params);
}

void glTexParameteri(GLenum target, GLenum pname, GLint param)
{
    if (target == GL_TEXTURE_EXTERNAL_OES) {
        target = GL_TEXTURE_2D;
        LOGD("glTexParameteri: EXTERNAL_OES -> 2D");
    }
    __glTexParameteri(target, pname, param);
}

void glTexParameteriv(GLenum target, GLenum pname, const GLint* params)
{
    if (target == GL_TEXTURE_EXTERNAL_OES) {
        target = GL_TEXTURE_2D;
        LOGD("glTexParameteriv: EXTERNAL_OES -> 2D");
    }
    __glTexParameteriv(target, pname, params);
}

void glEnable(GLenum cap)
{
    if (cap == GL_TEXTURE_EXTERNAL_OES) {
        cap = GL_TEXTURE_2D;
        LOGD("glEnable: EXTERNAL_OES -> 2D");
    }
    __glEnable(cap);
}

void glDisable(GLenum cap)
{
    if (cap == GL_TEXTURE_EXTERNAL_OES) {
        cap = GL_TEXTURE_2D;
        LOGD("glDisable: EXTERNAL_OES -> 2D");
    }
    __glDisable(cap);
}

void glBindTexture(GLenum target, GLuint texture)
{
    if (target == GL_TEXTURE_EXTERNAL_OES) {
        target = GL_TEXTURE_2D;
        LOGD("glBindTexture: EXTERNAL_OES -> 2D");
    }
    __glBindTexture(target, texture);
}
#endif // HOOK_MISSING_EGL_EXTERNAL_IMAGE

void glEGLImageTargetTexture2DOES(GLenum target, GLeglImageOES image)
{
#ifdef HOOK_MISSING_EGL_EXTERNAL_IMAGE
    if (target == GL_TEXTURE_EXTERNAL_OES) {
        target = GL_TEXTURE_2D;
        LOGD("glEGLImageTargetTexture2DOES: EXTERNAL_OES -> 2D");
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

