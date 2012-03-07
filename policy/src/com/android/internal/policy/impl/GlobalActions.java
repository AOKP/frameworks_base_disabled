/*
 * Copyright (C) 2012 The Android Open Source Project
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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.app.Dialog;
import android.app.Profile;
import android.app.ProfileManager;
import android.app.StatusBarManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.server.PowerSaverService;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.view.IWindowManager;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.internal.R;
import com.android.internal.app.ShutdownThread;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.TelephonyProperties;
import com.google.android.collect.Lists;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Helper to show the global actions dialog.  Each item is an {@link Action} that
 * may show depending on whether the keyguard is showing, and whether the device
 * is provisioned.
 */
class GlobalActions implements DialogInterface.OnDismissListener, DialogInterface.OnClickListener  {

    private static final String TAG = "POLICY: GlobalActions";
    private static final boolean SHOW_SILENT_TOGGLE = true;

    private MyAdapter mAdapter;
    private final Context mContext;
    private final AudioManager mAudioManager;
    private ArrayList<Action> mItems;
    private AlertDialog mDialog;

    private ToggleAction mAirplaneOn;
    private ToggleAction mPowersaverOn;
    private ToggleAction mFlashlightOn;
    private NavBarAction mHidenavbarOn;
    private SilentModeAction mSilentModeAction;

    private boolean mKeyguardShowing = false;
    private boolean mDeviceProvisioned = false;
    private boolean mIsWaitingForEcmExit = false;
    private ToggleAction.State mAirplaneState = ToggleAction.State.Off;
    
    private boolean mEnableAirplane = false;
    private boolean mEnableProfiles = true;
    private boolean mEnableEasteregg = false;
    private boolean mEnableFlashlight = true;
    private boolean mEnablePowersaver = false;
    private boolean mEnableScreenshot = true;
    private boolean mEnableHidenavbar = false;
    private boolean mReceiverRegistered = false;

    public static final String INTENT_TORCH_ON = "com.android.systemui.INTENT_TORCH_ON";
    public static final String INTENT_TORCH_OFF = "com.android.systemui.INTENT_TORCH_OFF";
    private Profile mChosenProfile;

    /**
     * @param context everything needs a context :(
     */
    public GlobalActions(Context context) {
        mContext = context;
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);

        // receive broadcasts
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(TelephonyIntents.ACTION_EMERGENCY_CALLBACK_MODE_CHANGED);
        context.registerReceiver(mBroadcastReceiver, filter);

