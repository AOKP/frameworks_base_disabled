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

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SyncStatusObserver;

import com.android.systemui.R;

public class SyncToggle extends Toggle {

    private SyncStatusObserver mSyncObserver = new SyncStatusObserver() {
        public void onStatusChanged(int which) {
            updateState();
        }
    };

    public SyncToggle(Context context) {
        super(context);
        updateState();
        ContentResolver.addStatusChangeListener(ContentResolver.SYNC_OBSERVER_TYPE_SETTINGS,
                mSyncObserver);
        setLabel(R.string.toggle_sync);
        if (mToggle.isChecked())
        	setIcon(R.drawable.toggle_sync);
        else
        	setIcon(R.drawable.toggle_sync_off);
    }

    @Override
    protected void onCheckChanged(boolean isChecked) {
        ContentResolver.setMasterSyncAutomatically(isChecked);
        if (mToggle.isChecked())
        	setIcon(R.drawable.toggle_sync);
        else
        	setIcon(R.drawable.toggle_sync_off);
    }

    @Override
    protected void updateInternalToggleState() {
        mToggle.setChecked(ContentResolver.getMasterSyncAutomatically());
        if (mToggle.isChecked())
        	setIcon(R.drawable.toggle_sync);
        else
        	setIcon(R.drawable.toggle_sync_off);
    }

    @Override
    protected boolean onLongPress() {
        Intent intent = new Intent(android.provider.Settings.ACTION_SYNC_SETTINGS);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
        return true;
    }
}
