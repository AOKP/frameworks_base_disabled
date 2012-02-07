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

package com.android.systemui.statusbar.policy.toggles;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.server.PowerSaverService;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.telephony.Phone;
import com.android.systemui.R;

public class LteToggle extends Toggle {

    private static final String TAG = "Toggle.Lte";

    private int mNetworkMode = -1;
    private boolean isCdma = false;

    public LteToggle(Context c) {
        super(c);

        SettingsObserver obs = new SettingsObserver(new Handler());
        obs.observe();
        setLabel(R.string.toggle_lte);
//        if (mToggle.isChecked())
        	setIcon(R.drawable.toggle_lte);
//        else
//        	setIcon(R.drawable.toggle_lte_off);

    }

    @Override
    protected void onCheckChanged(boolean isChecked) {
        TelephonyManager tm = (TelephonyManager) mView.getContext()
                .getSystemService(Context.TELEPHONY_SERVICE);
        tm.toggleLTE(isChecked);
//        if (isChecked)
        	setIcon(R.drawable.toggle_lte);
//        else
//        	setIcon(R.drawable.toggle_lte_off);
    }

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(
                    Settings.Secure.getUriFor(Settings.Secure.PREFERRED_NETWORK_MODE), false,
                    this);
            updateState();
        }

        @Override
        public void onChange(boolean selfChange) {
            mNetworkMode = Settings.Secure.getInt(mContext.getContentResolver(),
                    Settings.Secure.PREFERRED_NETWORK_MODE, Phone.PREFERRED_NT_MODE);

            updateState();
        }
    }

    private static int getCurrentPreferredNetworkMode(Context context) {
        int network = -1;
        try {
            network = Settings.Secure.getInt(context.getContentResolver(),
                    Settings.Secure.PREFERRED_NETWORK_MODE);
        } catch (SettingNotFoundException e) {
            e.printStackTrace();
        }
        return network;
    }

    private void requestPhoneStateChange(int newState) {
        if (!isValidNetwork(newState)) {
            Log.e(TAG, "attempting to switch to an invalid network type: " + newState);
            Log.e(TAG, "Phone CDMA status: " + isCdma);
            return;
        }

        Log.i(TAG, "Sending request to change phone network mode to: " + newState);
        Intent i = new Intent(PowerSaverService.ACTION_MODIFY_NETWORK_MODE);
        i.putExtra(PowerSaverService.EXTRA_NETWORK_MODE, newState);
        mContext.sendBroadcast(i);
    }

    private boolean isValidNetwork(int networkType) {
        TelephonyManager telephony = (TelephonyManager) mContext
                .getSystemService(Context.TELEPHONY_SERVICE);

        isCdma = (telephony.getCurrentPhoneType() == Phone.PHONE_TYPE_CDMA);

        switch (networkType) {
            case Phone.NT_MODE_CDMA:
            case Phone.NT_MODE_CDMA_NO_EVDO:
            case Phone.NT_MODE_EVDO_NO_CDMA:
            case Phone.NT_MODE_GLOBAL:
            case Phone.NT_MODE_LTE_ONLY:
                return (isCdma);
            case Phone.NT_MODE_GSM_ONLY:
            case Phone.NT_MODE_GSM_UMTS:
            case Phone.NT_MODE_WCDMA_ONLY:
            case Phone.NT_MODE_WCDMA_PREF:
                return (!isCdma);
        }
        return false;
    }

    @Override
    protected void updateInternalToggleState() {
        mNetworkMode = getCurrentPreferredNetworkMode(mContext);
        if (mToggle != null)
            mToggle.setChecked(mNetworkMode == Phone.NT_MODE_GLOBAL);
//        if (mToggle.isChecked())
        	setIcon(R.drawable.toggle_lte);
//        else
//        	setIcon(R.drawable.toggle_lte_off);
    }
    
    @Override
    protected boolean onLongPress() {
    	Intent intent = new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
    	return true;
    }
}
