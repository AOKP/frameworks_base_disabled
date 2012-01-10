
package com.android.systemui.statusbar.policy.toggles;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;

import com.android.systemui.R;

public class SilentToggle extends Toggle {

    public SilentToggle(Context context) {
        super(context);

        updateState();
        setLabel(R.string.toggle_silent);
        setIcon(R.drawable.toggle_silence);

        IntentFilter filter = new IntentFilter();
        filter.addAction(AudioManager.RINGER_MODE_CHANGED_ACTION);
        context.registerReceiver(new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                updateState();
            }
        }, filter);
    }

    @Override
    protected void updateInternalToggleState() {
        AudioManager am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        int mode = am.getRingerMode();
        mToggle.setChecked(mode == AudioManager.RINGER_MODE_SILENT);
    }

    @Override
    protected void onCheckChanged(boolean isChecked) {
        AudioManager am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        am.setRingerMode(isChecked ? AudioManager.RINGER_MODE_SILENT
                : AudioManager.RINGER_MODE_NORMAL);
    }
}
