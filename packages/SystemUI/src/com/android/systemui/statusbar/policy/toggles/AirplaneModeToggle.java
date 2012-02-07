
package com.android.systemui.statusbar.policy.toggles;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.provider.Settings;

import com.android.systemui.R;

public class AirplaneModeToggle extends Toggle {
    private static final String TAG = "StatusBar.AirplaneModeController";

    private boolean mAirplaneMode;

    public AirplaneModeToggle(Context context) {
        super(context);

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        context.registerReceiver(mBroadcastReceiver, filter);

        updateState();
        setLabel(R.string.toggle_airplane);
//        if (mToggle.isChecked())
        	setIcon(R.drawable.toggle_airplane);
//        else
//        	setIcon(R.drawable.toggle_airplane_off);

    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(intent.getAction())) {
                final boolean enabled = intent.getBooleanExtra("state", false);
                if (enabled != mAirplaneMode) {
                    mAirplaneMode = enabled;
                }
                updateState();
            }
        }
    };

    private boolean getAirplaneMode() {
        ContentResolver cr = mContext.getContentResolver();
        return 0 != Settings.System.getInt(cr, Settings.System.AIRPLANE_MODE_ON, 0);
    }

    // TODO: Fix this racy API by adding something better to TelephonyManager or
    // ConnectivityService.
    private void unsafe(final boolean enabled) {
        AsyncTask.execute(new Runnable() {
            public void run() {
                Settings.System.putInt(
                        mContext.getContentResolver(),
                        Settings.System.AIRPLANE_MODE_ON,
                        enabled ? 1 : 0);
                Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
                intent.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
                intent.putExtra("state", enabled);
                mContext.sendBroadcast(intent);
            }
        });
    }

    @Override
    protected void updateInternalToggleState() {
        mAirplaneMode = getAirplaneMode();
        mToggle.setChecked(mAirplaneMode);
//        if (mToggle.isChecked())
//        	setIcon(R.drawable.toggle_airplane);
//        else
//        	setIcon(R.drawable.toggle_airplane_off);
    }

    @Override
    protected void onCheckChanged(boolean checked) {
        if (checked != mAirplaneMode) {
            mAirplaneMode = checked;
            unsafe(checked);
        }
        if (checked)
        	 setIcon(R.drawable.toggle_airplane);
        else
        	 setIcon(R.drawable.toggle_airplane_off);
    }
    
    @Override
    protected boolean onLongPress() {
    	Intent intent = new Intent(android.provider.Settings.ACTION_AIRPLANE_MODE_SETTINGS);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
    	return true;
    }
}
