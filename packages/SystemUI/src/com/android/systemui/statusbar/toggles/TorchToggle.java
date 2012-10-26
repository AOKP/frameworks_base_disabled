/*
 * Copyright (C) 2010 The Android Open Source Project
 * Copyright 2011 Colin McDonough
 *
 * Modified for AOKP by Mike Wilson (Zaphod-Beeblebrox)
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

import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import com.android.systemui.R;

public class TorchToggle extends Toggle {

    private static final String TAG = "TorchToggle";
    public static final String INTENT_TORCH_ON = "com.aokp.torch.INTENT_TORCH_ON";
    public static final String INTENT_TORCH_OFF = "com.aokp.torch.INTENT_TORCH_OFF";

    private boolean mIsTorchOn;
    private Context mContext;

    public TorchToggle(Context context) {
        super(context);
        setLabel(R.string.toggle_torch);
        if (mToggle.isChecked())
            setIcon(R.drawable.toggle_torch);
        else
            setIcon(R.drawable.toggle_torch_off);
        mContext = context;
        mIsTorchOn = Settings.System.getBoolean(mContext.getContentResolver(),
                Settings.System.TORCH_STATE, false);
        TorchObserver mTorchObserver = new TorchObserver(new Handler());
        updateState();
    }

    @Override
    protected boolean updateInternalToggleState() {
        mToggle.setChecked(mIsTorchOn);
        if (mToggle.isChecked()) {
            setIcon(R.drawable.toggle_torch);
            return true;
        } else {
            setIcon(R.drawable.toggle_torch_off);
            return false;
        }
    }

    @Override
    protected void onCheckChanged(boolean isChecked) {
        if (isChecked) {
            mContext.sendBroadcast(new Intent(INTENT_TORCH_ON));
        } else {
            mContext.sendBroadcast(new Intent(INTENT_TORCH_OFF));
        }
    }

    @Override
    protected boolean onLongPress() {
        return false;
    }

    protected class TorchObserver extends ContentObserver {
        TorchObserver(Handler handler) {
            super(handler);
            observe();
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.TORCH_STATE), false, this);
        }

        @Override
        public void onChange(boolean selfChange) {
            mIsTorchOn = Settings.System.getBoolean(mContext.getContentResolver(),
                    Settings.System.TORCH_STATE, false);
            updateState();
        }
    }
}
