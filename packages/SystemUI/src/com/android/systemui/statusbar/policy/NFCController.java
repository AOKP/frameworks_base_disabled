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

package com.android.systemui.statusbar.policy;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.util.Slog;
import android.widget.CompoundButton;

public class NFCController extends BroadcastReceiver
        implements CompoundButton.OnCheckedChangeListener {
    private static final String TAG = "StatusBar.NFCController";

    private Context mContext;
    private CompoundButton mCheckBox;

    private boolean mNfcEnabled;
    private NfcAdapter mNfcAdapter;

    public NFCController(Context context, CompoundButton checkbox) {
        mContext = context;
        mNfcAdapter = NfcAdapter.getDefaultAdapter(context);
        mNfcEnabled = getNfcState();
        mCheckBox = checkbox;
        checkbox.setChecked(mNfcEnabled);
        checkbox.setOnCheckedChangeListener(this);

        IntentFilter filter = new IntentFilter();
        filter.addAction(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED);
        context.registerReceiver(this, filter);

    }

    public void release() {
        mContext.unregisterReceiver(this);
    }

    public void onCheckedChanged(CompoundButton view, boolean checked) {
        if (checked != mNfcEnabled) {
            mNfcEnabled = checked;
            setNfcState(mNfcEnabled);
        }
    }

    public void onReceive(Context context, Intent intent) {
        if (NfcAdapter.ACTION_ADAPTER_STATE_CHANGED.equals(intent.getAction())) {
            final boolean enabled = (intent.getIntExtra(NfcAdapter.EXTRA_ADAPTER_STATE,
                    NfcAdapter.STATE_OFF) == NfcAdapter.STATE_ON);
            if (enabled != mNfcEnabled) {
                mNfcEnabled = enabled;
                mCheckBox.setChecked(enabled);
            }
        }
    }

    private boolean getNfcState() {
        return mNfcAdapter.isEnabled();
    }

    private void setNfcState(final boolean desiredState) {
        AsyncTask.execute(new Runnable() {
            public void run() {
                if (desiredState) {
                    mNfcAdapter.enable();
                } else {
                    mNfcAdapter.disable();
                }
                return;
            }
        });
    }
}

