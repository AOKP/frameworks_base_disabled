
package com.android.systemui.statusbar.policy;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.graphics.drawable.Animatable;
import android.os.BatteryManager;
import android.os.Handler;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

public class BatteryBar extends LinearLayout implements Animatable, Runnable {

    private static final String TAG = BatteryBar.class.getSimpleName();

    // Total animation duration
    private static final int ANIM_DURATION = 8000; // 5 seconds

    // Duration between frames of charging animation
    private static final int FRAME_DURATION = ANIM_DURATION / 100;

    private boolean mAttached = false;
    private boolean mShowBatteryBar = false;
    private int mBatteryLevel = 0;
    private int mChargingLevel = -1;
    private boolean mBatteryCharging = false;

    private Handler mHandler = new Handler();

    LinearLayout mBatteryBarLayout;
    View mBatteryBar;

    class SettingsObserver extends ContentObserver {

        public SettingsObserver(Handler handler) {
            super(handler);
        }

        void observer() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.STATUSBAR_BATTERY_BAR), false, this);
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.STATUSBAR_BATTERY_BAR_COLOR), false,
                    this);
        }

        @Override
        public void onChange(boolean selfChange) {
            updateSettings();
        }
    }

    public BatteryBar(Context context) {
        this(context, null);
    }

    public BatteryBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BatteryBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mBatteryBarLayout = new LinearLayout(mContext);
        mBatteryBarLayout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));
        mBatteryBar = new View(mContext);
        mBatteryBar.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));
        mBatteryBarLayout.addView(mBatteryBar);
        addView(mBatteryBarLayout);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (!mAttached) {
            mAttached = true;
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_BATTERY_CHANGED);
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            filter.addAction(Intent.ACTION_SCREEN_ON);
            getContext().registerReceiver(mIntentReceiver, filter, null, getHandler());

            SettingsObserver observer = new SettingsObserver(mHandler);
            observer.observer();
            updateSettings();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (mAttached) {
            mAttached = false;
            getContext().unregisterReceiver(mIntentReceiver);
        }
    }

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (!mShowBatteryBar) {
                stop();
                return;
            }

            if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                mBatteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
                mBatteryCharging = intent.getIntExtra(BatteryManager.EXTRA_STATUS, 0) == BatteryManager.BATTERY_STATUS_CHARGING;
                if (mBatteryCharging && mBatteryLevel < 100) {
                    start();
                } else {
                    stop();
                }
            } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                stop();
            } else if (Intent.ACTION_SCREEN_ON.equals(action)) {
                if (mBatteryCharging && mBatteryLevel < 100) {
                    start();
                }
            }
        }
    };

    private void updateSettings() {
        ContentResolver resolver = getContext().getContentResolver();
        mShowBatteryBar = (Settings.System.getInt(resolver,
                Settings.System.STATUSBAR_BATTERY_BAR, 0) == 1);

        int color = Settings.System.getInt(resolver,
                Settings.System.STATUSBAR_BATTERY_BAR_COLOR, 0xFF33B5E5);

        mBatteryBar.setBackgroundColor(color);

        if (mShowBatteryBar) {
            setVisibility(VISIBLE);
        } else {
            setVisibility(GONE);
        }

        if (mBatteryCharging && mBatteryLevel < 100) {
            start();
        } else {
            stop();
        }
    }

    @Override
    public void run() {
        mChargingLevel++;
        if (mChargingLevel > 100) {
            mChargingLevel = mBatteryLevel;
            mBatteryBar.forceLayout();

        }
        setProgress(mChargingLevel);
        if (mChargingLevel < 95)
            mHandler.postDelayed(this, FRAME_DURATION);
        else
            mHandler.postDelayed(this, 2000);
    }

    private void setProgress(int n) {

        int width = (int) ((getWidth() / 100) + 0.5d) * n;
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) mBatteryBar
                .getLayoutParams();
        params.width = width;

        mBatteryBarLayout.setLayoutParams(params);
    }

    @Override
    public void start() {
        if (!isRunning()) {
            mHandler.removeCallbacks(this);
            mChargingLevel = mBatteryLevel;
            mHandler.postDelayed(this, FRAME_DURATION);
        }
    }

    @Override
    public void stop() {
        if (isRunning()) {
            mHandler.removeCallbacks(this);
            mChargingLevel = -1;
        }
        setProgress(mBatteryLevel);
    }

    @Override
    public boolean isRunning() {
        return mChargingLevel != -1;
    }

}
