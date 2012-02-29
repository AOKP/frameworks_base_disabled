/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Updated 2012 LiquidSmoothROMs Project @jbirdvegas
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
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewParent;
import android.widget.TextView;

import com.android.systemui.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public final class DateView extends TextView {
    private static final String TAG = "DateView";
    private static final boolean DEBUG = false;

    private boolean mAttachedToWindow;
    private boolean mWindowVisible;
    private boolean mUpdating;

    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (Intent.ACTION_TIME_TICK.equals(action)
                    || Intent.ACTION_SCREEN_ON.equals(action)
                    || Intent.ACTION_TIME_CHANGED.equals(action)
                    || Intent.ACTION_TIMEZONE_CHANGED.equals(action)) {
                updateClock();
            }
        }
    };

    public DateView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mAttachedToWindow = true;
        setUpdates();
    }
    
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mAttachedToWindow = false;
        setUpdates();
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        mWindowVisible = visibility == VISIBLE;
        setUpdates();
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        setUpdates();
    }

    @Override
    protected int getSuggestedMinimumWidth() {
        // makes the large background bitmap not force us to full width
        return 0;
    }

    public final void updateClock() {
        final Context context = getContext();
        ContentResolver cr = context.getContentResolver();
        Calendar time_now = Calendar.getInstance();
        Date now = new Date();
        SimpleDateFormat simpleDate = new SimpleDateFormat("D");

        // get date in simple formats
        CharSequence dow_long = DateFormat.format("EEEE", now);
        CharSequence dow_short = DateFormat.format("EEE", now);
        CharSequence month_short = DateFormat.format("MMM", now);
        CharSequence today = DateFormat.format("d", now);
        CharSequence year = DateFormat.format("yyyy", now);
        CharSequence date = DateFormat.getLongDateFormat(context).format(now);

        // setup final formating
        String dateFormat_long = String.format("%s %s", dow_long, date);
        String dateFormat_short = String.format("%s %s", dow_short, date);
        String date_default = context.getString(R.string.status_bar_date_formatter, dow_long, date);
        String date_short = String.format("%s %s %s", dow_short, month_short, today);
        String d_o_y = simpleDate.format(time_now.getTime());
        String day_of_year = String.format("day %s of %s", d_o_y, year);

        // check if user has preference
        int style = Settings.System.getInt(cr, Settings.System.STATUSBAR_DATE_FORMAT, 0);

        // set the date in the correct format
        String debug_string = "Statusbar date format: %s";
        switch (style) {
            case 0:
                // default, February 14, 2012
                setText(date_default);
                if (DEBUG) Log.d(TAG, String.format(debug_string, date_default));
            break;
            case 1:
                // Tuesday February 14, 2012
                setText(dateFormat_long);
                if (DEBUG) Log.d(TAG, String.format(debug_string, dateFormat_long));
            break;
            case 2:
                // Tues February 14, 2012
                setText(dateFormat_short);
                if (DEBUG) Log.d(TAG, String.format(debug_string, dateFormat_short));
            break;
            case 3:
                // Tuesday
                setText(dow_long);
                if (DEBUG) Log.d(TAG, String.format(debug_string, dow_long));
            break;
            case 4:
                // day 45 of 2012
                setText(day_of_year);
                if (DEBUG) Log.d(TAG, String.format(debug_string, day_of_year));
            break;
            case 5:
                // Tues Feb 14
                setText(date_short);
                if (DEBUG) Log.d(TAG, String.format(debug_string, date_short));
            break;
        }
    }

    private boolean isVisible() {
        View v = this;
        while (true) {
            if (v.getVisibility() != VISIBLE) {
                return false;
            }
            final ViewParent parent = v.getParent();
            if (parent instanceof View) {
                v = (View)parent;
            } else {
                return true;
            }
        }
    }

    private void setUpdates() {
        boolean update = mAttachedToWindow && mWindowVisible && isVisible();
        if (update != mUpdating) {
            mUpdating = update;
            if (update) {
                // Register for Intent broadcasts for the clock and battery
                IntentFilter filter = new IntentFilter();
                filter.addAction(Intent.ACTION_TIME_TICK);
                filter.addAction(Intent.ACTION_TIME_CHANGED);
                filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
                mContext.registerReceiver(mIntentReceiver, filter, null, null);
                updateClock();
            } else {
                mContext.unregisterReceiver(mIntentReceiver);
            }
        }
    }
}
