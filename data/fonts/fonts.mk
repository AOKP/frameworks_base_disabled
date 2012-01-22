# Copyright (C) 2011 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# Warning: this is actually a product definition, to be inherited from

# On space-constrained devices, we include a subset of fonts:
# First, core/required fonts
PRODUCT_COPY_FILES := \
    frameworks/base/data/fonts/Roboto-Regular.ttf:system/fonts/Roboto-Regular.ttf \
    frameworks/base/data/fonts/Roboto-Bold.ttf:system/fonts/Roboto-Bold.ttf \
    frameworks/base/data/fonts/Roboto-Italic.ttf:system/fonts/Roboto-Italic.ttf \
    frameworks/base/data/fonts/Roboto-BoldItalic.ttf:system/fonts/Roboto-BoldItalic.ttf \
    frameworks/base/data/fonts/DroidSerif-Regular.ttf:system/fonts/DroidSerif-Regular.ttf \
    frameworks/base/data/fonts/DroidSerif-Bold.ttf:system/fonts/DroidSerif-Bold.ttf \
    frameworks/base/data/fonts/DroidSerif-Italic.ttf:system/fonts/DroidSerif-Italic.ttf \
    frameworks/base/data/fonts/DroidSerif-BoldItalic.ttf:system/fonts/DroidSerif-BoldItalic.ttf \
    frameworks/base/data/fonts/DroidSansMono.ttf:system/fonts/DroidSansMono.ttf \
    frameworks/base/data/fonts/Clockopia.ttf:system/fonts/Clockopia.ttf \
    frameworks/base/data/fonts/AndroidClock.ttf:system/fonts/AndroidClock.ttf \
    frameworks/base/data/fonts/AndroidClock_Highlight.ttf:system/fonts/AndroidClock_Highlight.ttf \
    frameworks/base/data/fonts/AndroidClock_Solid.ttf:system/fonts/AndroidClock_Solid.ttf \
    frameworks/base/data/fonts/system_fonts.xml:system/etc/system_fonts.xml \
    frameworks/base/data/fonts/fallback_fonts.xml:system/etc/fallback_fonts.xml

# Next, include additional fonts, depending on how much space we have.
# Details see module definitions in Android.mk.
PRODUCT_PACKAGES := \
    DroidSansFallback.ttf
