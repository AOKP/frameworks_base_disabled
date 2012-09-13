/*
 * Copyright (C) 2012 The CyanogenMod Project
 * Copyright (C) 2012 The LiquidSmooth Project
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

package com.android.internal.telephony;

import static com.android.internal.telephony.RILConstants.*;

import android.content.Context;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;

/**
 * Qualcomm RIL class for basebands that do not send the CSIM status
 * piggybacked in RIL_UNSOL_RESPONSE_RADIO_STATE_CHANGED. Instead,
 * these radios will send radio state and we have to query for CSIM
 * Custom Qualcomm CSIM/RUIM Ready RIL for HTC Mecha (ThunderBolt)
 *
 * {@hide}
 */
public class HTCMechaQualcommRIL extends QualcommSharedRIL implements CommandsInterface {
    protected IccHandler mIccHandler;
    protected final int RIL_INT_RADIO_OFF = 0;
    protected final int RIL_INT_RADIO_UAV = 1;
    protected final int RIL_INT_RADIO_ON = 13;

    public HTCMechaQualcommRIL(Context context, int networkMode, int cdmaSubscription) {
        super(context, networkMode, cdmaSubscription);
    }

    @Override
    public void
    iccIO(int command, int fileid, String path, int p1, int p2, int p3,
            String data, String pin2, Message result) {
        //Note: This RIL request has not been renamed to ICC,
        //       but this request is also valid for SIM and RUIM
        String IccAid;
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_SIM_IO, result);

        IccCardStatus status = new IccCardStatus();

        if (path != null) {
            if (path.indexOf("7F20") != -1) {
                IccAid = mAid;
                path = path = path.replaceAll("7F20","7FFF");
            } else {
                IccAid = mAid;
            }
        } else {
            IccAid = mAid;
        }

