/*
 * Copyright (C) 2006 The Android Open Source Project
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

import android.app.ActivityManagerNative;
import android.app.StatusBarManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.ContentObserver;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.AlarmClock;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Events;
import android.content.ActivityNotFoundException;
import android.provider.Settings;
import android.os.Handler;
import android.os.RemoteException;
import android.speech.RecognizerIntent;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.format.DateFormat;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;
import java.net.URISyntaxException;

import com.android.internal.R;

/**
 * This widget display an analogic clock with two hands for hours and
 * minutes.
 */
public class ClockStock extends TextView implements OnClickListener, OnLongClickListener {	
    private boolean mAttached;
    private Calendar mCalendar;
    private String mClockFormatString;
    private SimpleDateFormat mClockFormat;
    private Intent intent;

    final static String ACTION_EVENT = "**event**";
    final static String ACTION_ALARM = "**alarm**";
    final static String ACTION_TODAY = "**today**";
    final static String ACTION_VOICEASSIST = "**assist**";
    final static String ACTION_NOTHING = "**nothing**";

    private String mShortClick;
    private String mLongClick;

    private static final int AM_PM_STYLE_NORMAL  = 0;
    private static final int AM_PM_STYLE_SMALL   = 1;
    private static final int AM_PM_STYLE_GONE    = 2;

    private static final int AM_PM_STYLE = AM_PM_STYLE_GONE;

    public ClockStock(Context context) {
        this(context, null);
    }

