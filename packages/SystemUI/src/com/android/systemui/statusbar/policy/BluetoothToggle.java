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

package com.android.systemui.statusbar.policy;

import java.util.ArrayList;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;

import com.android.systemui.R;

public class BluetoothToggle extends BroadcastReceiver implements
        CompoundButton.OnCheckedChangeListener {
    private static final String TAG = "StatusBar.BluetoothController";

    private Context mContext;

    private boolean mEnabled = false;
    private CompoundButton mBluetooth;
    private boolean mSystemChanged = false;

    public BluetoothToggle(Context context, CompoundButton b) {
        mContext = context;
        mBluetooth = b;

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
        context.registerReceiver(this, filter);

        final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {
            handleAdapterStateChange(adapter.getState());
            handleConnectionStateChange(adapter.getConnectionState());
        }

        mBluetooth.setOnCheckedChangeListener(this);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();

        if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
            handleAdapterStateChange(intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                    BluetoothAdapter.ERROR));
        } else if (action.equals(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)) {
            handleConnectionStateChange(intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE,
                    BluetoothAdapter.STATE_DISCONNECTED));
        }
    }

    public void handleAdapterStateChange(int adapterState) {
        mEnabled = (adapterState == BluetoothAdapter.STATE_ON);
        handleConnectionStateChange(adapterState);
    }

    public void handleConnectionStateChange(int connectionState) {
        mSystemChanged = true;

        switch (connectionState) {
            case BluetoothAdapter.STATE_ON:
                mEnabled = true;
                mBluetooth.setChecked(true);
                mBluetooth.setEnabled(true);
                break;
            case BluetoothAdapter.STATE_TURNING_ON:
                mBluetooth.setChecked(true);
                mBluetooth.setEnabled(false);
                break;
            case BluetoothAdapter.STATE_TURNING_OFF:
                mBluetooth.setChecked(false);
                mBluetooth.setEnabled(false);
                break;
            default:
            case BluetoothAdapter.STATE_OFF:
                mEnabled = false;
                mBluetooth.setChecked(false);
                mBluetooth.setEnabled(true);
                break;
        }

        mSystemChanged = false;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (mSystemChanged)
            return;

        final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {
            if (isChecked)
                adapter.enable();
            else
                adapter.disable();
        }
    }

}
