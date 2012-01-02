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

import com.android.systemui.R;

/**
 * TODO: Listen for changes to the setting.
 */
public class AutoRotateToggle extends Toggle {

    private boolean mAutoRotation;

    private ContentObserver mAccelerometerRotationObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            updateToggleState();
        }
    };

    public AutoRotateToggle(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AutoRotateToggle(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs);

        mAutoRotation = getAutoRotation();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mToggle.setChecked(mAutoRotation);
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
    protected void updateInternalState() {
        mToggle.setChecked(Settings.System.getInt(
                mContext.getContentResolver(),
                Settings.System.ACCELEROMETER_ROTATION, 0) != 0);
    }

    @Override
    protected void onCheckChanged(boolean isChecked) {
        if (isChecked != mAutoRotation) {
            setAutoRotation(isChecked);
        }
    }
}
