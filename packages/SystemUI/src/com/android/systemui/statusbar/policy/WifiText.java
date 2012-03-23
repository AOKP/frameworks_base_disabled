
package com.android.systemui.statusbar.policy;



import android.R.integer;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.CharacterStyle;
import android.text.style.RelativeSizeSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class WifiText extends TextView {

    private static final String TAG = "WiFiText";
    private int mRssi;
    private boolean mAttached;
    private boolean mInAirplaneMode;
    private static final int STYLE_HIDE = 0;
    private static final int STYLE_SHOW = 1;
    private int style;
    private Handler mHandler;
    private Context mContext;
    private WifiManager mWifiManager;
    protected int mSignalColor = com.android.internal.R.color.holo_blue_light;
    
    /** pulled the below values directly from the WifiManager.Java.  I don't like the idea of 
     *  this, as it could change and we may not know about it.
     *  I've also noticed that Rssi is sometimes > -55 which is a little odd.
     */
    /** Anything worse than or equal to this will show 0 bars. */
    private static final int MIN_RSSI = -100;

    /** Anything better than or equal to this will show the max bars. */
    private static final int MAX_RSSI = -55;

    
    
    public WifiText(Context context) {
        this(context, null);
    }

    public WifiText(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WifiText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
    }
    
    public BroadcastReceiver rssiReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {      	
        	mRssi = intent.getIntExtra(WifiManager.EXTRA_NEW_RSSI,MIN_RSSI); 
        	updateSignalText();
        }
    };


    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (!mAttached) {
            mAttached = true;
            mHandler = new Handler();
            SettingsObserver settingsObserver = new SettingsObserver(mHandler);
            settingsObserver.observe();
            mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
            mContext.registerReceiver(rssiReceiver, new IntentFilter(WifiManager.RSSI_CHANGED_ACTION));
            updateSettings();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mAttached) {
            mAttached = false;
           mContext.unregisterReceiver(rssiReceiver);
        }
    }

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.STATUSBAR_WIFI_SIGNAL_TEXT), false,
                    this);
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.STATUSBAR_WIFI_SIGNAL_TEXT_COLOR), false,
                    this);
        }

        @Override
        public void onChange(boolean selfChange) {
            updateSettings();
        }
    }

    private void updateSettings() {
        ContentResolver resolver = getContext().getContentResolver();
        mSignalColor = Settings.System.getInt(resolver,
                Settings.System.STATUSBAR_WIFI_SIGNAL_TEXT_COLOR,
                0xFF33B5E5);
        if (mSignalColor == Integer.MIN_VALUE) {
            // flag to reset the color
            mSignalColor = 0xFF33B5E5;
        }
        setTextColor(mSignalColor);
        updateSignalText();
    }

    private void updateSignalText() { 	
        style = Settings.System.getInt(getContext().getContentResolver(),
                Settings.System.STATUSBAR_WIFI_SIGNAL_TEXT, STYLE_HIDE);

        if (style == STYLE_SHOW) {
        	// Rssi signals are from -100 to -55.  need to normalize this
        	float max = Math.abs(MAX_RSSI);
        	float min = Math.abs(MIN_RSSI);
        	float signal = 0f;
        	signal = min - Math.abs(mRssi);
        	signal = ((signal / (min - max)) * 100f);
        	mRssi = (signal > 100f ? 100 : Math.round(signal));
            String result = Integer.toString(mRssi);

            setText(result + "% ");
        } 
    }
}
