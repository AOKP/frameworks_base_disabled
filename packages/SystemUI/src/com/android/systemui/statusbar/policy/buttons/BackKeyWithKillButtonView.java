
package com.android.systemui.statusbar.policy.buttons;

import java.util.List;

import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.content.Context;
import android.os.Handler;
import android.os.Process;
import android.os.RemoteException;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManagerPolicy.WindowState;

import com.android.systemui.statusbar.policy.KeyButtonView;

public class BackKeyWithKillButtonView extends KeyButtonView {

    Handler mHandler;

    public BackKeyWithKillButtonView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BackKeyWithKillButtonView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs);

        this.setOnLongClickListener(mBackKey);
        mSupportsLongpress = true;

        mHandler = new Handler();
    }

    Runnable mCheckLongPress = new Runnable() {
        public void run() {
            if (isPressed()) {
                performLongClick();
            }
        }
    };

    Runnable mBackLongPress = new Runnable() {
        public void run() {
            try {
                IActivityManager mgr = ActivityManagerNative.getDefault();
                List<RunningAppProcessInfo> apps = mgr.getRunningAppProcesses();
                for (RunningAppProcessInfo appInfo : apps) {
                    int uid = appInfo.uid;
                    // Make sure it's a foreground user application (not system,
                    // root, phone, etc.)
                    if (uid >= Process.FIRST_APPLICATION_UID && uid <= Process.LAST_APPLICATION_UID
                            && appInfo.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                        // Kill the entire pid
                        if (appInfo.pkgList != null && (apps.size() > 0)) {
                            mgr.forceStopPackage(appInfo.pkgList[0]);
                        } else {
                            Process.killProcess(appInfo.pid);
                        }
                        break;
                    }
                }
            } catch (RemoteException remoteException) {
                // Do nothing; just let it go.
            }
        }
    };

    private OnLongClickListener mBackKey = new OnLongClickListener() {

        @Override
        public boolean onLongClick(View v) {
            if (Settings.Secure.getInt(mContext.getContentResolver(),
                    Settings.Secure.KILL_APP_LONGPRESS_BACK, 0) == 1) {
                mHandler.postDelayed(mBackLongPress, ViewConfiguration.getGlobalActionKeyTimeout());
            }
            return true;
        }
    };

}
