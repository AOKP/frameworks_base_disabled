/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.internal.policy.impl;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.internal.R;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.SlidingTab;
import com.android.internal.widget.WaveView;
import com.android.internal.widget.multiwaveview.MultiWaveView;

/**
 * The screen within {@link LockPatternKeyguardView} that shows general information about the device
 * depending on its state, and how to get past it, as applicable.
 */
class LockScreen extends LinearLayout implements KeyguardScreen {

    private static final int ON_RESUME_PING_DELAY = 500; // delay first ping until the screen is on
    private static final boolean DBG = false;
    private static final String TAG = "LockScreen";
    private static final String ENABLE_MENU_KEY_FILE = "/data/local/enable_menu_key";
    private static final int WAIT_FOR_ANIMATION_TIMEOUT = 0;
    private static final int STAY_ON_WHILE_GRABBED_TIMEOUT = 30000;

    public static final int LAYOUT_STOCK = 2;
    public static final int LAYOUT_QUAD = 6;
    public static final int LAYOUT_OCTO = 8;

    private int mLockscreenTargets = LAYOUT_STOCK;

    private LockPatternUtils mLockPatternUtils;
    private KeyguardUpdateMonitor mUpdateMonitor;
    private KeyguardScreenCallback mCallback;

    // current configuration state of keyboard and display
    private int mKeyboardHidden;
    private int mCreationOrientation;

    private boolean mSilentMode;
    private AudioManager mAudioManager;
    private boolean mEnableMenuKeyInLockScreen;

    private KeyguardStatusViewManager mStatusViewManager;
    private UnlockWidgetCommonMethods mUnlockWidgetMethods;
    private View mUnlockWidget;

    private TextView mCarrier;

    private Drawable[] lockDrawables;

    ArrayList<Target> lockTargets = new ArrayList<Target>();

    private interface UnlockWidgetCommonMethods {
        // Update resources based on phone state
        public void updateResources();

        // Get the view associated with this widget
        public View getView();

        // Reset the view
        public void reset(boolean animate);

        // Animate the widget if it supports ping()
        public void ping();
    }

    class SlidingTabMethods implements SlidingTab.OnTriggerListener, UnlockWidgetCommonMethods {
        private final SlidingTab mSlidingTab;

        SlidingTabMethods(SlidingTab slidingTab) {
            mSlidingTab = slidingTab;
        }

        public void updateResources() {
            boolean vibe = mSilentMode
                    && (mAudioManager.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE);

            mSlidingTab.setRightTabResources(
                    mSilentMode ? (vibe ? R.drawable.ic_jog_dial_vibrate_on
                            : R.drawable.ic_jog_dial_sound_off)
                            : R.drawable.ic_jog_dial_sound_on,
                    mSilentMode ? R.drawable.jog_tab_target_yellow
                            : R.drawable.jog_tab_target_gray,
                    mSilentMode ? R.drawable.jog_tab_bar_right_sound_on
                            : R.drawable.jog_tab_bar_right_sound_off,
                    mSilentMode ? R.drawable.jog_tab_right_sound_on
                            : R.drawable.jog_tab_right_sound_off);
        }

        /** {@inheritDoc} */
        public void onTrigger(View v, int whichHandle) {
            if (whichHandle == SlidingTab.OnTriggerListener.LEFT_HANDLE) {
                mCallback.goToUnlockScreen();
            } else if (whichHandle == SlidingTab.OnTriggerListener.RIGHT_HANDLE) {
                toggleRingMode();
                mCallback.pokeWakelock();
            }
        }

        /** {@inheritDoc} */
        public void onGrabbedStateChange(View v, int grabbedState) {
            if (grabbedState == SlidingTab.OnTriggerListener.RIGHT_HANDLE) {
                mSilentMode = isSilentMode();
                mSlidingTab.setRightHintText(mSilentMode ? R.string.lockscreen_sound_on_label
                        : R.string.lockscreen_sound_off_label);
            }
            // Don't poke the wake lock when returning to a state where the handle is
            // not grabbed since that can happen when the system (instead of the user)
            // cancels the grab.
            if (grabbedState != SlidingTab.OnTriggerListener.NO_HANDLE) {
                mCallback.pokeWakelock();
            }
        }

        public View getView() {
            return mSlidingTab;
        }

        public void reset(boolean animate) {
            mSlidingTab.reset(animate);
        }

        public void ping() {
        }
    }

