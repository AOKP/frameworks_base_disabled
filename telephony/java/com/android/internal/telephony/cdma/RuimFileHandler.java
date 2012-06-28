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

package com.android.internal.telephony.cdma;

import android.os.*;
import android.util.Log;

import com.android.internal.telephony.IccConstants;
import com.android.internal.telephony.IccException;
import com.android.internal.telephony.IccFileHandler;
import com.android.internal.telephony.IccFileTypeMismatch;
import com.android.internal.telephony.IccIoResult;
import com.android.internal.telephony.IccUtils;
import com.android.internal.telephony.PhoneProxy;
import com.android.internal.telephony.TelephonyProperties;
import android.os.SystemProperties;

import java.util.ArrayList;

/**
 * {@hide}
 */
public final class RuimFileHandler extends IccFileHandler {
    static final String LOG_TAG = "CDMA";

    private boolean mMotoNewArch = false;


    //***** Instance Variables

    //***** Constructor
    RuimFileHandler(CDMAPhone phone) {
        super(phone);
        mMotoNewArch = SystemProperties.getBoolean(TelephonyProperties.PROPERTY_MOTO_NEWARCH, false);
    }

    public void dispose() {
    }

    protected void finalize() {
        Log.d(LOG_TAG, "RuimFileHandler finalized");
    }

    //***** Overridden from IccFileHandler

    @Override
    public void loadEFImgTransparent(int fileid, int highOffset, int lowOffset,
            int length, Message onLoaded) {
        Message response = obtainMessage(EVENT_READ_ICON_DONE, fileid, 0,
                onLoaded);

        phone.mCM.iccIO(COMMAND_GET_RESPONSE, fileid, "img", 0, 0,
                GET_RESPONSE_EF_IMG_SIZE_BYTES, null, null, response);
    }

    @Override
    public void loadEFTransparent(int fileid, Message message) {
        if (fileid == EF_CSIM_EPRL) {
            Message response = obtainMessage(EVENT_READ_BINARY_DONE, fileid, 0, message);

            phone.mCM.iccIO(COMMAND_READ_BINARY, fileid, getEFPath(fileid), 0, 0,
                READ_RECORD_MODE_ABSOLUTE, null, null, response);
        } else {
            super.loadEFTransparent(fileid, message);
        }
    }

    @Override
    public void handleMessage(Message msg) {

        super.handleMessage(msg);
    }

    protected String getEFPath(int efid) {
        if (mMotoNewArch) {
            switch(efid) {
            case EF_CSIM_IMSIM:
            case EF_CSIM_CDMAHOME:
            case EF_CSIM_LI:
            case EF_RUIM_SPN:
            case EF_CSIM_MDN:
            case EF_CSIM_EPRL:
            case EF_CSIM_SF_EUIMID:
                Log.d(LOG_TAG, "[CsimFileHandler] getEFPath for " + efid);
                return MF_SIM + DF_ADFISIM;
            }
        }
        else {
            switch(efid) {
            case EF_SMS:
            case EF_CST:
            case EF_RUIM_SPN:
                return MF_SIM + DF_CDMA;
            }
        }
        return getCommonIccEFPath(efid);
    }

    protected void logd(String msg) {
        Log.d(LOG_TAG, "[RuimFileHandler] " + msg);
    }

    protected void loge(String msg) {
        Log.e(LOG_TAG, "[RuimFileHandler] " + msg);
    }

}
