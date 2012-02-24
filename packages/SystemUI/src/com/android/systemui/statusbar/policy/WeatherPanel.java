package com.android.systemui.statusbar.policy;

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
import android.widget.ImageView;
import com.android.systemui.R;

public class WeatherPanel extends View {

    private boolean mAttached;

    public static final String EXTRA_CITY = "city";
    public static final String EXTRA_ZIP = "zip";
    public static final String EXTRA_CONDITION = "condition";
    public static final String EXTRA_FORECAST_DATE = "forecase_date";
    public static final String EXTRA_TEMP_F = "temp_f";
    public static final String EXTRA_TEMP_C = "temp_c";
    public static final String EXTRA_HUMIDITY = "humidity";
    public static final String EXTRA_WIND = "wind";
    public static final String EXTRA_LOW = "todays_low";
    public static final String EXTRA_HIGH = "todays_high";
    
    private TextView mHighTemp;
    private TextView mLowTemp;
    private TextView mCurrentTemp;
    private TextView mCity;
    private TextView mZipCode;
    private TextView mHumidity;
    private TextView mWinds;
    private ImageView mConditionImage;

    BroadcastReceiver weatherReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        	if (mCurrentTemp !=null)
        		mCurrentTemp.setText("Current:" + intent.getCharSequenceExtra("temp"));
        	if (mHighTemp !=null)
        		mHighTemp.setText("High:" + intent.getCharSequenceExtra(EXTRA_HIGH));
        	if (mLowTemp !=null)
        		mLowTemp.setText("Low:" + intent.getCharSequenceExtra(EXTRA_LOW));
        	if (mCity !=null)
        		mCity.setText(intent.getCharSequenceExtra(EXTRA_CITY));
        	if (mZipCode !=null)
        		mZipCode.setText("ZipCode:" + intent.getCharSequenceExtra(EXTRA_ZIP));
        	if (mHumidity !=null)
        		mHumidity.setText(intent.getCharSequenceExtra(EXTRA_HUMIDITY));
        	if (mWinds !=null)
        		mWinds.setText(intent.getCharSequenceExtra(EXTRA_WIND));
        	if (mConditionImage != null)
        		mConditionImage.setImageDrawable(R.drawable.weather_sunny);
        	    // I need to change this to a look up - setting to sunny for testing      	
        }
    };

    public WeatherPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        mHighTemp = clearTextViewData(this.findViewById(R.id.high_temp));
        mLowTemp = clearTextViewData(this.findViewById(R.id.low_temp));
        mCurrentTemp = clearTextViewData(this.findViewById(R.id.current_temp));
        mCity = clearTextViewData(this.findViewById(R.id.city));
        mZipCode = clearTextViewData(this.findViewById(R.id.zipcode));
        mHumidity = clearTextViewData(this.findViewById(R.id.humidity));
        mWinds = clearTextViewData(this.findViewById(R.id.winds));
        mConditionImage = this.findViewById(R.id.condition_image);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!mAttached) {
            mAttached = true;
            IntentFilter filter = new IntentFilter("com.aokp.romcontrol.INTENT_WEATHER_UPDATE");
            getContext().registerReceiver(weatherReceiver, filter, null, getHandler());

            SettingsObserver so = new SettingsObserver(getHandler());
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

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
            observe();
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.USE_WEATHER), false,
                    this);
            updateSettings();
        }

        @Override
        public void onChange(boolean selfChange) {
            updateSettings();
        }
    }

    protected void updateSettings() {
        ContentResolver resolver = mContext.getContentResolver();

        boolean useWeather = Settings.System.getInt(resolver, Settings.System.USE_WEATHER, 0) == 1;
        if (mCurrentTemp !=null)
    		mCurrentTemp.setVisibility(useWeather ? View.VISIBLE : View.GONE);
    	if (mHighTemp !=null)
    		mHighTemp.setVisibility(useWeather ? View.VISIBLE : View.GONE);
    	if (mLowTemp !=null)
    		mLowTemp.setVisibility(useWeather ? View.VISIBLE : View.GONE);
    	if (mCity !=null)
    		mCity.setVisibility(useWeather ? View.VISIBLE : View.GONE);
    	if (mZipCode !=null)
    		mZipCode.setVisibility(useWeather ? View.VISIBLE : View.GONE);
    	if (mHumidity !=null)
    		mHumidity.setVisibility(useWeather ? View.VISIBLE : View.GONE);
    	if (mWinds !=null)
    		mWinds.setVisibility(useWeather ? View.VISIBLE : View.GONE);
    	if (mConditionImage != null)
    		mConditionImage.setVisibility(useWeather ? View.VISIBLE : View.GONE);
    }

    protected TextView clearTextViewData(TextView tv){
      if (tv==null){
    	  return null;
      } else {		  
    	  tv.setText("");
    	  return tv;
      }
    }
}
