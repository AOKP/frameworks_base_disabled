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

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.TypedArray;
import android.provider.Settings;
import android.os.RemoteException;
import android.util.AttributeSet;
import android.util.Slog;
import android.view.View;
import android.widget.ImageView;

import com.android.systemui.R;

public class CompatModeButton extends ImageView {
    private static final boolean DEBUG = false;
    private static final String TAG = "StatusBar.CompatModeButton";
    
    boolean mHideExtras = false;

    private ActivityManager mAM;

    public CompatModeButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CompatModeButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs);
        ContentResolver resolver = context.getContentResolver();

        setClickable(true);

        mAM = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        mHideExtras = Settings.System.getBoolean(resolver, Settings.System.HIDE_EXTRAS_SYSTEM_BAR, false);

        refresh();
    }

    public void refresh() {
        int mode = mAM.getFrontActivityScreenCompatMode();
        if (mode == ActivityManager.COMPAT_MODE_UNKNOWN) {
            // If in an unknown state, don't change.
            return;
        }
        final boolean vis = (mode != ActivityManager.COMPAT_MODE_NEVER
                          && mode != ActivityManager.COMPAT_MODE_ALWAYS && !mHideExtras);
        if (DEBUG) Slog.d(TAG, "compat mode is " + mode + "; icon will " + (vis ? "show" : "hide"));
        setVisibility(vis ? View.VISIBLE : View.GONE);
    }
}