    class WaveViewMethods implements WaveView.OnTriggerListener, UnlockWidgetCommonMethods {

        private final WaveView mWaveView;

        WaveViewMethods(WaveView waveView) {
            mWaveView = waveView;
        }

        /** {@inheritDoc} */
        public void onTrigger(View v, int whichHandle) {
            if (whichHandle == WaveView.OnTriggerListener.CENTER_HANDLE) {
                requestUnlockScreen();
            }
        }

        /** {@inheritDoc} */
        public void onGrabbedStateChange(View v, int grabbedState) {
            // Don't poke the wake lock when returning to a state where the handle is
            // not grabbed since that can happen when the system (instead of the user)
            // cancels the grab.
            if (grabbedState == WaveView.OnTriggerListener.CENTER_HANDLE) {
                mCallback.pokeWakelock(STAY_ON_WHILE_GRABBED_TIMEOUT);
            }
        }

        public void updateResources() {
        }

        public View getView() {
            return mWaveView;
        }

        public void reset(boolean animate) {
            mWaveView.reset();
        }

        public void ping() {
        }
    }

    class MultiWaveViewMethods implements MultiWaveView.OnTriggerListener,
            UnlockWidgetCommonMethods {

        private final MultiWaveView mMultiWaveView;
        private boolean mCameraDisabled;

        MultiWaveViewMethods(MultiWaveView multiWaveView) {
            mMultiWaveView = multiWaveView;
            final boolean cameraDisabled = mLockPatternUtils.getDevicePolicyManager()
                    .getCameraDisabled(null);
            if (cameraDisabled) {
                Log.v(TAG, "Camera disabled by Device Policy");
                mCameraDisabled = true;
            } else {
                // Camera is enabled if resource is initially defined for MultiWaveView
                // in the lockscreen layout file
                mCameraDisabled = mMultiWaveView.getTargetResourceId()
                        != R.array.lockscreen_targets_with_camera;
            }
        }

        public void updateResources() {
            boolean isLandscape = (mCreationOrientation == Configuration
                    .ORIENTATION_LANDSCAPE);

            targetController.setLandscape(isLandscape);
            targetController.setupTargets();

            mMultiWaveView.setTargetResources(targetController.getDrawables());
        }

        public void onGrabbed(View v, int handle) {

        }

        public void onReleased(View v, int handle) {

        }

        public void onTrigger(View v, int target) {
            if (DBG)
                Log.v(TAG, "onTrigger: target = " + target);
            if (DBG)
                Log.v(TAG, "onTrigger: Orientation = " + mCreationOrientation);
            targetController.getTarget(target).doAction();
        }

        public void onGrabbedStateChange(View v, int handle) {
            // Don't poke the wake lock when returning to a state where the handle is
            // not grabbed since that can happen when the system (instead of the user)
            // cancels the grab.
            if (handle != MultiWaveView.OnTriggerListener.NO_HANDLE) {
                mCallback.pokeWakelock();
            }
        }

        public View getView() {
            return mMultiWaveView;
        }

        public void reset(boolean animate) {
            mMultiWaveView.reset(animate);
        }

        public void ping() {
            mMultiWaveView.ping();
        }
    }

    private void requestUnlockScreen() {
        // Delay hiding lock screen long enough for animation to finish
        postDelayed(new Runnable() {
            public void run() {
                mCallback.goToUnlockScreen();
            }
        }, WAIT_FOR_ANIMATION_TIMEOUT);
    }

    private void toggleRingMode() {
        // toggle silent mode
        mSilentMode = !mSilentMode;
        if (mSilentMode) {
            final boolean vibe = (Settings.System.getInt(
                    mContext.getContentResolver(),
                    Settings.System.VIBRATE_IN_SILENT, 1) == 1);

            mAudioManager.setRingerMode(vibe
                    ? AudioManager.RINGER_MODE_VIBRATE
                    : AudioManager.RINGER_MODE_SILENT);
        } else {
            mAudioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
        }
    }

