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
import android.net.ConnectivityManager;
import android.util.AttributeSet;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.android.systemui.R;

public class NetworkToggle extends Toggle {

    public NetworkToggle(Context context) {
        super(context);
        updateState();
        setLabel(R.string.toggle_data);
        setIcon(R.drawable.toggle_data);
    }

    private boolean isMobileDataEnabled() {
        ConnectivityManager cm = (ConnectivityManager) mContext
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getMobileDataEnabled();
    }

    private void setMobileDataEnabled(boolean on) {
        ConnectivityManager cm = (ConnectivityManager) mContext
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        cm.setMobileDataEnabled(on);
    }

    @Override
    protected void onCheckChanged(boolean isChecked) {
        final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {
            setMobileDataEnabled(isChecked);
        }
    }

    protected BroadcastReceiver getBroadcastReceiver() {
        return new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();

                if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                    updateState();
                }
            }
        };
    }

    protected IntentFilter getIntentFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        return filter;
    }

    @Override
    protected void updateInternalToggleState() {
        mToggle.setChecked(isMobileDataEnabled());
    }
}
