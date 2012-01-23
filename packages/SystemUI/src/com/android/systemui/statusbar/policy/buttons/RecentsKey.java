
package com.android.systemui.statusbar.policy.buttons;

import android.content.Context;
import android.os.Handler;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.AttributeSet;
import android.view.View;

import com.android.internal.statusbar.IStatusBarService;
import com.android.systemui.statusbar.policy.KeyButtonView;

public class RecentsKey extends KeyButtonView {

    Handler mHandler;

    public RecentsKey(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RecentsKey(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs);

        this.setOnLongClickListener(mLongRecent);

        mSupportsLongpress = true;
        mHandler = new Handler();

        this.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    final IStatusBarService mBarService = IStatusBarService.Stub.asInterface(
                            ServiceManager.getService(Context.STATUS_BAR_SERVICE));
                    mBarService.toggleRecentApps();
                } catch (RemoteException e) {
                }
            }
        });
    }

    Runnable mLongClick = new Runnable() {
        public void run() {
           if(isPressed()) {
               performLongClick();
           }
        }
    };

    Runnable mFirstLongClickOption = new Runnable() {
        public void run() {
            try {
                //TODO do stuff here
            } catch (RemoteException RE) {
                //swallowed exception should probably
                //be handled with more grace
            }
        }
    };

    private OnLongClickListener mLongRecent = new OnLongClickListener() {

        @Override
        public boolean onLongClick(View v) {
            if (Settings.Secure.getInt(mContext.getContentResolver(),
                    Settings.Secure.LONGPRESS_APP_TASKER_INTENT, 0) == 1) {
                //once we have other options we will send them to
                //new Runnables in RecentsKey(Context, AttributeSet, int)
                //by switching what we send to the Handler
                mHandler.post(mFirstLongClickOption);
            }
            return true;
        }
    };

}
