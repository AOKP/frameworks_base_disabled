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

package com.android.internal.widget;

import com.android.internal.R;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.graphics.Typeface;
import android.os.Handler;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.text.DateFormatSymbols;
import java.util.Calendar;

/**
 * Displays the time
 */
public class DigitalClockAlt extends RelativeLayout {

    private static final String SYSTEM = "/system/fonts/";
    private static final String SYSTEM_FONT_TIME_BOLD = SYSTEM + "AndroidClockMono-Bold.ttf";
    private static final String SYSTEM_FONT_TIME_LIGHT = SYSTEM + "AndroidClockMono-Light.ttf";
    private static final String SYSTEM_FONT_TIME_THIN = SYSTEM + "AndroidClockMono-Thin.ttf";
    private final static String M12 = "h:mm";
    private final static String M24 = "kk:mm";
    private static final int COLOR_WHITE = 0xFFFFFFFF;

    private Calendar mCalendar;
    private AmPm mAmPm;
    private String mFormat;
    private TextView mTimeDisplayHours;
    private TextView mTimeDisplayMinutes;
    private TextView mSep;
    private ContentObserver mFormatChangeObserver;
    private int mAttached = 0; // for debugging - tells us whether attach/detach is unbalanced

    /* called by system on minute ticks */
    private final Handler mHandler = new Handler();
    private BroadcastReceiver mIntentReceiver;

    private static final Typeface sBoldFont;
    private static final Typeface sLightFont;
    private static final Typeface sThinFont;

    static {
        sBoldFont = Typeface.createFromFile(SYSTEM_FONT_TIME_BOLD);
        sLightFont = Typeface.createFromFile(SYSTEM_FONT_TIME_LIGHT);
        sThinFont = Typeface.createFromFile(SYSTEM_FONT_TIME_THIN);
    }

    private static class TimeChangedReceiver extends BroadcastReceiver {
        private WeakReference<DigitalClockAlt> mClock;
        private Context mContext;

        public TimeChangedReceiver(DigitalClockAlt clock) {
            mClock = new WeakReference<DigitalClockAlt>(clock);
            mContext = clock.getContext();
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            // Post a runnable to avoid blocking the broadcast.
            final boolean timezoneChanged =
                    intent.getAction().equals(Intent.ACTION_TIMEZONE_CHANGED);
            final DigitalClockAlt clock = mClock.get();
            if (clock != null) {
                clock.mHandler.post(new Runnable() {
                    public void run() {
                        if (timezoneChanged) {
                            clock.mCalendar = Calendar.getInstance();
                        }
                        clock.updateTime();
                    }
                });
            } else {
                try {
                    mContext.unregisterReceiver(this);
                } catch (RuntimeException e) {
                    // Shouldn't happen
                }
            }
        }
    };

    static class AmPm {
        private TextView mAmPmTextView;
        private String mAmString, mPmString;

        AmPm(View parent, Typeface tf) {
            mAmPmTextView = (TextView) parent.findViewById(R.id.ampm);
            if (mAmPmTextView != null && tf != null) {
                mAmPmTextView.setTypeface(sLightFont);
            }

            String[] ampm = new DateFormatSymbols().getAmPmStrings();
            mAmString = ampm[0];
            mPmString = ampm[1];
        }

        void setAmPmColor(int color) {
            mAmPmTextView.setTextColor(color);
        }

        void setShowAmPm(boolean show) {
            if (mAmPmTextView != null) {
                mAmPmTextView.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        }

        void setIsMorning(boolean isMorning) {
            if (mAmPmTextView != null) {
                mAmPmTextView.setText(isMorning ? mAmString : mPmString);
            }
        }
    }

    private static class FormatChangeObserver extends ContentObserver {
        private WeakReference<DigitalClockAlt> mClock;
        private Context mContext;
        public FormatChangeObserver(DigitalClockAlt clock) {
            super(new Handler());
            mClock = new WeakReference<DigitalClockAlt>(clock);
            mContext = clock.getContext();
        }
        @Override
        public void onChange(boolean selfChange) {
            DigitalClockAlt digitalClock = mClock.get();
            if (digitalClock != null) {
                digitalClock.setDateFormat();
                digitalClock.updateTime();
            } else {
                try {
                    mContext.getContentResolver().unregisterContentObserver(this);
                } catch (RuntimeException e) {
                    // Shouldn't happen
                }
            }
        }
    }

    public DigitalClockAlt(Context context) {
        this(context, null);
    }

    public DigitalClockAlt(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        /* The time display consists of two tones. That's why we have two overlapping text views. */
        mTimeDisplayHours = (TextView) findViewById(R.id.timeDisplayHours);
        mTimeDisplayHours.setTypeface(sBoldFont);

        mSep = (TextView) findViewById(R.id.sep);
        mSep.setText(":");
        mSep.setTypeface(sThinFont);

        mTimeDisplayMinutes = (TextView) findViewById(R.id.timeDisplayMinutes);
        mTimeDisplayMinutes.setTypeface(sThinFont);
        mAmPm = new AmPm(this, null);
        mCalendar = Calendar.getInstance();

        setDateFormat();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        mAttached++;

        /* monitor time ticks, time changed, timezone */
        if (mIntentReceiver == null) {
            mIntentReceiver = new TimeChangedReceiver(this);
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_TIME_TICK);
            filter.addAction(Intent.ACTION_TIME_CHANGED);
            filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
            mContext.registerReceiver(mIntentReceiver, filter);
        }

        /* monitor 12/24-hour display preference */
        if (mFormatChangeObserver == null) {
            mFormatChangeObserver = new FormatChangeObserver(this);
            mContext.getContentResolver().registerContentObserver(
                    Settings.System.CONTENT_URI, true, mFormatChangeObserver);
        }

        updateTime();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        mAttached--;

        if (mIntentReceiver != null) {
            mContext.unregisterReceiver(mIntentReceiver);
        }
        if (mFormatChangeObserver != null) {
            mContext.getContentResolver().unregisterContentObserver(
                    mFormatChangeObserver);
        }

        mFormatChangeObserver = null;
        mIntentReceiver = null;
    }

    void updateTime(Calendar c) {
        mCalendar = c;
        updateTime();
    }

    public void updateTime() {
        mCalendar.setTimeInMillis(System.currentTimeMillis());

        CharSequence newHour = DateFormat.format(mFormat.equals(M12)
                ? "h" : "kk", mCalendar);
        if (newHour.equals("0")) newHour = "12";
        mTimeDisplayHours.setText(newHour);

        CharSequence newMin = DateFormat.format("mm", mCalendar);
        mTimeDisplayMinutes.setText(newMin);

        mAmPm.setIsMorning(mCalendar.get(Calendar.AM_PM) == 0);

        ContentResolver resolver = mContext.getContentResolver();
        // our custom lockscreen colors need to be applied here
        int mLockscreenColor = Settings.System.getInt(resolver,
                Settings.System.LOCKSCREEN_CUSTOM_TEXT_COLOR, COLOR_WHITE);
        mTimeDisplayHours.setTextColor(mLockscreenColor);
        mTimeDisplayMinutes.setTextColor(mLockscreenColor);
        mSep.setTextColor(mLockscreenColor);
        mAmPm.setAmPmColor(mLockscreenColor);
    }

    private void setDateFormat() {
        mFormat = android.text.format.DateFormat.is24HourFormat(getContext())
            ? M24 : M12;
        mAmPm.setShowAmPm(mFormat.equals(M12));
    }
}
