package com.android.systemui.statusbar.toggles;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.os.AsyncTask;
import android.provider.Settings;
import android.util.Log;

import com.android.systemui.R;

public class NFCToggle extends Toggle {
    private static final String TAG = "StatusBar.NFCToggle";

    private boolean mNfcEnabled;
    private NfcAdapter mNfcAdapter;

    public NFCToggle(Context context) {
        super(context);
        IntentFilter filter = new IntentFilter();
        filter.addAction(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED);
        context.registerReceiver(mBroadcastReceiver, filter);
        setLabel(R.string.toggle_nfc);
        updateState();
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (NfcAdapter.ACTION_ADAPTER_STATE_CHANGED.equals(intent.getAction())) {
                final boolean enabled = (intent.getIntExtra(NfcAdapter.EXTRA_ADAPTER_STATE,
                        NfcAdapter.STATE_OFF) == NfcAdapter.STATE_ON);
                mNfcEnabled = enabled;
                updateState();
            }
        }
    };
    
    private boolean getNfcState() {
        if (mNfcAdapter == null) {
            Log.d(TAG, "NFC service not running.");
            return false;
        }
        else {
            return mNfcAdapter.isEnabled();
        }
    }
    
    private void setNfcState(final boolean desiredState) {
        if (mNfcAdapter == null) {
            Log.d(TAG, "NFC service not running.");
        } else {
            AsyncTask.execute(new Runnable() {
                public void run() {
                    if (desiredState) {
                        mNfcAdapter.enable();
                    } else {
                        mNfcAdapter.disable();
                    }
                    return;
                }
            });
        }
    }

    @Override
    protected boolean updateInternalToggleState() {
        mNfcEnabled = getNfcState();
        mToggle.setChecked(mNfcEnabled);
        if (mToggle.isChecked())
            setIcon(R.drawable.toggle_nfc);
        else
            setIcon(R.drawable.toggle_nfc_off);
        return mToggle.isChecked();
    }

    @Override
    protected void onCheckChanged(boolean checked) {
        if (mNfcAdapter == null) { // try to get it again
            mNfcAdapter = NfcAdapter.getDefaultAdapter(mContext);
        }
        if (checked != mNfcEnabled) {
            mNfcEnabled = checked;
            setNfcState(mNfcEnabled);
        }
        // don't update immediately, NfcAdapter's broadcast will do it
        // updateState();
    }

    @Override
    protected boolean onLongPress() {
        if (mContext != null) {
            Intent intent = new Intent(
                    android.provider.Settings.ACTION_WIRELESS_SETTINGS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(intent);
        }
        return true;
    }
}