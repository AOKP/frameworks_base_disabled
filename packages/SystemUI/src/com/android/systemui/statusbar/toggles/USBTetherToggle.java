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

package com.android.systemui.statusbar.toggles;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.net.ConnectivityManager;

import com.android.systemui.R;

public class USBTetherToggle extends Toggle {

    private boolean mUsbConnected;
    private boolean mMassStorageActive;
    private String[] mUsbRegexs;
    private TetherChangeReceiver mTetherChangeReceiver;

    public USBTetherToggle(Context context) {
        super(context);
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        mUsbRegexs = cm.getTetherableUsbRegexs();
        setLabel(R.string.toggle_tether);

        mTetherChangeReceiver = new TetherChangeReceiver();

        IntentFilter filter = new IntentFilter(ConnectivityManager.ACTION_TETHER_STATE_CHANGED);
        Intent intent = context.registerReceiver(mTetherChangeReceiver, filter);

        filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_STATE);
        context.registerReceiver(mTetherChangeReceiver, filter);

        filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MEDIA_SHARED);
        filter.addAction(Intent.ACTION_MEDIA_UNSHARED);
        filter.addDataScheme("file");
        context.registerReceiver(mTetherChangeReceiver, filter);

        updateState();
    }

    private void setUsbTethering(boolean enabled) {
        ConnectivityManager cm =
                (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm.setUsbTethering(enabled) != ConnectivityManager.TETHER_ERROR_NO_ERROR) {
            mToggle.setChecked(false);
            return;
        }

    }

    private void updateUsbState(String[] available, String[] tethered,
            String[] errored) {
        ConnectivityManager cm =
                (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        boolean usbAvailable = mUsbConnected && !mMassStorageActive;
        int usbError = ConnectivityManager.TETHER_ERROR_NO_ERROR;
        for (String s : available) {
            for (String regex : mUsbRegexs) {
                if (s.matches(regex)) {
                    if (usbError == ConnectivityManager.TETHER_ERROR_NO_ERROR) {
                        usbError = cm.getLastTetherError(s);
                    }
                }
            }
        }
        boolean usbTethered = false;
        for (String s : tethered) {
            for (String regex : mUsbRegexs) {
                if (s.matches(regex))
                    usbTethered = true;
            }
        }
        boolean usbErrored = false;
        for (String s : errored) {
            for (String regex : mUsbRegexs) {
                if (s.matches(regex))
                    usbErrored = true;
            }
        }

        if (usbTethered) {
            // mToggle.setSummary(R.string.usb_tethering_active_subtext);
            mToggle.setEnabled(true);
            mToggle.setChecked(true);
        } else if (usbAvailable) {
            if (usbError == ConnectivityManager.TETHER_ERROR_NO_ERROR) {
                // mToggle.setSummary(R.string.usb_tethering_available_subtext);
            } else {
                // mToggle.setSummary(R.string.usb_tethering_errored_subtext);
            }
            mToggle.setEnabled(true);
            mToggle.setChecked(false);
        } else if (usbErrored) {
            // mToggle.setSummary(R.string.usb_tethering_errored_subtext);
            mToggle.setEnabled(false);
            mToggle.setChecked(false);
        } else if (mMassStorageActive) {
            // mToggle.setSummary(R.string.usb_tethering_storage_active_subtext);
            mToggle.setEnabled(false);
            mToggle.setChecked(false);
        } else {
            // mToggle.setSummary(R.string.usb_tethering_unavailable_subtext);
            mToggle.setEnabled(false);
            mToggle.setChecked(false);
        }
    }

    @Override
    protected void onCheckChanged(boolean isChecked) {
        setUsbTethering(isChecked);
        updateState();
    }

    @Override
    protected boolean updateInternalToggleState() {
        ConnectivityManager cm =
                (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

        String[] available = cm.getTetherableIfaces();
        String[] tethered = cm.getTetheredIfaces();
        String[] errored = cm.getTetheringErroredIfaces();
        updateUsbState(available, tethered, errored);

        if (mToggle.isChecked()) {
            setIcon(R.drawable.toggle_tether);
        } else {
            setIcon(R.drawable.toggle_tether_off);
        }
        return mToggle.isChecked();
    }

    @Override
    protected boolean onLongPress() {
        Intent intent = new Intent(
                android.provider.Settings.ACTION_WIRELESS_SETTINGS);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
        return true;
    }

    private class TetherChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context content, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ConnectivityManager.ACTION_TETHER_STATE_CHANGED)) {
                updateState();
            } else if (action.equals(Intent.ACTION_MEDIA_SHARED)) {
                mMassStorageActive = true;
                updateState();
            } else if (action.equals(Intent.ACTION_MEDIA_UNSHARED)) {
                mMassStorageActive = false;
                updateState();
            } else if (action.equals(UsbManager.ACTION_USB_STATE)) {
                mUsbConnected = intent.getBooleanExtra(UsbManager.USB_CONNECTED, false);
                updateState();
            }
        }
    }
}