        rr.mp.writeInt(command);
        rr.mp.writeInt(fileid);
        rr.mp.writeString(path);
        rr.mp.writeInt(p1);
        rr.mp.writeInt(p2);
        rr.mp.writeInt(p3);
        rr.mp.writeString(data);
        rr.mp.writeString(pin2);
        rr.mp.writeString(IccAid);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> iccIO: "
                    + " aid: " + IccAid + " "
                    + requestToString(rr.mRequest)
                    + " 0x" + Integer.toHexString(command)
                    + " 0x" + Integer.toHexString(fileid) + " "
                    + " path: " + path + ","
                    + p1 + "," + p2 + "," + p3);

        send(rr);
    }

    @Override
    protected Object
    responseIccCardStatus(Parcel p) {
        IccCardApplication ca;

        IccCardStatus status = new IccCardStatus();
        status.setCardState(p.readInt());
        status.setUniversalPinState(p.readInt());
        status.setGsmUmtsSubscriptionAppIndex(p.readInt());
        status.setCdmaSubscriptionAppIndex(p.readInt());
        status.setImsSubscriptionAppIndex(p.readInt());
        int numApplications = p.readInt();

        // limit to maximum allowed applications
        if (numApplications > IccCardStatus.CARD_MAX_APPS) {
            numApplications = IccCardStatus.CARD_MAX_APPS;
        }
        status.setNumApplications(numApplications);

        for (int i = 0 ; i < numApplications ; i++) {
            ca = new IccCardApplication();
            ca.app_type       = ca.AppTypeFromRILInt(p.readInt());
            ca.app_state      = ca.AppStateFromRILInt(p.readInt());
            ca.perso_substate = ca.PersoSubstateFromRILInt(p.readInt());
            ca.aid            = p.readString();
            ca.app_label      = p.readString();
            ca.pin1_replaced  = p.readInt();
            ca.pin1           = ca.PinStateFromRILInt(p.readInt());
            ca.pin2           = ca.PinStateFromRILInt(p.readInt());
            status.addApplication(ca);
        }

        mAid = status.getApplication(status.getGsmUmtsSubscriptionAppIndex()).aid;

        return status;
    }

    @Override
    protected Object
    responseSignalStrength(Parcel p) {
        int numInts = 14;
        int response[];

        /* HTC signal strength format:
         * 0: GW_SignalStrength
         * 1: GW_SignalStrength.bitErrorRate
         * 2: CDMA_SignalStrength.dbm
         * 3: CDMA_SignalStrength.ecio
         * 4: EVDO_SignalStrength.dbm
         * 5: EVDO_SignalStrength.ecio
         * 6: EVDO_SignalStrength.signalNoiseRatio
         * 7: ATT_SignalStrength.dbm
         * 8: ATT_SignalStrength.ecno
         * 9: LTE_SignalStrength.signalStrength
         * 10: LTE_SignalStrength.rsrp
         * 11: LTE_SignalStrength.rsrq
         * 12: LTE_SignalStrength.rssnr
         * 13: LTE_SignalStrength.cqi
         */
        response = new int[numInts];
        for (int i = 0; i < numInts; i++) {
            if (i > 8) {
                response[i-2] = p.readInt();
                response[i] = -1;
            } else {
                response[i] = p.readInt();
            }
        }

        return response;
    }

    @Override
    protected void
    processUnsolicited (Parcel p) {
        Object ret;
        int dataPosition = p.dataPosition(); // save off position within the Parcel
        int response = p.readInt();

        switch (response) {
            //case RIL_UNSOL_RESPONSE_RADIO_STATE_CHANGED: ret =  responseVoid(p); break;
            case 21004: ret = responseVoid(p); break; // RIL_UNSOL_VOICE_RADIO_TECH_CHANGED
            case 21005: ret = responseVoid(p); break; // RIL_UNSOL_IMS_NETWORK_STATE_CHANGED
            case 21007: ret = responseVoid(p); break; // RIL_UNSOL_DATA_NETWORK_STATE_CHANGED

            default:
                // Rewind the Parcel
                p.setDataPosition(dataPosition);
                // Forward responses that we are not overriding to the super class
                super.processUnsolicited(p);
                return;
        }

        switch (response) {
            case RIL_UNSOL_RESPONSE_RADIO_STATE_CHANGED:
                int state = p.readInt();
                setRadioStateFromRILInt(state);
                break;
            case 21004:
            case 21005:
            case 21007:
                if (RILJ_LOGD) unsljLogRet(response, ret);

                if (mExitEmergencyCallbackModeRegistrants != null) {
                    mExitEmergencyCallbackModeRegistrants.notifyRegistrants(
                                        new AsyncResult (null, null, null));
                }
                break;
         }
    }

    protected void setRadioStateFromRILInt (int stateCode) {
        CommandsInterface.RadioState radioState;
        HandlerThread handlerThread;
        IccHandler iccHandler;
        Looper looper;

        switch (stateCode) {
            case RIL_INT_RADIO_OFF:
                radioState = CommandsInterface.RadioState.RADIO_OFF;
                if (mIccHandler != null) {
                    mIccThread = null;
                    mIccHandler = null;
                }
                break;
            case RIL_INT_RADIO_UAV:
                radioState = CommandsInterface.RadioState.RADIO_UNAVAILABLE;
                break;
            case RIL_INT_RADIO_ON:
                if (mIccHandler == null) {
                    handlerThread = new HandlerThread("IccHandler");
                    mIccThread = handlerThread;
                    mIccThread.start();

                    looper = mIccThread.getLooper();
                    mIccHandler = new IccHandler(this,looper);
                    mIccHandler.run();
                }
		        radioState = CommandsInterface.RadioState.RADIO_ON;
                break;
            default:
                throw new RuntimeException("Unrecognized RIL_RadioState: " + stateCode);
        }

        setRadioState(radioState);
    }

    class IccHandler extends Handler implements Runnable {
        private static final int EVENT_RADIO_ON = 1;
        private static final int EVENT_ICC_STATUS_CHANGED = 2;
        private static final int EVENT_GET_ICC_STATUS_DONE = 3;
        private static final int EVENT_RADIO_OFF_OR_UNAVAILABLE = 4;

        private RIL mRil;
        private boolean mRadioOn = false;

        public IccHandler (RIL ril, Looper looper) {
            super (looper);
            mRil = ril;
        }

        public void handleMessage (Message paramMessage) {
            switch (paramMessage.what) {
                case EVENT_RADIO_ON:
                    mRadioOn = true;
                    Log.d(LOG_TAG, "Radio On -> Forcing Sim Status Update");
                    sendMessage(obtainMessage(EVENT_ICC_STATUS_CHANGED));
                    break;
                case EVENT_GET_ICC_STATUS_DONE:
                    AsyncResult asyncResult = (AsyncResult) paramMessage.obj;
                    if (asyncResult.exception != null) {
                        Log.e(LOG_TAG, "IccCardStatusDone Should NOT Return Exceptions!", asyncResult.exception);
                        break;
                    }
                    IccCardStatus status = (IccCardStatus) asyncResult.result;
                    if (status.getNumApplications() == 0) {
                        if (!mRil.getRadioState().isOn()) {
                            break;
                        }
			            mRil.setRadioState(CommandsInterface.RadioState.RADIO_ON);
                    } else {
                        int appIndex = -1;
                        appIndex = status.getGsmUmtsSubscriptionAppIndex();
                        IccCardApplication application = status.getApplication(appIndex);
                        IccCardApplication.AppState app_state = application.app_state;
                        IccCardApplication.AppType app_type = application.app_type;

                        switch (app_state) {
                            case APPSTATE_PIN:
                            case APPSTATE_PUK:
                                switch (app_type) {
                                    case APPTYPE_CSIM:
                                    case APPTYPE_RUIM:
					                    mRil.setRadioState(CommandsInterface.RadioState.RADIO_ON);
                                        break;
                                    default:
                                        Log.e(LOG_TAG, "We DO NOT Handle ICC Of Type: " + app_type);
                                        return;
                                }
                                break;
                            case APPSTATE_READY:
                                switch (app_type) {
                                    case APPTYPE_CSIM:
                                    case APPTYPE_RUIM:
					                    mRil.setRadioState(CommandsInterface.RadioState.RADIO_ON);
                                        break;
                                    default:
                                        Log.e(LOG_TAG, "We DO NOT Handle ICC Of Type: " + app_type);
                                        return;
                                }
                                break;
                            default:
                                return;
                        }
                    }
                    break;
                case EVENT_ICC_STATUS_CHANGED:
                    if (mRadioOn) {
                        Log.d(LOG_TAG, "Received EVENT_ICC_STATUS_CHANGED, GetIccCardStatus");
                        mRil.getIccCardStatus(obtainMessage(EVENT_GET_ICC_STATUS_DONE, paramMessage.obj));
                    } else {
                        Log.d(LOG_TAG, "Received EVENT_ICC_STATUS_CHANGED While Radio Is OFF");
                    }
                    break;
                case EVENT_RADIO_OFF_OR_UNAVAILABLE:
                    mRadioOn = false;
                    break;
                default:
                    Log.e(LOG_TAG, "Unknown Event " + paramMessage.what);
                    break;
            }
        }

        public void run () {
            mRil.registerForIccStatusChanged(this, EVENT_ICC_STATUS_CHANGED, null);
            Message msg = obtainMessage(EVENT_RADIO_ON);
            mRil.getIccCardStatus(msg);
        }
    }
}
