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

package com.android.systemui.statusbar.policy.toggles;

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
public class WifiAPToggle extends Toggle {

    private boolean mIsApOn;
    private int mWifiApState = WifiManager.WIFI_AP_STATE_DISABLED;

    public WifiAPToggle(Context context) {
        super(context);

        IntentFilter wifiFilter = new IntentFilter(WifiManager.WIFI_AP_STATE_CHANGED_ACTION);
        wifiFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        mContext.registerReceiver(mBroadcastReceiver, wifiFilter);

        setLabel(R.string.toggle_wifiap);
        setIcon(R.drawable.toggle_wifi_ap);
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            mWifiApState = intent
                    .getIntExtra(WifiManager.EXTRA_WIFI_AP_STATE, -1);
            updateState();

        }
    };

    public boolean isWifiOn(Context context) {
        WifiManager wifiManager = (WifiManager) context
                .getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            switch (wifiManager.getWifiState()) {
                case WifiManager.WIFI_AP_STATE_ENABLED:
                case WifiManager.WIFI_AP_STATE_ENABLING:
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
                int wifiState = wifiManager.getWifiState();
                if (desiredState
                        && ((wifiState == WifiManager.WIFI_STATE_ENABLING) || (wifiState == WifiManager.WIFI_STATE_ENABLED))) {
                    wifiManager.setWifiEnabled(false);
                }

                wifiManager.setWifiApEnabled(null, desiredState);
                return;
            }
        });
    }

    @Override
    protected void updateInternalToggleState() {
        WifiManager wifiManager = (WifiManager) mContext
                .getSystemService(Context.WIFI_SERVICE);

        mWifiApState = wifiManager.getWifiApState();

        switch (mWifiApState) {
            case WifiManager.WIFI_AP_STATE_ENABLED:
                mIsApOn = true;
                mToggle.setChecked(true);
                mToggle.setEnabled(true);
                break;
            default:
            case WifiManager.WIFI_AP_STATE_DISABLED:
                mIsApOn = false;
                mToggle.setChecked(false);
                mToggle.setEnabled(true);
                break;
            case WifiManager.WIFI_AP_STATE_DISABLING:
                mIsApOn = false;
                mToggle.setChecked(false);
                mToggle.setEnabled(false);
                break;
            case WifiManager.WIFI_AP_STATE_ENABLING:
                mIsApOn = false;
                mToggle.setChecked(true);
                mToggle.setEnabled(false);
                break;
        }

    }

    @Override
    protected void onCheckChanged(boolean isChecked) {
        if (isChecked != mIsApOn) {
            changeWifiState(isChecked);
        }

    }

    @Override
    protected boolean onLongPress() {
        Intent intent = new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
        return true;
    }

}
