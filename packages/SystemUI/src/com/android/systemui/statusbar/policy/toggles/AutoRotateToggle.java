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

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.IWindowManager;
import android.view.View;

import com.android.systemui.R;

/**
 * TODO: Listen for changes to the setting.
 */
public class AutoRotateToggle extends Toggle {

    private boolean mAutoRotation;

    private ContentObserver mAccelerometerRotationObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            updateState();
        }
    };

    public AutoRotateToggle(Context context) {
        super(context);
        mAutoRotation = getAutoRotation();
        updateState();
        setLabel(R.string.toggle_rotate);
        if (mToggle.isChecked())
        	setIcon(R.drawable.toggle_rotate);
        else
        	setIcon(R.drawable.toggle_rotate_off);
    }

    private boolean getAutoRotation() {
        ContentResolver cr = mContext.getContentResolver();
        return 0 != Settings.System.getInt(cr, Settings.System.ACCELEROMETER_ROTATION, 0);
    }

    private void setAutoRotation(final boolean autorotate) {
        mAutoRotation = autorotate;
        AsyncTask.execute(new Runnable() {
            public void run() {
                try {
                    IWindowManager wm = IWindowManager.Stub.asInterface(
                            ServiceManager.getService(Context.WINDOW_SERVICE));
                    if (autorotate) {
                        wm.thawRotation();
                    } else {
                        wm.freezeRotation(-1);
                    }
                } catch (RemoteException exc) {
                    Log.w(TAG, "Unable to save auto-rotate setting");
                }
            }
        });
    }

    @Override
    protected void onCheckChanged(boolean isChecked) {
        if (isChecked != mAutoRotation) {
            setAutoRotation(isChecked);
        }
        if (isChecked)
        	setIcon(R.drawable.toggle_rotate);
        else
        	setIcon(R.drawable.toggle_rotate_off);
    }

    @Override
    protected void updateInternalToggleState() {
        mToggle.setChecked(Settings.System.getInt(
                mContext.getContentResolver(),
                Settings.System.ACCELEROMETER_ROTATION, 0) != 0);
        if (mToggle.isChecked())
        	setIcon(R.drawable.toggle_rotate);
        else
        	setIcon(R.drawable.toggle_rotate_off);
    }
    
    @Override
    protected boolean onLongPress() {
    	Intent intent = new Intent(android.provider.Settings.ACTION_DISPLAY_SETTINGS);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
    	return true;
    }
}
