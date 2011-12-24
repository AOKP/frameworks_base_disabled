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

import java.util.Observable;
import java.util.Observer;

import android.content.ContentQueryMap;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.location.LocationManager;
import android.provider.Settings;
import android.widget.CompoundButton;

public class GpsToggle implements
        CompoundButton.OnCheckedChangeListener {
    private static final String TAG = "StatusBar.BluetoothController";

    private Context mContext;

    ContentQueryMap mContentQueryMap;
    Observer mSettingsObserver;

    CompoundButton mGps;

    boolean mSystemChanged = false;

    public GpsToggle(Context context, CompoundButton b) {
        mContext = context;
        mGps = b;

        ContentResolver res = mContext.getContentResolver();
        boolean gpsEnabled = Settings.Secure.isLocationProviderEnabled(
                res, LocationManager.GPS_PROVIDER);
        mGps.setChecked(gpsEnabled);

        mGps.setOnCheckedChangeListener(this);

        Cursor settingsCursor = mContext.getContentResolver().query(Settings.Secure.CONTENT_URI,
                null,
                "(" + Settings.System.NAME + "=?)",
                new String[] {
                    Settings.Secure.LOCATION_PROVIDERS_ALLOWED
                },
                null);
        mContentQueryMap = new ContentQueryMap(settingsCursor, Settings.System.NAME, true, null);

        if (mSettingsObserver == null) {
            mSettingsObserver = new Observer() {
                public void update(Observable o, Object arg) {
                    refreshStatus();
                }
            };
            mContentQueryMap.addObserver(mSettingsObserver);
        }

        refreshStatus();
    }

    public void refreshStatus() {
        mSystemChanged = true;
        ContentResolver res = mContext.getContentResolver();
        boolean gpsEnabled = Settings.Secure.isLocationProviderEnabled(
                res, LocationManager.GPS_PROVIDER);
        mGps.setChecked(gpsEnabled);
        mSystemChanged = false;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (mSystemChanged)
            return;
        Settings.Secure.setLocationProviderEnabled(mContext.getContentResolver(),
                LocationManager.GPS_PROVIDER, isChecked ? true : false);
    }
}