    public ClockStock(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ClockStock(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
         if(isClickable()){
            setOnClickListener(this);
            setOnLongClickListener(this);

            SettingsObserver settingsObserver = new SettingsObserver(new Handler());
            settingsObserver.observe();
            updateSettings();
            if (mShortClick == null || mShortClick == "") {
                mShortClick = "**nothing**";
            }
            if (mLongClick == null || mLongClick == "") {
                mLongClick = "**nothing**";
            }
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (!mAttached) {
            mAttached = true;
            IntentFilter filter = new IntentFilter();

            filter.addAction(Intent.ACTION_TIME_TICK);
            filter.addAction(Intent.ACTION_TIME_CHANGED);
            filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
            filter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);

            getContext().registerReceiver(mIntentReceiver, filter, null, getHandler());
        }

        // NOTE: It's safe to do these after registering the receiver since the receiver always runs
        // in the main thread, therefore the receiver can't run before this method returns.

        // The time zone may have changed while the receiver wasn't registered, so update the Time
        mCalendar = Calendar.getInstance(TimeZone.getDefault());

        // Make sure we update to the current time
        updateClock();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mAttached) {
            getContext().unregisterReceiver(mIntentReceiver);
            mAttached = false;
        }
    }

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_TIMEZONE_CHANGED)) {
                String tz = intent.getStringExtra("time-zone");
                mCalendar = Calendar.getInstance(TimeZone.getTimeZone(tz));
                if (mClockFormat != null) {
                    mClockFormat.setTimeZone(mCalendar.getTimeZone());
                }
            }
            updateClock();
        }
    };

    final void updateClock() {
        mCalendar.setTimeInMillis(System.currentTimeMillis());
        setText(getSmallTime());
    }

    private final CharSequence getSmallTime() {
        Context context = getContext();
        boolean b24 = DateFormat.is24HourFormat(context);
        int res;

        if (b24) {
            res = R.string.twenty_four_hour_time_format;
        } else {
            res = R.string.twelve_hour_time_format;
        }

        final char MAGIC1 = '\uEF00';
        final char MAGIC2 = '\uEF01';

        SimpleDateFormat sdf;
        String format = context.getString(res);
        if (!format.equals(mClockFormatString)) {
            /*
             * Search for an unquoted "a" in the format string, so we can
             * add dummy characters around it to let us find it again after
             * formatting and change its size.
             */
            if (AM_PM_STYLE != AM_PM_STYLE_NORMAL) {
                int a = -1;
                boolean quoted = false;
                for (int i = 0; i < format.length(); i++) {
                    char c = format.charAt(i);

                    if (c == '\'') {
                        quoted = !quoted;
                    }
                    if (!quoted && c == 'a') {
                        a = i;
                        break;
                    }
                }

                if (a >= 0) {
                    // Move a back so any whitespace before AM/PM is also in the alternate size.
                    final int b = a;
                    while (a > 0 && Character.isWhitespace(format.charAt(a-1))) {
                        a--;
                    }
                    format = format.substring(0, a) + MAGIC1 + format.substring(a, b)
                        + "a" + MAGIC2 + format.substring(b + 1);
                }
            }

            mClockFormat = sdf = new SimpleDateFormat(format);
            mClockFormatString = format;
        } else {
            sdf = mClockFormat;
        }
        String result = sdf.format(mCalendar.getTime());

        if (AM_PM_STYLE != AM_PM_STYLE_NORMAL) {
            int magic1 = result.indexOf(MAGIC1);
            int magic2 = result.indexOf(MAGIC2);
            if (magic1 >= 0 && magic2 > magic1) {
                SpannableStringBuilder formatted = new SpannableStringBuilder(result);
                if (AM_PM_STYLE == AM_PM_STYLE_GONE) {
                    formatted.delete(magic1, magic2+1);
                } else {
                    if (AM_PM_STYLE == AM_PM_STYLE_SMALL) {
                        CharacterStyle style = new RelativeSizeSpan(0.7f);
                        formatted.setSpan(style, magic1, magic2,
                                          Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
                    }
                    formatted.delete(magic2, magic2 + 1);
                    formatted.delete(magic1, magic1 + 1);
                }
                return formatted;
            }
        }
        return result;
    }

    @Override
    public void onClick(View v) {

        StatusBarManager statusBarManager = (StatusBarManager) getContext().getSystemService(Context.STATUS_BAR_SERVICE);
        if (mShortClick.equals(ACTION_NOTHING)) {
            return;
        } else {
            try {
                ActivityManagerNative.getDefault().dismissKeyguardOnNextActivity();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            if (mShortClick.equals(ACTION_TODAY)) {
                 // A date-time specified in milliseconds since the epoch.
                 long startMillis = System.currentTimeMillis();
                 Uri.Builder builder = CalendarContract.CONTENT_URI.buildUpon();
                 builder.appendPath("time");
                 ContentUris.appendId(builder, startMillis);
                 intent = new Intent(Intent.ACTION_VIEW)
                      .setData(builder.build());
            } else if (mShortClick.equals(ACTION_EVENT)) {
                 intent = new Intent(Intent.ACTION_INSERT)
                      .setData(Events.CONTENT_URI);
            } else if (mShortClick.equals(ACTION_VOICEASSIST)) {
                 intent = new Intent(RecognizerIntent.ACTION_WEB_SEARCH);
            } else if (mShortClick.equals(ACTION_ALARM)) {
                 intent = new Intent(AlarmClock.ACTION_SET_ALARM);
            } else {
                try {
                    intent = Intent.parseUri(mShortClick, 0);
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            }
            try {
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(intent);
            } catch (ActivityNotFoundException e){
                e.printStackTrace();
            }
            statusBarManager.collapse();
        }
    }

    @Override
    public boolean onLongClick(View v) {

        StatusBarManager statusBarManager = (StatusBarManager) getContext().getSystemService(Context.STATUS_BAR_SERVICE);
        if (mLongClick.equals(ACTION_NOTHING)) {
            return true;
        } else {
            try {
                ActivityManagerNative.getDefault().dismissKeyguardOnNextActivity();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            if (mLongClick.equals(ACTION_TODAY)) {
                 // A date-time specified in milliseconds since the epoch.
                 long startMillis = System.currentTimeMillis();
                 Uri.Builder builder = CalendarContract.CONTENT_URI.buildUpon();
                 builder.appendPath("time");
                 ContentUris.appendId(builder, startMillis);
                 intent = new Intent(Intent.ACTION_VIEW)
                      .setData(builder.build());
            } else if (mLongClick.equals(ACTION_EVENT)) {
                 intent = new Intent(Intent.ACTION_INSERT)
                      .setData(Events.CONTENT_URI);
            } else if (mLongClick.equals(ACTION_VOICEASSIST)) {
                 intent = new Intent(RecognizerIntent.ACTION_WEB_SEARCH);
            } else if (mLongClick.equals(ACTION_ALARM)) {
                 intent = new Intent(AlarmClock.ACTION_SET_ALARM);
            } else {
                try {
                    intent = Intent.parseUri(mLongClick, 0);
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            }
            try {
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(intent);
            } catch (ActivityNotFoundException e){
                e.printStackTrace();
            }
            statusBarManager.collapse();
        }
        return true;
    }

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.NOTIFICATION_CLOCK_SHORTCLICK), false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.NOTIFICATION_CLOCK_LONGCLICK), false, this);
        }

         @Override
        public void onChange(boolean selfChange) {
            updateSettings();
        }
    }
    protected void updateSettings() {
        ContentResolver cr = mContext.getContentResolver();

        mShortClick = Settings.System.getString(cr,
                Settings.System.NOTIFICATION_CLOCK_SHORTCLICK);

        mLongClick = Settings.System.getString(cr,
                Settings.System.NOTIFICATION_CLOCK_LONGCLICK);
    }
}

