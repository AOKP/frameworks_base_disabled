/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.systemui.statusbar.toggles;

import com.android.systemui.R;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;

/**
 * TODO: Listen for changes to the setting.
 */
public class WifiToggle extends Toggle {

    private boolean mIsWifiOn;
    private int mWifiState = WifiManager.WIFI_STATE_UNKNOWN;

    public WifiToggle(Context context) {
        super(context);

        IntentFilter wifiFilter = new IntentFilter(
                WifiManager.WIFI_STATE_CHANGED_ACTION);
        mContext.registerReceiver(mBroadcastReceiver, wifiFilter);

        setLabel(R.string.toggle_wifi);
        updateState();
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (!WifiManager.WIFI_STATE_CHANGED_ACTION.equals(intent
                    .getAction())) {
                return;
            }
            mWifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, -1);
            updateState();

        }
    };

    public boolean isWifiOn(Context context) {
        WifiManager wifiManager = (WifiManager) context
                .getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            switch (wifiManager.getWifiState()) {
                case WifiManager.WIFI_STATE_ENABLED:
                case WifiManager.WIFI_STATE_ENABLING:
                    return true;
                default:
                    return false;
            }
        }
        return false;
    }

    private void changeWifiState(final boolean desiredState) {
        final WifiManager wifiManager = (WifiManager) mContext
                .getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null) {
            Log.d("WifiButton", "No wifiManager.");
            return;
        }

        AsyncTask.execute(new Runnable() {
            public void run() {
                int wifiApState = wifiManager.getWifiApState();
                if (desiredState
                        && ((wifiApState == WifiManager.WIFI_AP_STATE_ENABLING) || (wifiApState == WifiManager.WIFI_AP_STATE_ENABLED))) {
                    wifiManager.setWifiApEnabled(null, false);
                }

                wifiManager.setWifiEnabled(desiredState);
                return;
            }
        });
    }

    @Override
    protected boolean updateInternalToggleState() {
        final WifiManager wifiManager = (WifiManager) mContext
                .getSystemService(Context.WIFI_SERVICE);
        mWifiState = wifiManager.getWifiState();

        switch (mWifiState) {
            case WifiManager.WIFI_STATE_ENABLED:
                mIsWifiOn = true;
                mToggle.setChecked(true);
                mToggle.setEnabled(true);
                break;
            case WifiManager.WIFI_STATE_DISABLED:
                mIsWifiOn = false;
                mToggle.setChecked(false);
                mToggle.setEnabled(true);
                break;
            case WifiManager.WIFI_STATE_DISABLING:
                mIsWifiOn = false;
                mToggle.setChecked(false);
                mToggle.setEnabled(false);
                break;
            case WifiManager.WIFI_STATE_ENABLING:
                mIsWifiOn = false;
                mToggle.setChecked(true);
                mToggle.setEnabled(false);
                break;
            case WifiManager.WIFI_STATE_UNKNOWN:
            default:
                mToggle.setChecked(false);
                mToggle.setEnabled(false);
                break;
        }
        if (mToggle.isChecked()) {
            setIcon(R.drawable.toggle_wifi);
        } else {
            setIcon(R.drawable.toggle_wifi_off);
        }
        return mToggle.isChecked();
    }

    @Override
    protected void onCheckChanged(boolean isChecked) {
        if (isChecked != mIsWifiOn) {
            changeWifiState(isChecked);
        }
        updateState();
    }

    @Override
    protected boolean onLongPress() {
        Intent intent = new Intent(
                android.provider.Settings.ACTION_WIFI_SETTINGS);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
        return true;
    }

}