    /**
     * In general, we enable unlocking the insecure key guard with the menu key. However, there are
     * some cases where we wish to disable it, notably when the menu button placement or technology
     * is prone to false positives.
     * 
     * @return true if the menu key should be enabled
     */
    private boolean shouldEnableMenuKey() {
        final Resources res = getResources();
        final boolean configDisabled = res.getBoolean(R.bool.config_disableMenuKeyInLockScreen);
        final boolean isTestHarness = ActivityManager.isRunningInTestHarness();
        final boolean fileOverride = (new File(ENABLE_MENU_KEY_FILE)).exists();
        boolean defaultValue = !configDisabled || isTestHarness || fileOverride;

        return (Settings.System.getInt(getContext().getContentResolver(),
                Settings.System.LOCKSCREEN_ENABLE_MENU_KEY, defaultValue ? 1 : 0) == 1);
    }

    class Target {
        public static final String ACTION_UNLOCK = "**unlock**";
        public static final String ACTION_SOUND_TOGGLE = "**sound**";
        public static final String ACTION_APP_PHONE = "**phone**";
        public static final String ACTION_APP_CAMERA = "**camera**";
        public static final String ACTION_APP_MMS = "**mms**";
        public static final String ACTION_APP_CUSTOM = "**app**";
        public static final String ACTION_NULL = "**null**";

        String action = ACTION_NULL;
        Drawable icon;
        String customAppIntentUri;
        int index;

        public Target(int index) {
            this.index = index;
        }

        void setDrawable() {
            icon = getDrawable();
        }

        Drawable getDrawable() {
            int resId;
            Drawable drawable = null;
            PackageManager pm = getContext().getPackageManager();
            Resources res = getContext().getResources();
            if (action.equals(ACTION_UNLOCK)) {
                resId = R.drawable.ic_lockscreen_unlock;
                drawable = res.getDrawable(resId);
            } else if (action.equals(ACTION_APP_PHONE)) {
                resId = R.drawable.ic_lockscreen_phone;
                drawable = res.getDrawable(resId);
            } else if (action.equals(ACTION_APP_CAMERA)) {
                resId = R.drawable.ic_lockscreen_camera;
                drawable = res.getDrawable(resId);
            } else if (action.equals(ACTION_APP_MMS)) {
                    resId = R.drawable.ic_lockscreen_sms;
                    drawable = res.getDrawable(resId);
            } else if (action.equals(ACTION_SOUND_TOGGLE)) {
                resId = mSilentMode ? R.drawable.ic_lockscreen_silent
                        : R.drawable.ic_lockscreen_soundon;
                drawable = res.getDrawable(resId);
            } else if (action.equals(ACTION_NULL)) {
                drawable = null;
            } else if (action.equals(ACTION_APP_CUSTOM)) {
                try {
                    Intent intent = Intent.parseUri(customAppIntentUri, 0);
                    drawable = resize(pm.getActivityIcon(intent));
                } catch (PackageManager.NameNotFoundException e) {
                    Log.e(TAG, "NameNotFoundException: [" + customAppIntentUri + "]");
                } catch (URISyntaxException e) {
                    Log.e(TAG, "URISyntaxException: [" + customAppIntentUri + "]");
                }
            }
            return drawable;
        }