        // get notified of phone state changes
        TelephonyManager telephonyManager =
                (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        telephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_SERVICE_STATE);
    }

    /**
     * Show the global actions dialog (creating if necessary)
     * @param keyguardShowing True if keyguard is showing
     */
    public void showDialog(boolean keyguardShowing, boolean isDeviceProvisioned) {
        mKeyguardShowing = keyguardShowing;
        mDeviceProvisioned = isDeviceProvisioned;

        if(mDialog != null) {
            mReceiverRegistered = false;
            mDialog.cancel();
        }

        //always update the PowerMenu dialog
        mDialog = createDialog();
        prepareDialog();

        mDialog.show();
        mDialog.getWindow().getDecorView().setSystemUiVisibility(View.STATUS_BAR_DISABLE_EXPAND);
    }

    /**
     * Create the global actions dialog.
     * @return A new dialog.
     */
    private AlertDialog createDialog() {

        mEnableHidenavbar = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.POWER_DIALOG_SHOW_HIDENAVBAR, 1) == 1;

        mEnableAirplane = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.POWER_DIALOG_SHOW_AIRPLANE, 0) == 1;

        mEnableEasteregg = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.POWER_DIALOG_SHOW_EASTEREGG, 0) == 1;

        mEnableFlashlight = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.POWER_DIALOG_SHOW_FLASHLIGHT, 0) == 1;

        mEnablePowersaver = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.POWER_DIALOG_SHOW_POWERSAVER, 0) == 1;

        mEnableProfiles = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.POWER_DIALOG_SHOW_PROFILES, 1) == 1;

        mEnableScreenshot = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.POWER_DIALOG_SHOW_SCREENSHOT, 1) == 1;

        mSilentModeAction = new SilentModeAction(mAudioManager, mHandler);

        mAirplaneOn = new ToggleAction(
                R.drawable.ic_lock_airplane_mode,
                R.drawable.ic_lock_airplane_mode_off,
                R.string.global_actions_toggle_airplane_mode,
                R.string.global_actions_airplane_mode_on_status,
                R.string.global_actions_airplane_mode_off_status) {

            void onToggle(boolean on) {
                if (Boolean.parseBoolean(
                        SystemProperties.get(TelephonyProperties.PROPERTY_INECM_MODE))) {
                    mIsWaitingForEcmExit = true;
                    // Launch ECM exit dialog
                    Intent ecmDialogIntent =
                            new Intent(TelephonyIntents.ACTION_SHOW_NOTICE_ECM_BLOCK_OTHERS, null);
                    ecmDialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    mContext.startActivity(ecmDialogIntent);
                } else {
                    changeAirplaneModeSystemSetting(on);
                }
            }

            @Override
            protected void changeStateFromPress(boolean buttonOn) {
                // In ECM mode airplane state cannot be changed
                if (!(Boolean.parseBoolean(
                        SystemProperties.get(TelephonyProperties.PROPERTY_INECM_MODE)))) {
                    mState = buttonOn ? State.TurningOn : State.TurningOff;
                    mAirplaneState = mState;
                }
            }

            public boolean showDuringKeyguard() {
                return true;
            }

            public boolean showBeforeProvisioning() {
                return false;
            }
        };

        mHidenavbarOn = new NavBarAction(mHandler);

        mFlashlightOn = new ToggleAction(
                R.drawable.ic_lock_torch,
                R.drawable.ic_lock_torch,
                R.string.global_actions_toggle_torch,
                R.string.global_actions_torch_on_status,
                R.string.global_actions_torch_off_status) {

            void onToggle(boolean on) {
            	if (on) {
                    Intent i = new Intent(INTENT_TORCH_ON);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    mContext.startActivity(i);
                } else {
                	Intent i = new Intent(INTENT_TORCH_OFF);
                	i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                	mContext.startActivity(i);
                }
            }

            public boolean showDuringKeyguard() {
                return true;
            }

            public boolean showBeforeProvisioning() {
                return false;
            }
        };

        mPowersaverOn = new ToggleAction(
                R.drawable.ic_lock_power_saver,
                R.drawable.ic_lock_power_saver,
                R.string.global_actions_toggle_power_saver,
                R.string.global_actions_power_saver_on_status,
                R.string.global_actions_power_saver_off_status) {

            void onToggle(boolean on) {
                Settings.Secure.putInt(mContext.getContentResolver(),
                        Settings.Secure.POWER_SAVER_MODE,
                         on ? PowerSaverService.POWER_SAVER_MODE_ON
                                : PowerSaverService.POWER_SAVER_MODE_OFF);
            }

            public boolean showDuringKeyguard() {
                return true;
            }

            public boolean showBeforeProvisioning() {
                return false;
            }
        };
        
        mItems = new ArrayList<Action>();

        // next: hidenavbar
        if(mEnableHidenavbar) {
            Log.d(TAG, "Adding Hidenavbar Menu");
            mItems.add(mHidenavbarOn); 
        } else {
            Log.d(TAG, "Not Adding Hidenavbar");
        }

        // first: airplane
        if(mEnableAirplane) {
            Log.d(TAG, "Adding Airplane Menu");
            mItems.add(mAirplaneOn);
        } else {
            Log.d(TAG, "Not Adding Airplane");
        }

        // next: easteregg
        if (mEnableEasteregg) {
            Log.d(TAG, "Adding Easteregg Menu");
            mItems.add(new SinglePressAction(com.android.internal.R.drawable.ic_lock_nyandroid,
                    R.string.global_action_easter_egg) {
                public void onPress() {
                    try {
                        mContext.startActivity(new Intent(Intent.ACTION_MAIN)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TASK
                            | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                        .setClassName("com.android.systemui","com.android.systemui.Nyandroid"));
                    } catch (ActivityNotFoundException ex) {
                    }
                }

                public boolean showDuringKeyguard() {
                    return true;
                }

                public boolean showBeforeProvisioning() {
                    return true;
                }
            });
        } else {
            Log.d(TAG, "Not Adding Easteregg");
        }

        // next: flashlight
        if(mEnableFlashlight) {
            Log.d(TAG, "Adding Flashlight Menu");
            mItems.add(mFlashlightOn); 
        } else {
            Log.d(TAG, "Not Adding Flashlight");
        }

        // next: powersaver
        try {
            Settings.Secure.getInt(mContext.getContentResolver(),
                    Settings.Secure.POWER_SAVER_MODE);
            if(mEnablePowersaver) {
                Log.d(TAG, "Adding Powersaver Menu");
                mItems.add(mPowersaverOn); 
            } else {
                Log.d(TAG, "Not Adding Powersaver");
            }
        } catch (SettingNotFoundException e) {
        }

        // next: power off
        mItems.add(
            new SinglePressAction(
                    com.android.internal.R.drawable.ic_lock_power_off,
                    R.string.global_action_power_off) {
                public void onPress() {
                    // shutdown by making sure radio and power are handled accordingly.
                    ShutdownThread.shutdown(mContext, true);
                }

                public boolean showDuringKeyguard() {
                    return true;
                }

                public boolean showBeforeProvisioning() {
                    return true;
                }
            });

        // next: reboot
        mItems.add(
            new SinglePressAction(
                    com.android.internal.R.drawable.ic_lock_reboot,
                    R.string.global_action_reboot) {
                public void onPress() {
                    ShutdownThread.reboot(mContext, "null", true);
                }

                public boolean showDuringKeyguard() {
                    return true;
                }

                public boolean showBeforeProvisioning() {
                    return true;
                }
            });

        // next: profiles
        if (mEnableProfiles) {
            Log.d(TAG, "Adding Profiles Menu");
            mItems.add(
                new ProfileChooseAction() {
                public void onPress() {
                    createProfileDialog();
                }

                public boolean showDuringKeyguard() {
                    return false;
                }

                public boolean showBeforeProvisioning() {
                    return false;
                }
            });
        } else {
            Log.d(TAG, "Not Adding Profiles");
        }

        // next: screenshot
        if (mEnableScreenshot) {
            Log.d(TAG, "Adding Screenshot Menu");
            mItems.add(
                new SinglePressAction(
                        com.android.internal.R.drawable.ic_lock_screenshot,
                        R.string.global_action_screenshot) {
                public void onPress() {
                    takeScreenshot();
                }

                public boolean showDuringKeyguard() {
                    return true;
                }

                public boolean showBeforeProvisioning() {
                    return true;
                }
            });
        } else {
            Log.d(TAG, "Not Adding Screenshot");
        }

        // last: silent mode
        if (SHOW_SILENT_TOGGLE) {
            mItems.add(mSilentModeAction);
        }

        mAdapter = new MyAdapter();
        final AlertDialog.Builder ab = new AlertDialog.Builder(mContext);
        ab.setAdapter(mAdapter, this).setInverseBackgroundForced(true);
        final AlertDialog dialog = ab.create();

        dialog.getListView().setItemsCanFocus(true);
        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_DIALOG);
        dialog.setOnDismissListener(this);
        return dialog;
    }

    private void createProfileDialog(){
        final ProfileManager profileManager = (ProfileManager)mContext.getSystemService(Context.PROFILE_SERVICE);
        final Profile[] profiles = profileManager.getProfiles();
        UUID activeProfile = profileManager.getActiveProfile().getUuid();
        final CharSequence[] names = new CharSequence[profiles.length];

        int i=0;
        int checkedItem = 0;

        for(Profile profile : profiles) {
            if(profile.getUuid().equals(activeProfile)) {
                checkedItem = i;
                mChosenProfile = profile;
            }
            names[i++] = profile.getName();
        }

        final AlertDialog.Builder ab = new AlertDialog.Builder(mContext);

        AlertDialog dialog = ab
                .setTitle(R.string.global_action_choose_profile)
                .setSingleChoiceItems(names, checkedItem, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (which < 0)
                            return;
                        mChosenProfile = profiles[which];
                    }
                })
                .setPositiveButton(com.android.internal.R.string.yes,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                profileManager.setActiveProfile(mChosenProfile.getUuid());
                            }
                        })
                .setNegativeButton(com.android.internal.R.string.no,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                }).create();
        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_DIALOG);
        if (!mContext.getResources().getBoolean(com.android.internal.R.bool.config_sf_slowBlur)) {
            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
        }
        dialog.show();
    }

    /**
     * functions needed for taking screenhots.  
     * This leverages the built in ICS screenshot functionality 
     */
    final Object mScreenshotLock = new Object();
    ServiceConnection mScreenshotConnection = null;

    final Runnable mScreenshotTimeout = new Runnable() {
        @Override public void run() {
            synchronized (mScreenshotLock) {
                if (mScreenshotConnection != null) {
                    mContext.unbindService(mScreenshotConnection);
                    mScreenshotConnection = null;
                }
            }
        }
    };

    private void takeScreenshot() {
        synchronized (mScreenshotLock) {
            if (mScreenshotConnection != null) {
                return;
            }
            ComponentName cn = new ComponentName("com.android.systemui",
                    "com.android.systemui.screenshot.TakeScreenshotService");
            Intent intent = new Intent();
            intent.setComponent(cn);
            ServiceConnection conn = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    synchronized (mScreenshotLock) {
                        if (mScreenshotConnection != this) {
                            return;
                        }
                        Messenger messenger = new Messenger(service);
                        Message msg = Message.obtain(null, 1);
                        final ServiceConnection myConn = this;
                        Handler h = new Handler(mHandler.getLooper()) {
                            @Override
                            public void handleMessage(Message msg) {
                                synchronized (mScreenshotLock) {
                                    if (mScreenshotConnection == myConn) {
                                        mContext.unbindService(mScreenshotConnection);
                                        mScreenshotConnection = null;
                                        mHandler.removeCallbacks(mScreenshotTimeout);
                                    }
                                }
                            }
                        };
                        msg.replyTo = new Messenger(h);
                        msg.arg1 = msg.arg2 = 0;                   

                        /* wait for the dialog box to close */
                        try {
                            Thread.sleep(1000); 
                        } catch (InterruptedException ie) {
                        }
                        
                        /* take the screenshot */
                        try {
                            messenger.send(msg);
                        } catch (RemoteException e) {
                        }
                    }
                }
                @Override
                public void onServiceDisconnected(ComponentName name) {}
            };
            if (mContext.bindService(intent, conn, Context.BIND_AUTO_CREATE)) {
                mScreenshotConnection = conn;
                mHandler.postDelayed(mScreenshotTimeout, 10000);
            }
        }
    }

    private void prepareDialog() {
        final boolean silentModeOn =
                mAudioManager.getRingerMode() != AudioManager.RINGER_MODE_NORMAL;
        mAirplaneOn.updateState(mAirplaneState);
        mAdapter.notifyDataSetChanged();

        if (mKeyguardShowing) {
            mDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
        } else {
            mDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_DIALOG);
        }

        if (SHOW_SILENT_TOGGLE) {
            IntentFilter filter = new IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION);
            mContext.registerReceiver(mRingerModeReceiver, filter);
            mReceiverRegistered = true;
        }
        final boolean powerSaverOn = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.POWER_SAVER_MODE, PowerSaverService.POWER_SAVER_MODE_OFF) == PowerSaverService.POWER_SAVER_MODE_ON;
        mPowersaverOn.updateState(powerSaverOn ? ToggleAction.State.On : ToggleAction.State.Off);
    }

    /** {@inheritDoc} */
    public void onDismiss(DialogInterface dialog) {
        if (SHOW_SILENT_TOGGLE) {
            if(mReceiverRegistered) {
                mContext.unregisterReceiver(mRingerModeReceiver);
                mReceiverRegistered = false;
            }
        }
    }

    /** {@inheritDoc} */
    public void onClick(DialogInterface dialog, int which) {
        if (!(mAdapter.getItem(which) instanceof SilentModeAction)) {
            dialog.dismiss();
        }
        mAdapter.getItem(which).onPress();
    }

    /**
     * The adapter used for the list within the global actions dialog, taking
     * into account whether the keyguard is showing via
     * {@link GlobalActions#mKeyguardShowing} and whether the device is provisioned
     * via {@link GlobalActions#mDeviceProvisioned}.
     */
    private class MyAdapter extends BaseAdapter {

        public int getCount() {
            int count = 0;

            for (int i = 0; i < mItems.size(); i++) {
                final Action action = mItems.get(i);

                if (mKeyguardShowing && !action.showDuringKeyguard()) {
                    continue;
                }
                if (!mDeviceProvisioned && !action.showBeforeProvisioning()) {
                    continue;
                }
                count++;
            }
            return count;
        }

        @Override
        public boolean isEnabled(int position) {
            return getItem(position).isEnabled();
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        public Action getItem(int position) {

            int filteredPos = 0;
            for (int i = 0; i < mItems.size(); i++) {
                final Action action = mItems.get(i);
                if (mKeyguardShowing && !action.showDuringKeyguard()) {
                    continue;
                }
                if (!mDeviceProvisioned && !action.showBeforeProvisioning()) {
                    continue;
                }
                if (filteredPos == position) {
                    return action;
                }
                filteredPos++;
            }

            throw new IllegalArgumentException("position " + position
                    + " out of range of showable actions"
                    + ", filtered count=" + getCount()
                    + ", keyguardshowing=" + mKeyguardShowing
                    + ", provisioned=" + mDeviceProvisioned);
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            Action action = getItem(position);
            return action.create(mContext, convertView, parent, LayoutInflater.from(mContext));
        }
    }

    // note: the scheme below made more sense when we were planning on having
    // 8 different things in the global actions dialog.  seems overkill with
    // only 3 items now, but may as well keep this flexible approach so it will
    // be easy should someone decide at the last minute to include something
    // else, such as 'enable wifi', or 'enable bluetooth'

    /**
     * What each item in the global actions dialog must be able to support.
     */
    private interface Action {
        View create(Context context, View convertView, ViewGroup parent, LayoutInflater inflater);

        void onPress();

        /**
         * @return whether this action should appear in the dialog when the keygaurd
         *    is showing.
         */
        boolean showDuringKeyguard();

        /**
         * @return whether this action should appear in the dialog before the
         *   device is provisioned.
         */
        boolean showBeforeProvisioning();

        boolean isEnabled();
    }

    /**
     * A single press action maintains no state, just responds to a press
     * and takes an action.
     */
    private static abstract class SinglePressAction implements Action {
        private final int mIconResId;
        private final int mMessageResId;

        protected SinglePressAction(int iconResId, int messageResId) {
            mIconResId = iconResId;
            mMessageResId = messageResId;
        }

        public boolean isEnabled() {
            return true;
        }

        abstract public void onPress();

        public View create(
                Context context, View convertView, ViewGroup parent, LayoutInflater inflater) {
            View v = inflater.inflate(R.layout.global_actions_item, parent, false);
            ImageView icon = (ImageView) v.findViewById(R.id.icon);
            TextView messageView = (TextView) v.findViewById(R.id.message);
            v.findViewById(R.id.status).setVisibility(View.GONE);
            icon.setImageDrawable(context.getResources().getDrawable(mIconResId));
            messageView.setText(mMessageResId);
            return v;
        }
    }

    /**
     * A single press action maintains no state, just responds to a press
     * and takes an action.
     */
    private abstract class ProfileChooseAction implements Action {
        private ProfileManager mProfileManager;

        protected ProfileChooseAction() {
            mProfileManager = (ProfileManager)mContext.getSystemService(Context.PROFILE_SERVICE);
        }

        public boolean isEnabled() {
            return true;
        }

        abstract public void onPress();

        public View create(
                Context context, View convertView, ViewGroup parent, LayoutInflater inflater) {
            View v = (convertView != null) ?
                    convertView :
                    inflater.inflate(R.layout.global_actions_item, parent, false);

            ImageView icon = (ImageView) v.findViewById(R.id.icon);
            TextView messageView = (TextView) v.findViewById(R.id.message);
            TextView statusView = (TextView) v.findViewById(R.id.status);
            statusView.setVisibility(View.VISIBLE);
            statusView.setText(mProfileManager.getActiveProfile().getName());
            icon.setImageDrawable(context.getResources().getDrawable(com.android.internal.R.drawable.ic_lock_profile));
            messageView.setText(R.string.global_action_choose_profile);
            return v;
        }
    }

    /**
     * A toggle action knows whether it is on or off, and displays an icon
     * and status message accordingly.
     */
    private static abstract class ToggleAction implements Action {

        enum State {
            Off(false),
            TurningOn(true),
            TurningOff(true),
            On(false);

            private final boolean inTransition;

            State(boolean intermediate) {
                inTransition = intermediate;
            }

            public boolean inTransition() {
                return inTransition;
            }
        }

        protected State mState = State.Off;

        // prefs
        protected int mEnabledIconResId;
        protected int mDisabledIconResid;
        protected int mMessageResId;
        protected int mEnabledStatusMessageResId;
        protected int mDisabledStatusMessageResId;

        /**
         * @param enabledIconResId The icon for when this action is on.
         * @param disabledIconResid The icon for when this action is off.
         * @param essage The general information message, e.g 'Silent Mode'
         * @param enabledStatusMessageResId The on status message, e.g 'sound disabled'
         * @param disabledStatusMessageResId The off status message, e.g. 'sound enabled'
         */
        public ToggleAction(int enabledIconResId,
                int disabledIconResid,
                int essage,
                int enabledStatusMessageResId,
                int disabledStatusMessageResId) {
            mEnabledIconResId = enabledIconResId;
            mDisabledIconResid = disabledIconResid;
            mMessageResId = essage;
            mEnabledStatusMessageResId = enabledStatusMessageResId;
            mDisabledStatusMessageResId = disabledStatusMessageResId;
        }

        /**
         * Override to make changes to resource IDs just before creating the
         * View.
         */
        void willCreate() {
        }

        public View create(Context context, View convertView, ViewGroup parent,
                LayoutInflater inflater) {
            willCreate();

            View v = inflater.inflate(R
                            .layout.global_actions_item, parent, false);
            ImageView icon = (ImageView) v.findViewById(R.id.icon);
            TextView messageView = (TextView) v.findViewById(R.id.message);
            TextView statusView = (TextView) v.findViewById(R.id.status);
            final boolean enabled = isEnabled();

            if (messageView != null) {
                messageView.setText(mMessageResId);
                messageView.setEnabled(enabled);
            }

            boolean on = ((mState == State.On) || (mState == State.TurningOn));
            if (icon != null) {
                icon.setImageDrawable(context.getResources().getDrawable(
                        (on ? mEnabledIconResId : mDisabledIconResid)));
                icon.setEnabled(enabled);
            }

            if (statusView != null) {
                statusView.setText(on ? mEnabledStatusMessageResId : mDisabledStatusMessageResId);
                statusView.setVisibility(View.VISIBLE);
                statusView.setEnabled(enabled);
            }
            v.setEnabled(enabled);
            return v;
        }

        public final void onPress() {
            if (mState.inTransition()) {
                Log.w(TAG, "shouldn't be able to toggle when in transition");
                return;
            }
            final boolean nowOn = !(mState == State.On);
            onToggle(nowOn);
            changeStateFromPress(nowOn);
        }

        public boolean isEnabled() {
            return !mState.inTransition();
        }

        /**
         * Implementations may override this if their state can be in on of the intermediate
         * states until some notification is received (e.g airplane mode is 'turning off' until
         * we know the wireless connections are back online
         * @param buttonOn Whether the button was turned on or off
         */
        protected void changeStateFromPress(boolean buttonOn) {
            mState = buttonOn ? State.On : State.Off;
        }

        abstract void onToggle(boolean on);

        public void updateState(State state) {
            mState = state;
        }
    }

    private static class SilentModeAction implements Action, View.OnClickListener {

        private final int[] ITEM_IDS = { R.id.option1, R.id.option2, R.id.option3 };
        private final AudioManager mAudioManager;
        private final Handler mHandler;

        SilentModeAction(AudioManager audioManager, Handler handler) {
            mAudioManager = audioManager;
            mHandler = handler;
        }

        private int ringerModeToIndex(int ringerMode) {
            // They just happen to coincide
            return ringerMode;
        }

        private int indexToRingerMode(int index) {
            // They just happen to coincide
            return index;
        }

        public View create(Context context, View convertView, ViewGroup parent,
                LayoutInflater inflater) {
            View v = inflater.inflate(R.layout.global_actions_silent_mode, parent, false);

            int selectedIndex = ringerModeToIndex(mAudioManager.getRingerMode());
            for (int i = 0; i < 3; i++) {
                View itemView = v.findViewById(ITEM_IDS[i]);
                itemView.setSelected(selectedIndex == i);
                // Set up click handler
                itemView.setTag(i);
                itemView.setOnClickListener(this);
            }
            return v;
        }

        public void onPress() {
        }

        public boolean showDuringKeyguard() {
            return true;
        }

        public boolean showBeforeProvisioning() {
            return false;
        }

        public boolean isEnabled() {
            return true;
        }

        void willCreate() {
        }

        public void onClick(View v) {
            if (!(v.getTag() instanceof Integer)) return;
            int index = (Integer) v.getTag();
            mAudioManager.setRingerMode(indexToRingerMode(index));
            mHandler.sendEmptyMessageDelayed(MESSAGE_DISMISS, DIALOG_DISMISS_DELAY);
        }
    }

    private static class NavBarAction implements Action, View.OnClickListener {

        private final int[] ITEM_IDS = { R.id.navbartoggle, R.id.navbarhome, R.id.navbarback,R.id.navbarmenu };
        
        public Context mContext;
        private int mInjectKeycode;
        public boolean mNavBarVisible;
        private final Handler mHandler;
        private IWindowManager mWindowManager;
        
        NavBarAction(Handler handler) {
        	mHandler = handler;
        }

        public View create(Context context, View convertView, ViewGroup parent,
                LayoutInflater inflater) {
        	mContext = context;
        	mNavBarVisible = Settings.System.getInt(mContext.getContentResolver(),
                    Settings.System.NAVIGATION_BAR_BUTTONS_SHOW, 1) == 1;
        	mWindowManager = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));
            View v = inflater.inflate(R.layout.global_actions_navbar_mode, parent, false);

            for (int i = 0; i < 4; i++) {
                View itemView = v.findViewById(ITEM_IDS[i]);
                itemView.setSelected((i==0)&&mNavBarVisible);
                itemView.setTag(i);
                itemView.setOnClickListener(this);
            }
            return v;
        }

        public void onPress() {
        }

        public boolean showDuringKeyguard() {
            return true;
        }

        public boolean showBeforeProvisioning() {
            return false;
        }

        public boolean isEnabled() {
            return true;
        }

        void willCreate() {
        }

        public void onClick(View v) {
            if (!(v.getTag() instanceof Integer)) return;
            int index = (Integer) v.getTag();
            
            switch (index) {
            
            case 0 :
                Settings.System.putInt(mContext.getContentResolver(),
                        Settings.System.NAVIGATION_BAR_BUTTONS_SHOW,
                         mNavBarVisible ? 0 : 1);
                v.setSelected(!mNavBarVisible);
                mHandler.sendEmptyMessage(MESSAGE_DISMISS);
                break;
            case 1:
            	 injectKeyDelayed(KeyEvent.KEYCODE_HOME);
            	 mHandler.sendEmptyMessage(MESSAGE_DISMISS);
            	break;
            case 2:    	
            	injectKeyDelayed(KeyEvent.KEYCODE_BACK);
            	mHandler.sendEmptyMessage(MESSAGE_DISMISS);
            	break;
            case 3:    	
            	injectKeyDelayed(KeyEvent.KEYCODE_MENU);
            	mHandler.sendEmptyMessage(MESSAGE_DISMISS);
            	break;	    
            }  
        }
     
        public void injectKeyDelayed(int keycode){
        	mInjectKeycode = keycode;
        	mHandler.removeCallbacks(onInjectKeyDelayed);
        	mHandler.postDelayed(onInjectKeyDelayed, 50);
        }

        final Runnable onInjectKeyDelayed = new Runnable() {
        	public void run() {
        		try {
        			mWindowManager.injectKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, mInjectKeycode), true);
        			mWindowManager.injectKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, mInjectKeycode), true);
        		} catch (RemoteException e) {
        			e.printStackTrace();
        		}
        	}
        };
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_CLOSE_SYSTEM_DIALOGS.equals(action)
                    || Intent.ACTION_SCREEN_OFF.equals(action)) {
                String reason = intent.getStringExtra(PhoneWindowManager.SYSTEM_DIALOG_REASON_KEY);
                if (!PhoneWindowManager.SYSTEM_DIALOG_REASON_GLOBAL_ACTIONS.equals(reason)) {
                    mHandler.sendEmptyMessage(MESSAGE_DISMISS);
                }
            } else if (TelephonyIntents.ACTION_EMERGENCY_CALLBACK_MODE_CHANGED.equals(action)) {
                if (!(intent.getBooleanExtra("PHONE_IN_ECM_STATE", false)) &&
                        mIsWaitingForEcmExit) {
                    mIsWaitingForEcmExit = false;
                    changeAirplaneModeSystemSetting(true);
                }
            }
        }
    };

    PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onServiceStateChanged(ServiceState serviceState) {
            final boolean inAirplaneMode = serviceState.getState() == ServiceState.STATE_POWER_OFF;
            mAirplaneState = inAirplaneMode ? ToggleAction.State.On : ToggleAction.State.Off;
            mAirplaneOn.updateState(mAirplaneState);
            mAdapter.notifyDataSetChanged();
        }
    };

    private BroadcastReceiver mRingerModeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(AudioManager.RINGER_MODE_CHANGED_ACTION)) {
                mHandler.sendEmptyMessage(MESSAGE_REFRESH);
            }
        }
    };

    private static final int MESSAGE_DISMISS = 0;
    private static final int MESSAGE_REFRESH = 1;
    private static final int DIALOG_DISMISS_DELAY = 50; // ms

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == MESSAGE_DISMISS) {
                if (mDialog != null) {
                    mDialog.dismiss();
                }
            } else if (msg.what == MESSAGE_REFRESH) {
                mAdapter.notifyDataSetChanged();
            }
        }
    };

    /**
     * Change the airplane mode system setting
     */
    private void changeAirplaneModeSystemSetting(boolean on) {
        Settings.System.putInt(
                mContext.getContentResolver(),
                Settings.System.AIRPLANE_MODE_ON,
                on ? 1 : 0);
        Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        intent.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
        intent.putExtra("state", on);
        mContext.sendBroadcast(intent);
    }
}

