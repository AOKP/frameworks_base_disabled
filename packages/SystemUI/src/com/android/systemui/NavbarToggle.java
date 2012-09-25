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

import android.app.Activity;
import android.os.Bundle;
import android.provider.Settings;

/*
 * Torch is an LED flashlight.
 */
public class NavbarToggle extends Activity  {
  public NavbarToggle() {
    super();
  }

  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }

  @Override
  public void onResume() {
    super.onResume();
    final boolean NavOn = Settings.System.getInt(getContentResolver(),
      Settings.System.NAVIGATION_BAR_SHOW_NOW, 1) == 1;
    Settings.System.putInt(getContentResolver(),
      Settings.System.NAVIGATION_BAR_SHOW_NOW,(!NavOn) ? 1 : 0);
    this.finish();
  }
}
