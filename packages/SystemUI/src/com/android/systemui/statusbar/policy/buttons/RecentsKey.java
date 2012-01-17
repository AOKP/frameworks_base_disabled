
package com.android.systemui.statusbar.policy.buttons;

import android.content.Context;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.AttributeSet;
import android.view.View;

import com.android.internal.statusbar.IStatusBarService;
import com.android.systemui.statusbar.policy.KeyButtonView;

public class RecentsKey extends KeyButtonView {

    public RecentsKey(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RecentsKey(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs);

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

}
