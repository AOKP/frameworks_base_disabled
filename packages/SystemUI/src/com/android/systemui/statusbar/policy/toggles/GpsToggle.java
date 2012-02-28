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

import java.util.Observable;
import java.util.Observer;

import com.android.systemui.R;

import android.content.ContentQueryMap;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.location.LocationManager;
import android.provider.Settings;
import android.view.View;
import android.content.Intent;


public class GpsToggle extends Toggle {

    public GpsToggle(Context context) {
        super(context);

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
                    updateState();
                }
            };
            mContentQueryMap.addObserver(mSettingsObserver);
        }
        setLabel(R.string.toggle_gps);
        if (mToggle.isChecked())
        	setIcon(R.drawable.toggle_gps);
        else
        	setIcon(R.drawable.toggle_gps_off);
        updateState();
    }

    ContentQueryMap mContentQueryMap;
    Observer mSettingsObserver;

    @Override
    protected void onCheckChanged(boolean isChecked) {
        Settings.Secure.setLocationProviderEnabled(mContext.getContentResolver(),
                LocationManager.GPS_PROVIDER, isChecked ? true : false);
        if (isChecked)
        	setIcon(R.drawable.toggle_gps);
        else
        	setIcon(R.drawable.toggle_gps_off);
    }

    @Override
    protected void updateInternalToggleState() {
        ContentResolver res = mContext.getContentResolver();
        boolean gpsEnabled = Settings.Secure.isLocationProviderEnabled(
                res, LocationManager.GPS_PROVIDER);
        mToggle.setChecked(gpsEnabled);
        if (mToggle.isChecked())
        	setIcon(R.drawable.toggle_gps);
        else
        	setIcon(R.drawable.toggle_gps_off);
    }
    
    @Override
    protected boolean onLongPress() {
    	Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
    	return true;
    }

}
