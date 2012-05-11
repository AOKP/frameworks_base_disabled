/*
 * Copyright (C) Texas Instruments - http://www.ti.com/
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

#define LOG_TAG "APE_TAG"
#include <utils/Log.h>

#include "include/APE.h"

namespace android {

APE::APE(){

}

APE::~APE(){

}

bool APE::isAPE(uint8_t *apeTag) const {
    if(apeTag[0] == 'A' && apeTag[1] == 'P' && apeTag[2] == 'E' &&
        apeTag[3] == 'T' && apeTag[4] == 'A' && apeTag[5] == 'G' &&
        apeTag[6] == 'E' && apeTag[7] == 'X'){
        return true;
    }
    return false;
}

size_t sizeItemKey(const sp<DataSource> &source, off64_t offset){
    off64_t ItemKeyOffset = offset;
    uint8_t keyTerminator;
    size_t keySize = 0;
    while (keyTerminator != 0){
        source->readAt(ItemKeyOffset, &keyTerminator, 1);
        ItemKeyOffset++;
        keySize++;
    }
    return keySize - 1;
}

bool APE::parceAPE(const sp<DataSource> &source, off64_t offset,
        size_t* headerSize, sp<MetaData> &meta){

    struct Map {
            int key;
            const char *tag;
    } const kMap[] = {
            { kKeyAlbum, "Album" },
            { kKeyArtist, "Artist" },
            { kKeyAlbumArtist, "Album" },
            { kKeyComposer, "Composer" },
            { kKeyGenre, "Genre" },
            { kKeyTitle, "Title" },
            { kKeyYear, "Year" },
            { kKeyCDTrackNumber, "Track" },
            { kKeyDate, "Record Date"},
    };

    static const size_t kNumMapEntries = sizeof(kMap) / sizeof(kMap[0]);

    off64_t headerOffset = offset;
    headerOffset += 16;
    itemNumber = 0;
    if (source->readAt(headerOffset, &itemNumber, 1) == 0)
        return false;

    headerOffset += 16;

    for(uint32_t it = 0; it < itemNumber; it++){
        lenValue = 0;
        if (source->readAt(headerOffset, &lenValue, 1) == 0)
            return false;

        headerOffset += 4;

        itemFlags = 0;
        if (source->readAt(headerOffset, &itemFlags, 1) == 0)
            return false;

        headerOffset += 4;

        size_t sizeKey = sizeItemKey(source, headerOffset);

        char *key = new char[sizeKey];

        if (source->readAt(headerOffset, key, sizeKey) == 0)
            return false;

        key[sizeKey] = '\0';
        headerOffset += sizeKey + 1;

        char *val = new char[lenValue + 1];

        if (source->readAt(headerOffset, val, lenValue) == 0)
            return false;

        val[lenValue] = '\0';

        for (size_t i = 0; i < kNumMapEntries; i++){
            if (!strcmp(key, kMap[i].tag)){
                if (itemFlags == 0)
                    meta->setCString(kMap[i].key, (const char *)val);
                break;
            }
        }
        headerOffset += lenValue;
        delete[] key;
        delete[] val;
    }

    return true;
}
} //namespace android