        void doAction() {
            if (action.equals(ACTION_UNLOCK)) {
                mCallback.goToUnlockScreen();
            } else if (action.equals(ACTION_APP_PHONE)) {
                Intent phoneIntent = new Intent(Intent.ACTION_MAIN);
                phoneIntent.setClassName("com.android.contacts",
                        "com.android.contacts.activities.DialtactsActivity");
                phoneIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(phoneIntent);
                mCallback.goToUnlockScreen();
            } else if (action.equals(ACTION_APP_MMS)) {
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_LAUNCHER);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                intent.setClassName("com.android.mms", "com.android.mms.ui.ConversationList");
                mContext.startActivity(intent);
                mCallback.goToUnlockScreen();
            } else if (action.equals(ACTION_APP_CAMERA)) {
                Intent intent = new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(intent);
                mCallback.goToUnlockScreen();
            } else if (action.equals(ACTION_SOUND_TOGGLE)) {
                toggleRingMode();
                mUnlockWidgetMethods.updateResources();

                String message = mSilentMode ?
                        getContext().getString(R.string.global_action_silent_mode_on_status)
                        : getContext().getString(R.string.global_action_silent_mode_off_status);

                final int toastIcon = mSilentMode ? R.drawable.ic_lock_ringer_off
                        : R.drawable.ic_lock_ringer_on;

                final int toastColor = mSilentMode ?
                        getContext().getResources().getColor(R.color.keyguard_text_color_soundoff)
                        : getContext().getResources().getColor(R.color.keyguard_text_color_soundon);
                toastMessage(mCarrier, message, toastColor, toastIcon);

                mCallback.pokeWakelock();
            } else if (action.equals(ACTION_APP_CUSTOM)) {
                try {
                    Intent intent = Intent.parseUri(customAppIntentUri, 0);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    mContext.startActivity(intent);
                    mCallback.goToUnlockScreen();
                } catch (URISyntaxException e) {
                    Log.e(TAG, "URISyntaxException: [" + customAppIntentUri + "]");
                }
            }
        }
    }

    class TargetController {
        ArrayList<Target> targets = new ArrayList<Target>();
        int unlockTarget = -1;
        boolean landscape = false;

        public TargetController() {
            setupTargets();
        }

        public void setLandscape(boolean isLandscape) {
            this.landscape = isLandscape;

        }

        public void setupTargets() {
            targets.clear();

            int numTargets = mLockscreenTargets;

            for (int i = 0; i < numTargets; i++) {
                String settingUri = Settings.System.getString(mContext.getContentResolver(),
                        Settings.System.LOCKSCREEN_CUSTOM_APP_ACTIVITIES[i]);
                Target t = new Target(i);
                if (settingUri == null) {
                    if (i == 0) {
                        t.action = Target.ACTION_UNLOCK;
                    } else if (numTargets == 2 && i == 1) {
                        t.action = Target.ACTION_APP_CAMERA;
                    } else if ((numTargets / 2) == (i + 1)) {
                        t.action = Target.ACTION_APP_CAMERA;
                    }
                } else {
                    if (settingUri.equals(Target.ACTION_UNLOCK)) {
                        t.action = Target.ACTION_UNLOCK;
                        unlockTarget = i;
                    } else if (settingUri.equals(Target.ACTION_APP_CAMERA)) {
                        t.action = Target.ACTION_APP_CAMERA;
                    } else if (settingUri.equals(Target.ACTION_APP_PHONE)) {
                        t.action = Target.ACTION_APP_PHONE;
                    } else if (settingUri.equals(Target.ACTION_SOUND_TOGGLE)) {
                        t.action = Target.ACTION_SOUND_TOGGLE;
                    } else if (settingUri.equals(Target.ACTION_APP_MMS)) {
                        t.action = Target.ACTION_APP_MMS;
                    } else {
                        t.action = Target.ACTION_APP_CUSTOM;
                        t.customAppIntentUri = settingUri;
                    }
                }
                t.setDrawable();
                targets.add(t);
            }

            if (unlockTarget == -1)
                if (targets.size() > 1)
                    targets.get(0).action = Target.ACTION_UNLOCK;
                else {
                    Target t = new Target(0);
                    t.action = Target.ACTION_UNLOCK;
                    targets.add(0, t);
                }
        }

        public Drawable[] getDrawables() {
            int size = targets.size();
            Drawable[] d = new Drawable[size];
            for (int i = 0; i < targets.size(); i++)
                d[i] = targets.get(i).getDrawable();

            return d;
        }

        public Target getTarget(int target) {
            return targets.get(target);
        }
    }

    TargetController targetController;

    /**
     * @param context Used to setup the view.
     * @param configuration The current configuration. Used to use when selecting layout, etc.
     * @param lockPatternUtils Used to know the state of the lock pattern settings.
     * @param updateMonitor Used to register for updates on various keyguard related state, and
     *            query the initial state at setup.
     * @param callback Used to communicate back to the host keyguard view.
     */
    LockScreen(Context context, Configuration configuration, LockPatternUtils lockPatternUtils,
            KeyguardUpdateMonitor updateMonitor,
            KeyguardScreenCallback callback) {
        super(context);
        mLockPatternUtils = lockPatternUtils;
        mUpdateMonitor = updateMonitor;
        mCallback = callback;

        mEnableMenuKeyInLockScreen = shouldEnableMenuKey();

        mCreationOrientation = configuration.orientation;

        mKeyboardHidden = configuration.hardKeyboardHidden;

        SettingsObserver settingsObserver = new SettingsObserver(new Handler());
        settingsObserver.observe();

        targetController = new TargetController();

        if (LockPatternKeyguardView.DEBUG_CONFIGURATION) {
            Log.v(TAG, "***** CREATING LOCK SCREEN", new RuntimeException());
            Log.v(TAG, "Cur orient=" + mCreationOrientation
                    + " res orient=" + context.getResources().getConfiguration().orientation);
        }

        final LayoutInflater inflater = LayoutInflater.from(context);
        if (DBG)
            Log.v(TAG, "Creation orientation = " + mCreationOrientation);

        boolean landscape = mCreationOrientation == Configuration.ORIENTATION_LANDSCAPE;

        switch (mLockscreenTargets) {
            default:
            case LAYOUT_STOCK:
            case LAYOUT_QUAD:
                if (landscape)
                    inflater.inflate(R.layout.keyguard_screen_tab_unlock_land, this,
                            true);
                else
                    inflater.inflate(R.layout.keyguard_screen_tab_unlock, this,
                            true);
                break;
            case LAYOUT_OCTO:
                if (landscape)
                    inflater.inflate(R.layout.keyguard_screen_tab_octounlock_land, this,
                            true);
                else
                    inflater.inflate(R.layout.keyguard_screen_tab_octounlock, this,
                            true);
                break;
        }

        mStatusViewManager = new KeyguardStatusViewManager(this, mUpdateMonitor, mLockPatternUtils,
                mCallback, false);

        setFocusable(true);
        setFocusableInTouchMode(true);
        setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);

        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        mSilentMode = isSilentMode();

        mCarrier = (TextView) findViewById(R.id.carrier);

        mUnlockWidget = findViewById(R.id.unlock_widget);
        if (mUnlockWidget instanceof SlidingTab) {
            SlidingTab slidingTabView = (SlidingTab) mUnlockWidget;
            slidingTabView.setHoldAfterTrigger(true, false);
            slidingTabView.setLeftHintText(R.string.lockscreen_unlock_label);
            slidingTabView.setLeftTabResources(
                    R.drawable.ic_jog_dial_unlock,
                    R.drawable.jog_tab_target_green,
                    R.drawable.jog_tab_bar_left_unlock,
                    R.drawable.jog_tab_left_unlock);
            SlidingTabMethods slidingTabMethods = new SlidingTabMethods(slidingTabView);
            slidingTabView.setOnTriggerListener(slidingTabMethods);
            mUnlockWidgetMethods = slidingTabMethods;
        } else if (mUnlockWidget instanceof WaveView) {
            WaveView waveView = (WaveView) mUnlockWidget;
            WaveViewMethods waveViewMethods = new WaveViewMethods(waveView);
            waveView.setOnTriggerListener(waveViewMethods);
            mUnlockWidgetMethods = waveViewMethods;
        } else if (mUnlockWidget instanceof MultiWaveView) {
            MultiWaveView multiWaveView = (MultiWaveView) mUnlockWidget;
            MultiWaveViewMethods multiWaveViewMethods = new MultiWaveViewMethods(multiWaveView);
            multiWaveView.setOnTriggerListener(multiWaveViewMethods);
            mUnlockWidgetMethods = multiWaveViewMethods;
        } else {
            throw new IllegalStateException("Unrecognized unlock widget: " + mUnlockWidget);
        }

        // Update widget with initial ring state
        mUnlockWidgetMethods.updateResources();

        if (DBG)
            Log.v(TAG, "*** LockScreen accel is "
                    + (mUnlockWidget.isHardwareAccelerated() ? "on" : "off"));
    }

    private boolean isSilentMode() {
        return mAudioManager.getRingerMode() != AudioManager.RINGER_MODE_NORMAL;
    }

    /**
     * Displays a message in a text view and then restores the previous text.
     * 
     * @param textView The text view.
     * @param text The text.
     * @param color The color to apply to the text, or 0 if the existing color should be used.
     * @param iconResourceId The left hand icon.
     */
    private void toastMessage(final TextView textView, final String text,
            final int color, final int iconResourceId) {
        if (mPendingR1 != null) {
            textView.removeCallbacks(mPendingR1);
            mPendingR1 = null;
        }
        if (mPendingR2 != null) {
            mPendingR2.run(); // fire immediately, restoring non-toasted appearance
            textView.removeCallbacks(mPendingR2);
            mPendingR2 = null;
        }
        final String oldText = textView.getText().toString();
        final ColorStateList oldColors = textView.getTextColors();

        mPendingR1 = new Runnable() {
            public void run() {
                textView.setText(text);
                if (color != 0) {
                    textView.setTextColor(color);
                }
                textView.setCompoundDrawablesWithIntrinsicBounds(0, iconResourceId, 0, 0);
            }
        };

        textView.postDelayed(mPendingR1, 0);
        mPendingR2 = new Runnable() {
            public void run() {
                textView.setText(oldText);
                textView.setTextColor(oldColors);
                textView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            }
        };
        textView.postDelayed(mPendingR2, 3500);
    }

    private Runnable mPendingR1;
    private Runnable mPendingR2;

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU && mEnableMenuKeyInLockScreen) {
            mCallback.goToUnlockScreen();
        }
        if (keyCode == KeyEvent.KEYCODE_POWER){
        	Log.d("TORCH","Caught onKeyDown - Power");
        	return true;
        }
        return false;
    }
   
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event){
    	if (keyCode == KeyEvent.KEYCODE_POWER){
    		quitTorch();
    		return true;
    	}
    	return false;
    } 
    
    void updateConfiguration() {
        Configuration newConfig = getResources().getConfiguration();
        if (newConfig.orientation != mCreationOrientation) {
            mCallback.recreateMe(newConfig);
        } else if (newConfig.hardKeyboardHidden != mKeyboardHidden) {
            mKeyboardHidden = newConfig.hardKeyboardHidden;
            final boolean isKeyboardOpen = mKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO;
            if (mUpdateMonitor.isKeyguardBypassEnabled() && isKeyboardOpen) {
                mCallback.goToUnlockScreen();
            }
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (LockPatternKeyguardView.DEBUG_CONFIGURATION) {
            Log.v(TAG, "***** LOCK ATTACHED TO WINDOW");
            Log.v(TAG, "Cur orient=" + mCreationOrientation
                    + ", new config=" + getResources().getConfiguration());
        }
        updateConfiguration();
    }

    /** {@inheritDoc} */
    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (LockPatternKeyguardView.DEBUG_CONFIGURATION) {
            Log.w(TAG, "***** LOCK CONFIG CHANGING", new RuntimeException());
            Log.v(TAG, "Cur orient=" + mCreationOrientation
                    + ", new config=" + newConfig);
        }
        updateConfiguration();
    }

    /** {@inheritDoc} */
    public boolean needsInput() {
        return false;
    }

    /** {@inheritDoc} */
    public void onPause() {
        mStatusViewManager.onPause();
        mUnlockWidgetMethods.reset(false);
    }

    private final Runnable mOnResumePing = new Runnable() {
        public void run() {
            mUnlockWidgetMethods.ping();
        }
    };

    /** {@inheritDoc} */
    public void onResume() {
        mStatusViewManager.onResume();
        postDelayed(mOnResumePing, ON_RESUME_PING_DELAY);
    }

    /** {@inheritDoc} */
    public void cleanUp() {
        mUpdateMonitor.removeCallback(this); // this must be first
        mLockPatternUtils = null;
        mUpdateMonitor = null;
        mCallback = null;
    }

    /** {@inheritDoc} */
    public void onRingerModeChanged(int state) {
        boolean silent = AudioManager.RINGER_MODE_NORMAL != state;
        if (silent != mSilentMode) {
            mSilentMode = silent;
            mUnlockWidgetMethods.updateResources();
        }
    }

    public void onPhoneStateChanged(String newState) {
    }

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.LOCKSCREEN_LAYOUT), false,
                    this);
            updateSettings();
        }

        @Override
        public void onChange(boolean selfChange) {
            updateSettings();
        }
    }

    private Drawable resize(Drawable image) {
        Bitmap d = ((BitmapDrawable) image).getBitmap();
        Bitmap bitmapOrig = Bitmap.createScaledBitmap(d, 55, 55, false);
        return new BitmapDrawable(getContext().getResources(), bitmapOrig);
    }

    private void launchCustomApp(String uri) {
        try {
            Intent intent = Intent.parseUri(uri, 0);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(intent);
        } catch (URISyntaxException e) {
            Log.e(TAG, "URISyntaxException: [" + uri + "]");
        }
    }

    protected void updateSettings() {
        ContentResolver resolver = mContext.getContentResolver();

        mLockscreenTargets = Settings.System.getInt(resolver,
                Settings.System.LOCKSCREEN_LAYOUT, LAYOUT_STOCK);

    }
    
    protected void startTorch(){
    	Log.d("TORCH","Starting Torch from Lockscreen");
    }
    
    protected void quitTorch(){
    	Log.d("TORCH","Quitting Torch from Lockscreen");
    }
}
