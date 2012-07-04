/* libs/opengles/matrix.h
**
** Copyright 2006, The Android Open Source Project
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

#ifndef ANDROID_OPENGLES_MATRIX_H
#define ANDROID_OPENGLES_MATRIX_H

#include <stdint.h>
#include <stddef.h>
#include <sys/types.h>
#include <utils/Log.h>

#include <private/pixelflinger/ggl_context.h>

#include <GLES/gl.h>

namespace android {

const int OGLES_MODELVIEW_STACK_DEPTH   = 16;
const int OGLES_PROJECTION_STACK_DEPTH  =  2;
const int OGLES_TEXTURE_STACK_DEPTH     =  2;

void ogles_init_matrix(ogles_context_t*);
void ogles_uninit_matrix(ogles_context_t*);
void ogles_invalidate_perspective(ogles_context_t* c);
void ogles_validate_transform_impl(ogles_context_t* c, uint32_t want);

int ogles_surfaceport(ogles_context_t* c, GLint x, GLint y);

void ogles_scissor(ogles_context_t* c, 
        GLint x, GLint y, GLsizei w, GLsizei h);

void ogles_viewport(ogles_context_t* c,
        GLint x, GLint y, GLsizei w, GLsizei h);

inline void ogles_validate_transform(
        ogles_context_t* c, uint32_t want)
{
    if (c->transforms.dirty & want)
        ogles_validate_transform_impl(c, want);
}

// ----------------------------------------------------------------------------

inline
GLfixed vsquare3(GLfixed a, GLfixed b, GLfixed c) 
{
#if defined(__arm__) && !defined(__thumb__)

    GLfixed r;
    int32_t t;
    asm(
        "smull %0, %1, %2, %2       \n"
        "smlal %0, %1, %3, %3       \n"
        "smlal %0, %1, %4, %4       \n"
        "movs  %0, %0, lsr #16      \n"
        "adc   %0, %0, %1, lsl #16  \n"
        :   "=&r"(r), "=&r"(t) 
        :   "%r"(a), "r"(b), "r"(c)
        :   "cc"
        ); 
    return r;

#else

    return ((   int64_t(a)*a +
                int64_t(b)*b +
                int64_t(c)*c + 0x8000)>>16);

#endif
}

static inline GLfixed mla2a( GLfixed a0, GLfixed b0,
                            GLfixed a1, GLfixed b1,
                            GLfixed c)
{
#if defined(__arm__) && !defined(__thumb__)
                            
    GLfixed r;
    int32_t t;
    asm(
        "smull %0, %1, %2, %3       \n"
        "smlal %0, %1, %4, %5       \n"
        "add   %0, %6, %0, lsr #16  \n"
        "add   %0, %0, %1, lsl #16  \n"
        :   "=&r"(r), "=&r"(t) 
        :   "%r"(a0), "r"(b0), 
            "%r"(a1), "r"(b1),
            "r"(c)
        :
        ); 
    return r;
    
#else

    return ((   int64_t(a0)*b0 +
                int64_t(a1)*b1)>>16) + c;

#endif
}

static inline GLfixed mla3a( GLfixed a0, GLfixed b0,
                             GLfixed a1, GLfixed b1,
                             GLfixed a2, GLfixed b2,
                             GLfixed c)
{
#if defined(__arm__) && !defined(__thumb__)
                            
    GLfixed r;
    int32_t t;
    asm(
        "smull %0, %1, %2, %3       \n"
        "smlal %0, %1, %4, %5       \n"
        "smlal %0, %1, %6, %7       \n"
        "add   %0, %8, %0, lsr #16  \n"
        "add   %0, %0, %1, lsl #16  \n"
        :   "=&r"(r), "=&r"(t) 
        :   "%r"(a0), "r"(b0),
            "%r"(a1), "r"(b1),
            "%r"(a2), "r"(b2),
            "r"(c)
        :
        ); 
    return r;
    
#else

    return ((   int64_t(a0)*b0 +
                int64_t(a1)*b1 +
                int64_t(a2)*b2)>>16) + c;

#endif
}

// b0, b1, b2 are signed 16-bit quanities
// that have been shifted right by 'shift' bits relative to normal
// S16.16 fixed point
static inline GLfixed mla3a16( GLfixed a0, int32_t b1b0,
                               GLfixed a1,
                               GLfixed a2, int32_t b2,
                               GLint shift,
                               GLfixed c)
{
#if defined(__arm__) && !defined(__thumb__)
                            
    GLfixed r;
    asm(
        "smulwb %0, %1, %2          \n"
        "smlawt %0, %3, %2, %0      \n" 
        "smlawb %0, %4, %5, %0      \n"
        "add    %0, %7, %0, lsl %6  \n"
        :   "=&r"(r)
        :   "r"(a0), "r"(b1b0),
            "r"(a1),
            "r"(a2), "r"(b2),
            "r"(shift),
            "r"(c)
        :
        ); 
    return r;
    
#else

    int32_t accum;
    int16_t b0 = b1b0 & 0xffff;
    int16_t b1 = (b1b0 >> 16) & 0xffff;
    accum  = int64_t(a0)*int16_t(b0) >> 16;
    accum += int64_t(a1)*int16_t(b1) >> 16;
    accum += int64_t(a2)*int16_t(b2) >> 16;
    accum = (accum << shift) + c;
    return accum;

#endif
}


static inline GLfixed mla3a16_btb( GLfixed a0,
                                   GLfixed a1,
                                   GLfixed a2,
                                   int32_t b1b0, int32_t xxb2,
                                   GLint shift,
                                   GLfixed c)
{
#if defined(__arm__) && !defined(__thumb__)
                            
    GLfixed r;
    asm(
        "smulwb %0, %1, %4          \n"
        "smlawt %0, %2, %4, %0      \n" 
        "smlawb %0, %3, %5, %0      \n"
        "add    %0, %7, %0, lsl %6  \n"
        :   "=&r"(r)
        :   "r"(a0),
            "r"(a1),
            "r"(a2),
            "r"(b1b0), "r"(xxb2),
            "r"(shift),
            "r"(c)
        :
        ); 
    return r;
    
#else

    int32_t accum;
    int16_t b0 =  b1b0        & 0xffff;
    int16_t b1 = (b1b0 >> 16) & 0xffff;
    int16_t b2 =  xxb2        & 0xffff;
    accum  = int64_t(a0)*int16_t(b0) >> 16;
    accum += int64_t(a1)*int16_t(b1) >> 16;
    accum += int64_t(a2)*int16_t(b2) >> 16;
    accum = (accum << shift) + c;
    return accum;

#endif
}

static inline GLfixed mla3a16_btt( GLfixed a0,
                                   GLfixed a1,
                                   GLfixed a2,
                                   int32_t b1b0, int32_t b2xx,
                                   GLint shift,
                                   GLfixed c)
{
#if defined(__arm__) && !defined(__thumb__)
                            
    GLfixed r;
    asm(
        "smulwb %0, %1, %4          \n"
        "smlawt %0, %2, %4, %0      \n" 
        "smlawt %0, %3, %5, %0      \n"
        "add    %0, %7, %0, lsl %6  \n"
        :   "=&r"(r)
        :   "r"(a0),
            "r"(a1),
            "r"(a2),
            "r"(b1b0), "r"(b2xx),
            "r"(shift),
            "r"(c)
        :
        ); 
    return r;
    
#else

    int32_t accum;
    int16_t b0 =  b1b0        & 0xffff;
    int16_t b1 = (b1b0 >> 16) & 0xffff;
    int16_t b2 = (b2xx >> 16) & 0xffff;
    accum  = int64_t(a0)*int16_t(b0) >> 16;
    accum += int64_t(a1)*int16_t(b1) >> 16;
    accum += int64_t(a2)*int16_t(b2) >> 16;
    accum = (accum << shift) + c;
    return accum;

#endif
}

static inline GLfixed mla3( GLfixed a0, GLfixed b0,
                            GLfixed a1, GLfixed b1,
                            GLfixed a2, GLfixed b2)
{
#if defined(__arm__) && !defined(__thumb__)
                            
    GLfixed r;
    int32_t t;
    asm(
        "smull %0, %1, %2, %3       \n"
        "smlal %0, %1, %4, %5       \n"
        "smlal %0, %1, %6, %7       \n"
        "movs  %0, %0, lsr #16      \n"
        "adc   %0, %0, %1, lsl #16  \n"
        :   "=&r"(r), "=&r"(t) 
        :   "%r"(a0), "r"(b0),
            "%r"(a1), "r"(b1),
            "%r"(a2), "r"(b2)
        :   "cc"
        ); 
    return r;
    
#else

    return ((   int64_t(a0)*b0 +
                int64_t(a1)*b1 +
                int64_t(a2)*b2 + 0x8000)>>16);

#endif
}

static inline GLfixed mla4( GLfixed a0, GLfixed b0,
                            GLfixed a1, GLfixed b1,
                            GLfixed a2, GLfixed b2,
                            GLfixed a3, GLfixed b3)
{
#if defined(__arm__) && !defined(__thumb__)
                            
    GLfixed r;
    int32_t t;
    asm(
        "smull %0, %1, %2, %3       \n"
        "smlal %0, %1, %4, %5       \n"
        "smlal %0, %1, %6, %7       \n"
        "smlal %0, %1, %8, %9       \n"
        "movs  %0, %0, lsr #16      \n"
        "adc   %0, %0, %1, lsl #16  \n"
        :   "=&r"(r), "=&r"(t) 
        :   "%r"(a0), "r"(b0),
            "%r"(a1), "r"(b1),
            "%r"(a2), "r"(b2),
            "%r"(a3), "r"(b3)
        :   "cc"
        ); 
    return r;
    
#else

    return ((   int64_t(a0)*b0 +
                int64_t(a1)*b1 +
                int64_t(a2)*b2 +
                int64_t(a3)*b3 + 0x8000)>>16);

#endif
}

inline
GLfixed dot4(const GLfixed* a, const GLfixed* b) 
{
    return mla4(a[0], b[0], a[1], b[1], a[2], b[2], a[3], b[3]);
}


inline
GLfixed dot3(const GLfixed* a, const GLfixed* b) 
{
    return mla3(a[0], b[0], a[1], b[1], a[2], b[2]);
}

#define VFP_CLOBBER_S0_S31                                  \
    "s0", "s1", "s2", "s3", "s4", "s5", "s6", "s7", "s8",   \
    "s9", "s10", "s11", "s12", "s13", "s14", "s15", "s16",  \
    "s17", "s18", "s19", "s20", "s21", "s22", "s23", "s24", \
    "s25", "s26", "s27", "s28", "s29", "s30", "s31"

// Sets length and stride to 0.
#define VFP_VECTOR_LENGTH_ZERO          \
    "fmrx    r0, fpscr            \n\t" \
    "bic     r0, r0, #0x00370000  \n\t" \
    "fmxr    fpscr, r0            \n\t"

// Set vector length. VEC_LENGTH has to be bitween 0 for length 1 and 3 for length 4.
#define VFP_VECTOR_LENGTH(VEC_LENGTH) \
    "fmrx    r0, fpscr                         \n\t" \
    "bic     r0, r0, #0x00370000               \n\t" \
    "orr     r0, r0, #0x000" #VEC_LENGTH "0000 \n\t" \
    "fmxr    fpscr, r0                         \n\t"

#if defined(HAVE_ARM_VFP)
static inline void matrix_4x4_mul(const float *src_mat_1,
                                  const float *src_mat_2,
                                  float *dst_mat)
{
    asm volatile (
        VFP_VECTOR_LENGTH(3)

        // Interleaving loads and adds/muls for faster calculation.
        // Let A:=src_ptr_1, B:=src_ptr_2, then
        // function computes A*B as (B^T * A^T)^T.

        // Load the whole matrix into memory.
        "fldmias  %2, {s8-s23}    \n\t"
        // Load first column to scalar bank.
        "fldmias  %1!, {s0-s3}    \n\t"
        // First column times matrix.
        "fmuls s24, s8, s0        \n\t"
        "fmacs s24, s12, s1       \n\t"

        // Load second column to scalar bank.
        "fldmias %1!,  {s4-s7}    \n\t"

        "fmacs s24, s16, s2       \n\t"
        "fmacs s24, s20, s3       \n\t"
        // Save first column.
        "fstmias  %0!, {s24-s27}  \n\t"

        // Second column times matrix.
        "fmuls s28, s8, s4        \n\t"
        "fmacs s28, s12, s5       \n\t"

        // Load third column to scalar bank.
        "fldmias  %1!, {s0-s3}    \n\t"

        "fmacs s28, s16, s6       \n\t"
        "fmacs s28, s20, s7       \n\t"
        // Save second column.
        "fstmias  %0!, {s28-s31}  \n\t"

        // Third column times matrix.
        "fmuls s24, s8, s0        \n\t"
        "fmacs s24, s12, s1       \n\t"

        // Load fourth column to scalar bank.
        "fldmias %1,  {s4-s7}    \n\t"

        "fmacs s24, s16, s2       \n\t"
        "fmacs s24, s20, s3       \n\t"
        // Save third column.
        "fstmias  %0!, {s24-s27}  \n\t"

        // Fourth column times matrix.
        "fmuls s28, s8, s4        \n\t"
        "fmacs s28, s12, s5       \n\t"
        "fmacs s28, s16, s6       \n\t"
        "fmacs s28, s20, s7       \n\t"
        // Save fourth column.
        "fstmias  %0!, {s28-s31}  \n\t"

        VFP_VECTOR_LENGTH_ZERO
        : "=r" (dst_mat), "=r" (src_mat_2)
        : "r" (src_mat_1), "0" (dst_mat), "1" (src_mat_2)
        : "r0", "cc", "memory", VFP_CLOBBER_S0_S31
    );
}
#endif /* HAVE_ARM_VFP */

}; // namespace android

#endif // ANDROID_OPENGLES_MATRIX_H

