
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
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.internal.R;

public class WeatherPanel extends FrameLayout {

    private static final String TAG = "WeatherPanel";

    private boolean mAttached;

    public static final String EXTRA_CITY = "city";
    public static final String EXTRA_CONDITION = "condition";
    public static final String EXTRA_CONDITION_CODE = "condition_code";
    public static final String EXTRA_FORECAST_DATE = "forecast_date";
    public static final String EXTRA_TEMP = "temp";
    public static final String EXTRA_HUMIDITY = "humidity";
    public static final String EXTRA_WIND = "wind";
    public static final String EXTRA_LOW = "todays_low";
    public static final String EXTRA_HIGH = "todays_high";

    private TextView mHighTemp;
    private TextView mSlash;
    private TextView mLowTemp;
    private TextView mCurrentTemp;
    private TextView mCity;
    private TextView mHumidity;
    private TextView mWinds;
    private TextView mCondition;
    private ImageView mConditionImage;
    private Context mContext;
    private String mCondition_code = "";

    BroadcastReceiver weatherReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateWeather(intent);
        }
    };

    public void updateWeather(Intent intent) {
        mCondition_code = (String) intent.getCharSequenceExtra(EXTRA_CONDITION_CODE);
        if (mCurrentTemp != null)
            mCurrentTemp.setText(intent.getCharSequenceExtra(EXTRA_TEMP));
        if (mHighTemp != null)
            mHighTemp.setText(intent.getCharSequenceExtra(EXTRA_HIGH));
        if (mLowTemp != null)
            mLowTemp.setText(intent.getCharSequenceExtra(EXTRA_LOW));
        if (mCity != null)
            mCity.setText(intent.getCharSequenceExtra(EXTRA_CITY));
        if (mHumidity != null)
            mHumidity.setText(intent.getCharSequenceExtra(EXTRA_HUMIDITY));
        if (mWinds != null)
            mWinds.setText(intent.getCharSequenceExtra(EXTRA_WIND));
        if (mCondition != null)
            mCondition.setText(intent.getCharSequenceExtra(EXTRA_CONDITION));
        if (mConditionImage != null) {
            String condition_filename = "weather_" + mCondition_code;
            int resID = getResources().getIdentifier(condition_filename, "drawable",
                    "android");
            Log.d("Weather", "Condition:" + mCondition_code + " ID:" + resID);
            if (resID != 0) {
                mConditionImage.setImageDrawable(getResources().getDrawable(resID));
            } else {
                mConditionImage.setImageDrawable(getResources().getDrawable(
                        com.android.internal.R.drawable.weather_na));
            }
        }
    }
    
    public void setTextColor(int color)
    {
        if (mCurrentTemp != null)
            mCurrentTemp.setTextColor(color);
        if (mHighTemp != null)
            mHighTemp.setTextColor(color);
        if (mLowTemp != null)
            mLowTemp.setTextColor(color);
        if (mCity != null)
            mCity.setTextColor(color);
        if (mHumidity != null)
            mHumidity.setTextColor(color);
        if (mWinds != null)
            mWinds.setTextColor(color);
        if (mCondition != null)
            mCondition.setTextColor(color);
        if (mSlash != null)
            mSlash.setTextColor(color);
    }

    private View.OnClickListener mPanelOnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            Intent weatherintent = new Intent("com.aokp.romcontrol.INTENT_WEATHER_REQUEST");

            if (v.getId() == com.android.internal.R.id.condition_image) {
                weatherintent.putExtra("com.aokp.romcontrol.INTENT_EXTRA_TYPE", "startapp");
                weatherintent.putExtra("com.aokp.romcontrol.INTENT_EXTRA_ISMANUAL", true);
            } else {
                weatherintent.putExtra("com.aokp.romcontrol.INTENT_EXTRA_TYPE", "updateweather");
                weatherintent.putExtra("com.aokp.romcontrol.INTENT_EXTRA_ISMANUAL", true);
            }

            v.getContext().sendBroadcast(weatherintent);

        }
    };

    public WeatherPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        //setOnClickListener(mPanelOnClickListener);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mHighTemp = (TextView) this.findViewById(com.android.internal.R.id.high_temp);
        mLowTemp = (TextView) this.findViewById(com.android.internal.R.id.low_temp);
        mCurrentTemp = (TextView) this.findViewById(com.android.internal.R.id.current_temp);
        mCity = (TextView) this.findViewById(com.android.internal.R.id.city);
        mHumidity = (TextView) this.findViewById(com.android.internal.R.id.humidity);
        mWinds = (TextView) this.findViewById(com.android.internal.R.id.winds);
        mCondition = (TextView) this.findViewById(com.android.internal.R.id.condition);
        mConditionImage = (ImageView) this.findViewById(com.android.internal.R.id.condition_image);
        mSlash = (TextView) this.findViewById(com.android.internal.R.id.weatherpanel_slash);

        //if (mConditionImage != null) {
        //    mConditionImage.setOnClickListener(mPanelOnClickListener);
        //}

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
