/*
 * Copyright (C) 2010 The Android Open Source Project
 * Copyright 2011 Colin McDonough
 * 
 * Modified for AOKP by Mike Wilson (Zaphod-Beeblebrox)
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
import android.content.Intent;
import android.util.Log;

import com.android.systemui.R;

/**
 * TODO: Fix the WakeLock
 */
public class TorchToggle extends Toggle {

    private static final String TAG = "TorchToggle";

    private boolean mIsTorchOn;
    private Context mContext;
    private Torch mTorch;
    private int runawayflag;

    public TorchToggle(Context context) {
        super(context);
        setLabel(R.string.toggle_torch);
        setIcon(R.drawable.toggle_torch);
        mContext = context;
    }

    @Override
    protected void updateInternalToggleState() {
        Log.d(TAG, "Updating TorchToggleState");
        if (mIsTorchOn) {
            if (mTorch == null) {
                onCheckChanged(true); // It looks like we are 'on' but don't have a torch reference
                                      // .. try to restart?
            }
        } else {
            if (mTorch != null) {
                mTorch.finish(); // looks like we are 'off', but still have a torch .. finish it.
            }
        }
    }

    @Override
    protected void onCheckChanged(boolean isChecked) {
        if (isChecked) {
            Log.d(TAG, "Starting Torch Intent");
            Intent intent = new Intent(mContext, Torch.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(intent);
            mIsTorchOn = true;
            runawayflag = 0;
        }
        else {
            if (mTorch != null) {
                Log.d(TAG, "Destroying Torch");
                mTorch.finish();
            } else {
                Log.d(TAG, "Tried to destroy Torch that was NULL!"); // I'm going to try a little
                                                                     // recursion here.
                if (++runawayflag > 10) {
                    Log.d(TAG, "Tried 10 times to get Torch!"); // I don't want an uncontrolled
                                                                // loop.
                } else {
                    mTorch = Torch.getTorch(); // let's try to get torch and retry the procedure
                    onCheckChanged(false); // I love recursion!
                }
            }
            mIsTorchOn = false;
        }
        mTorch = Torch.getTorch();
    }

    @Override
    protected boolean onLongPress() {
        return false;
    }

}
