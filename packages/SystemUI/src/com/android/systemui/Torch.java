/*
 * Copyright 2011 Colin McDonough
 *
 * Modified for AOKP by Mike Wilson - Zaphod-Beeblebrox
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

package com.android.systemui;

import java.io.IOException;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;

import com.android.systemui.R;

/*
 * Torch is an LED flashlight.
 */
public class Torch extends Activity implements SurfaceHolder.Callback {

    private static final String TAG = "Torch";

    private static final String WAKE_LOCK_TAG = "TORCH_WAKE_LOCK";

    private Camera mCamera;
    private boolean lightOn;
    private boolean startingTorch;
    private boolean previewOn;
    private View button;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private SharedPreferences prefs;

    private WakeLock wakeLock;

    private static Torch torch;
    
    public static final String KEY_TORCH_ON = "torch_on";
    public static final String INTENT_TORCH_ON = "com.android.systemui.INTENT_TORCH_ON";
    public static final String INTENT_TORCH_OFF = "com.android.systemui.INTENT_TORCH_OFF";

    public Torch() {
        super();
        torch = this;
    }

    public static Torch getTorch() {
        return torch;
    }

    private void getCamera() {
        if (mCamera == null) {
            try {
                mCamera = Camera.open();
            } catch (RuntimeException e) {
                Log.e(TAG, "Camera.open() failed: " + e.getMessage());
            }
        }
    }

    private void turnLightOn() {

        if (mCamera == null) {
            Log.d(TAG, "Camera not Found!");
            return;
        }
        lightOn = true;
        Parameters parameters = mCamera.getParameters();
        if (parameters == null) {
            Log.d(TAG, "Camera Params not Found!");
            return;
        }
        List<String> flashModes = parameters.getSupportedFlashModes();
        // Check if camera flash exists
        if (flashModes == null) {
            Log.d(TAG, "Camera Flash not Found!");
            return;
        }
        String flashMode = parameters.getFlashMode();
        //Log.i(TAG, "Flash mode: " + flashMode);
        //Log.i(TAG, "Flash modes: " + flashModes);
        if (!Parameters.FLASH_MODE_TORCH.equals(flashMode)) {
            // Turn on the flash
            if (flashModes.contains(Parameters.FLASH_MODE_TORCH)) {
                parameters.setFlashMode(Parameters.FLASH_MODE_TORCH);
                mCamera.setParameters(parameters);
                startWakeLock();
            } else {
                Log.e(TAG, "FLASH_MODE_TORCH not supported");
            }
        }
    }

    private void turnLightOff() {
        if (lightOn) {
            // set the background to dark
            lightOn = false;
            if (mCamera == null) {
                return;
            }
            Parameters parameters = mCamera.getParameters();
            if (parameters == null) {
                return;
            }
            List<String> flashModes = parameters.getSupportedFlashModes();
            String flashMode = parameters.getFlashMode();
            // Check if camera flash exists
            if (flashModes == null) {
                return;
            }
            Log.i(TAG, "Flash mode: " + flashMode);
            Log.i(TAG, "Flash modes: " + flashModes);
            if (!Parameters.FLASH_MODE_OFF.equals(flashMode)) {
                // Turn off the flash
                if (flashModes.contains(Parameters.FLASH_MODE_OFF)) {
                    parameters.setFlashMode(Parameters.FLASH_MODE_OFF);
                    mCamera.setParameters(parameters);
                    stopWakeLock();
                } else {
                    Log.e(TAG, "FLASH_MODE_OFF not supported");
                }
            }
        }
    }

    private void startPreview() {
        if (!previewOn && mCamera != null) {
            mCamera.startPreview();
            previewOn = true;
        }
    }

    private void stopPreview() {
        if (previewOn && mCamera != null) {
            mCamera.stopPreview();
            previewOn = false;
        }
    }

    private void startWakeLock() {
        if (wakeLock == null) {
            Log.d(TAG, "wakeLock is null, getting a new WakeLock");
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            Log.d(TAG, "PowerManager acquired");
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG);
            Log.d(TAG, "WakeLock set");
        }
        wakeLock.acquire();
        Log.d(TAG, "WakeLock acquired");
    }

    private void stopWakeLock() {
        if (wakeLock != null) {
            wakeLock.release();
            Log.d(TAG, "WakeLock released");
        }
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.torch_toggle);
        surfaceView = (SurfaceView) this.findViewById(R.id.surfaceview);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        prefs = getSharedPreferences("torch", Context.MODE_WORLD_READABLE);
        Log.i(TAG, "onCreate");
    }

    private void startTorch(){
    	if (!startingTorch) {
    		startingTorch = true;
    		getCamera();
    		startPreview();
    		turnLightOn();
    		SharedPreferences.Editor editor = prefs.edit();
        	editor.putBoolean(KEY_TORCH_ON,false);
        	editor.commit();
        	startingTorch = false;
    	}
    }
    
    private void stopTorch(){
    	if (!startingTorch) {
    		if (mCamera != null) {
                turnLightOff();
                stopPreview();
                mCamera.release();
    		}
    		SharedPreferences.Editor editor = prefs.edit();
        	editor.putBoolean(KEY_TORCH_ON,false);
        	editor.commit();
    		this.finish();
    	}
    }
    
        @Override
    public void onRestart() {
        super.onRestart();
    }

    @Override
    public void onStart() {
    	super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        String action = getIntent().getAction();
    	if (action == INTENT_TORCH_ON) {
    		startTorch();
    	} else if (action == INTENT_TORCH_OFF) {
    		stopTorch();
    	}
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
    
    @Override
    public void onNewIntent(Intent intent){
    	setIntent(intent);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int I, int J, int K) {
        moveTaskToBack(true); // once Surface is set up - we should be able to background ourselves.
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            mCamera.setPreviewDisplay(holder);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    }
}
