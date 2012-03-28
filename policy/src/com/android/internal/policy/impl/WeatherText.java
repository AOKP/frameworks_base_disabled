
package com.android.internal.policy.impl;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class WeatherText extends TextView {

    private boolean mAttached;

    public static final String EXTRA_CITY = "city";
    public static final String EXTRA_CONDITION = "condition";
    public static final String EXTRA_FORECAST_DATE = "forecast_date";
    public static final String EXTRA_TEMP = "temp";
    public static final String EXTRA_HUMIDITY = "humidity";
    public static final String EXTRA_WIND = "wind";
    public static final String EXTRA_LOW = "todays_low";
    public static final String EXTRA_HIGH = "todays_high";
    
    private static boolean showLocation = false;
    

    BroadcastReceiver weatherReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String wText = (showLocation) ? (intent.getCharSequenceExtra(EXTRA_CITY) + ", " + intent.getCharSequenceExtra(EXTRA_TEMP) + ", "
                    + intent.getCharSequenceExtra(EXTRA_CONDITION)) : (intent.getCharSequenceExtra(EXTRA_TEMP) + ", "
                            + intent.getCharSequenceExtra(EXTRA_CONDITION));
            setText(wText);
        }
    };

    public WeatherText(Context context, AttributeSet attrs) {
        super(context, attrs);
        setText("");
        ContentResolver resolver = mContext.getContentResolver();
        showLocation = Settings.System.getInt(resolver, Settings.System.WEATHER_SHOW_LOCATION, 0) == 1;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!mAttached) {
            mAttached = true;
            IntentFilter filter = new IntentFilter("com.aokp.romcontrol.INTENT_WEATHER_UPDATE");
            getContext().registerReceiver(weatherReceiver, filter, null, getHandler());
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mAttached) {
            getContext().unregisterReceiver(weatherReceiver);
            mAttached = false;
        }
    }
}
