/*
 * Copyright (C) 2008 The Android Open Source Project
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

#ifndef ANDROID_HARDWARE_CAMERA_PARAMETERS_BASE_H
#define ANDROID_HARDWARE_CAMERA_PARAMETERS_BASE_H

#include <utils/KeyedVector.h>
#include <utils/String8.h>

namespace android {

struct Size {
    int width;
    int height;

    Size() {
        width = 0;
        height = 0;
    }

    Size(int w, int h) {
        width = w;
        height = h;
    }
};

class CameraParametersBase
{
public:
    CameraParametersBase();
    CameraParametersBase(const String8 &params) { unflatten(params); }
    virtual ~CameraParametersBase();

    String8 flatten() const;
    void unflatten(const String8 &params);

    void set(const char *key, const char *value);
    void set(const char *key, int value);
    void setFloat(const char *key, float value);
    const char *get(const char *key) const;
    int getInt(const char *key) const;
    float getFloat(const char *key) const;

    void remove(const char *key);

    void dump() const;
    status_t dump(int fd, const Vector<String16>& args) const;

private:
    DefaultKeyedVector<String8,String8>    mMap;
};

}; // namespace android

#endif
