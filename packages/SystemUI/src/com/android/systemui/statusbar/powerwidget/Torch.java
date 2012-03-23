/*
 * Copyright 2011 Colin McDonough
 *
 * This has been modified by syoaran12 (Adam Fisch)
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

package com.android.systemui.statusbar.powerwidget;

import java.io.IOException;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.KeyEvent;
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
  private boolean previewOn;
  private View button;
  private SurfaceView surfaceView;
  private SurfaceHolder surfaceHolder;

  private WakeLock wakeLock;

  private static Torch torch;

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

  /*
   * Called by the view (see fake_torch.xml)
   */
  public void toggleLight(View view) {
    toggleLight();
  }

  private void toggleLight() {
    if (lightOn) {
      turnLightOff();
    } else {
      turnLightOn();
    }
  }

  private void turnLightOn() {
    if (mCamera == null) {
    	Log.d(TAG, "Y U NO HAVE CAMERA!");
      return;
    }
    lightOn = true;
    Parameters parameters = mCamera.getParameters();
    if (parameters == null) {
    	Log.d(TAG, "Y U NO HAVE CAMERA PARAMS!");
      return;
    }
    List<String> flashModes = parameters.getSupportedFlashModes();
    // Check if camera flash exists
    if (flashModes == null) {
    	Log.d(TAG, "Y U NO HAVE FLASH!");
      return;
    }
    String flashMode = parameters.getFlashMode();
    Log.i(TAG, "Flash mode: " + flashMode);
    Log.i(TAG, "Flash modes: " + flashModes);
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
      lightOn = false;
      if (mCamera == null) {
    	  Log.d(TAG, "Y U NO HAVE CAMERA!");
        return;
      }
      Parameters parameters = mCamera.getParameters();
      if (parameters == null) {
    	  Log.d(TAG, "Y U NO HAVE CAMERA PARAMS!");
        return;
      }
      List<String> flashModes = parameters.getSupportedFlashModes();
      String flashMode = parameters.getFlashMode();
      // Check if camera flash exists
      if (flashModes == null) {
    	  Log.d(TAG, "Y U NO HAVE FLASH!");
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
    setContentView(R.layout.fake_torch);
    surfaceView = (SurfaceView) this.findViewById(R.id.surfaceview);
    surfaceHolder = surfaceView.getHolder();
    surfaceHolder.addCallback(this);
    surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    disablePhoneSleep();
    Log.i(TAG, "onCreate");
  }

  private void disablePhoneSleep() {
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
  }

  @Override
  public void onRestart() {
    super.onRestart();
    Log.i(TAG, "onRestart");
  }

  @Override
  public void onStart() {
    super.onStart();
    Log.i(TAG, "onStart");
    getCamera();
    startPreview();
  }

  @Override
  public void onResume() {
    super.onResume();
    turnLightOn();
    Log.i(TAG, "onResume");
  }

  @Override
  public void onPause() {
    super.onPause();
    Log.i(TAG, "onPause");
  }

  @Override
  public void onStop() {
    super.onStop();
    Log.i(TAG, "onStop");
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    if (mCamera != null) {
      turnLightOff();
      stopPreview();
      mCamera.release();
      torch = null;
    }
    Log.i(TAG, "onDestroy");
  }

  @Override
  public void surfaceChanged(SurfaceHolder holder, int I, int J, int K) {
    Log.d(TAG, "surfaceChanged");
  }

  @Override
  public void surfaceCreated(SurfaceHolder holder) {
    Log.d(TAG, "surfaceCreated");
    try {
      mCamera.setPreviewDisplay(holder);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void surfaceDestroyed(SurfaceHolder holder) {
    Log.d(TAG, "surfaceDestroyed");
  }
}