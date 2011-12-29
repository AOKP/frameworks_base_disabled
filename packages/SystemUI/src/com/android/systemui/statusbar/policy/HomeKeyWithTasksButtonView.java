
package com.android.systemui.statusbar.policy;

import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.AttributeSet;
import android.view.View;

import com.android.internal.statusbar.IStatusBarService;
import com.android.systemui.statusbar.phone.PhoneStatusBar;

public class HomeKeyWithTasksButtonView extends KeyButtonView {
    
    IStatusBarService mBarService;

    public HomeKeyWithTasksButtonView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HomeKeyWithTasksButtonView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs);
        
        mBarService = IStatusBarService.Stub.asInterface(
                ServiceManager.getService(Context.STATUS_BAR_SERVICE));
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
            try {
                mBarService.toggleRecentApps();
            } catch (RemoteException e) {
            }
            return true;
        }
    };

}
