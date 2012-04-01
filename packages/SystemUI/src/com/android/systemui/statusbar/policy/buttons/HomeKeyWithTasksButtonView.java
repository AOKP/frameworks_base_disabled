
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

    private static final String TAG = "Key.Home";

    IStatusBarService mBarService;

    public static final int LONGPRESS_NONE = 0;
    public static final int LONGPRESS_HOME = 1;

    public HomeKeyWithTasksButtonView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HomeKeyWithTasksButtonView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs);

        if (Settings.System.getInt(getContext().getContentResolver(),
                Settings.System.NAVIGATION_BAR_HOME_LONGPRESS, 0) >= 1)
            mSupportsLongpress = true;
        else
            mSupportsLongpress = false;

        setOnLongClickListener(mLongPressListener);

    }

    Runnable mCheckLongPress = new Runnable() {
        public void run() {
            if (isPressed()) {
                performLongClick();
            }
        }
    };

    private OnLongClickListener mLongPressListener = new OnLongClickListener() {

        @Override
        public boolean onLongClick(View v) {
            try {
                mBarService = IStatusBarService.Stub.asInterface(
                        ServiceManager.getService(Context.STATUS_BAR_SERVICE));
                mBarService.toggleRecentApps();
            } catch (RemoteException e) {
            }
            return true;
        }
    };

}
