/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.statusbar.policy.toggles;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import com.android.internal.statusbar.IStatusBarService;
import android.os.ServiceManager;
import android.os.RemoteException;

import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.systemui.R;

/**
 * TODO: Listen for changes to the setting.
 */
public abstract class Toggle implements OnCheckedChangeListener {

    protected static final String TAG = "Toggle";
    private static final boolean DEBUG = true;

    View mView;
    protected Context mContext;

    // widgets
    protected ImageView mIcon;
    protected TextView mText;
    protected CompoundButton mToggle;

    protected boolean mSystemChange = false;

    public Toggle(Context context) {
        mContext = context;

        boolean useAltButtonLayout = Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUSBAR_TOGGLES_USE_BUTTONS, 0) == 1;

        mView = View.inflate(mContext, useAltButtonLayout ? R.layout.toggle_button
                : R.layout.toggle, null);

        mIcon = (ImageView) mView.findViewById(R.id.icon);
        mToggle = (CompoundButton) mView.findViewById(R.id.toggle);
        mText = (TextView) mView.findViewById(R.id.label);

        mToggle.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public final void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (mSystemChange)
                    return;
                onCheckChanged(isChecked);
            }
        });

        mToggle.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (onLongPress()) {
                    collapseStatusBar();
                    return true;
                } else
                    return false;
            }
        });
    }

    /**
     * this method is called when we need to update the state of the toggle due to outside
     * interactions.
     */
    protected abstract void updateInternalToggleState();

    /**
     * this method is called when the user manually toggles, update states as needed
     */
    protected abstract void onCheckChanged(boolean isChecked);

    /**
     * this method is called when the user longpresses the toggle
     */
    protected abstract boolean onLongPress();

    public void updateState() {
        mSystemChange = true;
        updateInternalToggleState();
        mSystemChange = false;
    }

    public void collapseStatusBar() {
        try {
            IStatusBarService sb = IStatusBarService.Stub.asInterface(
                    ServiceManager.getService(Context.STATUS_BAR_SERVICE));
            sb.collapse();
        } catch (RemoteException e) {
        }
    }

    @Override
    public final void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (mSystemChange)
            return;
        onCheckChanged(isChecked);
    }

    public View getView() {
        return mView;
    }

    public void setLabel(String s) {
        if (mText != null)
            mText.setText(s);
    }

    public void setLabel(int res) {
        if (mText != null)
            mText.setText(res);
    }

    public void setIcon(int res) {
        if (mIcon != null) {
            mIcon.setImageResource(res);
        }
    }

    public void setupInfo(boolean showIcon, boolean showText) {
        mIcon.setVisibility(showIcon ? View.VISIBLE : View.GONE);
        mText.setVisibility(showText ? View.VISIBLE : View.GONE);
    }

    protected void onStatusbarExpanded() {
    }
}
