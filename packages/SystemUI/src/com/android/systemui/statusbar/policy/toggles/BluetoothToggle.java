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

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.AttributeSet;

import com.android.systemui.R;

public class BluetoothToggle extends Toggle {

    private int mAdapterState = BluetoothAdapter.STATE_OFF;

    public BluetoothToggle(Context context) {
        super(context);

        final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {
            mAdapterState = adapter.getState();
            updateState();
        }

        context.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mAdapterState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.STATE_OFF);
                updateState();
            }
        }, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        setLabel(R.string.toggle_bt);
        setIcon(R.drawable.toggle_bluetooth);
    }

    @Override
    protected void onCheckChanged(boolean isChecked) {
        final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {
            if (isChecked)
                adapter.enable();
            else
                adapter.disable();
        }

    }

    @Override
    protected void updateInternalToggleState() {
        switch (mAdapterState) {
            case BluetoothAdapter.STATE_ON:
                mToggle.setChecked(true);
                mToggle.setEnabled(true);
                break;
            case BluetoothAdapter.STATE_TURNING_ON:
                mToggle.setChecked(true);
                mToggle.setEnabled(false);
                break;
            case BluetoothAdapter.STATE_TURNING_OFF:
                mToggle.setChecked(false);
                mToggle.setEnabled(false);
                break;
            default:
            case BluetoothAdapter.STATE_OFF:
                mToggle.setChecked(false);
                mToggle.setEnabled(true);
                break;
        }
    }

}
