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

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.CompoundButton;

public class ToggleController implements CompoundButton.OnCheckedChangeListener,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "StatusBar.DoNotDisturbController";

    SharedPreferences mPrefs;
    private Context mContext;
    private CompoundButton mCheckBox;

    private boolean mShowToggles;

    public ToggleController(Context context, CompoundButton checkbox) {
        mContext = context;

        mPrefs = Prefs.read(context);
        mPrefs.registerOnSharedPreferenceChangeListener(this);
        mShowToggles = mPrefs.getBoolean(Prefs.SHOW_TOGGLES, true);

        mCheckBox = checkbox;

        checkbox.setChecked(mShowToggles);
        checkbox.setOnCheckedChangeListener(this);

    }

    // The checkbox is ON for notifications coming in and OFF for Do not disturb, so we
    // don't have a double negative.
    public void onCheckedChanged(CompoundButton view, boolean checked) {
        // Slog.d(TAG, "onCheckedChanged checked=" + checked + " mDoNotDisturb=" + mDoNotDisturb);
        if (checked != mShowToggles) {
            SharedPreferences.Editor editor = Prefs.edit(mContext);
            editor.putBoolean(Prefs.SHOW_TOGGLES, checked);
            editor.apply();
        }
    }

    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        final boolean val = prefs.getBoolean(Prefs.SHOW_TOGGLES,
                true);
        if (val != mShowToggles) {
            mShowToggles = val;
            mCheckBox.setChecked(val);
        }
    }

    public void release() {
        mPrefs.unregisterOnSharedPreferenceChangeListener(this);
    }
}
