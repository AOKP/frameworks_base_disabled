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
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.app.PendingIntent;
import com.roman.romcontrol.service;
import com.android.systemui.R;

/**
 * TODO: Fix the WakeLock
 */
public class TorchToggle extends Toggle implements OnSharedPreferenceChangeListener {

    private static final String TAG = "TorchToggle";
    
    public static final String KEY_TORCH_ON = "torch_on";
    public static final String INTENT_TORCH_ON = "com.aokp.romcontrol.INTENT_TORCH_ON";
    public static final String INTENT_TORCH_OFF = "com.aokp.romcontrol.INTENT_TORCH_OFF";

    private boolean mIsTorchOn;
    private Context mContext;
    
    SharedPreferences prefs;
    
    PendingIntent torchIntent;
    
    public TorchToggle(Context context) {
        super(context);
        setLabel(R.string.toggle_torch);
        if (mToggle.isChecked())
        	setIcon(R.drawable.toggle_torch);
        else
        	setIcon(R.drawable.toggle_torch_off);
        mContext = context;
        prefs = mContext.getSharedPreferences("torch", Context.MODE_WORLD_READABLE);
        prefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void updateInternalToggleState() {
        mToggle.setChecked(mIsTorchOn);
        if (mToggle.isChecked())
        	setIcon(R.drawable.toggle_torch);
        else
        	setIcon(R.drawable.toggle_torch_off);
    }

    @Override
    protected void onCheckChanged(boolean isChecked) {
        if (isChecked) {
            Log.d(TAG, "Starting Torch_ON Intent");
            Intent i = new Intent(mContext, TorchService.class);
            i.setAction(TorchService.INTENT_TORCH_ON);
            torchIntent = PendingIntent.getService(mContext, 0, i,
                    0);             
        }
        else {
        	Log.d(TAG, "Starting Torch_OFF Intent");
            Intent i = new Intent(mContext, TorchService.class);
            i.setAction(TorchService.INTENT_TORCH_OFF);
            torchIntent = PendingIntent.getService(mContext, 0, i,
                    0);  
        }
    }
    
    @Override
    protected boolean onLongPress() {
    	return false;
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,String key) 
    {
      mIsTorchOn = sharedPreferences.getBoolean(KEY_TORCH_ON,false);
      updateInternalToggleState();
    }
}
