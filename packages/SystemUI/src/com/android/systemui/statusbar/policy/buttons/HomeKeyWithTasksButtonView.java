
package com.android.systemui.statusbar.policy.buttons;

import android.content.Context;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.android.internal.statusbar.IStatusBarService;
import com.android.systemui.statusbar.policy.KeyButtonView;

public class HomeKeyWithTasksButtonView extends KeyButtonView {

    IStatusBarService mBarService;

    public static final int LONGPRESS_NONE = 0;
    public static final int LONGPRESS_HOME = 1;

    public HomeKeyWithTasksButtonView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HomeKeyWithTasksButtonView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs);

        this.setOnLongClickListener(mRecentsToggle);
        mSupportsLongpress = true;
    }

    Runnable mCheckLongPress = new Runnable() {
        public void run() {
            if (isPressed()) {
                performLongClick();
            }
        }
    };

    private OnLongClickListener mRecentsToggle = new OnLongClickListener() {

        @Override
        public boolean onLongClick(View v) {
            int setting = Settings.System.getInt(mContext.getContentResolver(),
                    Settings.System.NAVIGATION_BAR_HOME_LONGPRESS, LONGPRESS_NONE);

            Log.e("HOME_BUTTON", "setting: " + setting);
            switch (setting) {
                case LONGPRESS_NONE:
                    return true;

                case LONGPRESS_HOME:
                    try {
                        mBarService = IStatusBarService.Stub.asInterface(
                                ServiceManager.getService(Context.STATUS_BAR_SERVICE));
                        mBarService.toggleRecentApps();
                        Log.e("HOME_BUTTON", "toggled recent apps");
                        return true;
                    } catch (RemoteException e) {
                    }
                    return false;
            }
            return false;
        }
    };

}
