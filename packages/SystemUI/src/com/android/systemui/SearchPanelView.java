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

package com.android.systemui;

import android.animation.LayoutTransition;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.SearchManager;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.res.Configuration;
import android.content.Context;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.res.Resources;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ActivityInfo;
import android.content.pm.ActivityInfo;
import android.content.ServiceConnection;
import android.database.ContentObserver;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Vibrator;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.os.PowerManager;
import android.os.Process;
import android.os.ServiceManager;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Slog;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnPreDrawListener;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.android.internal.widget.multiwaveview.GlowPadView;
import com.android.internal.widget.multiwaveview.GlowPadView.OnTriggerListener;
import com.android.internal.widget.multiwaveview.TargetDrawable;
import com.android.systemui.R;
import com.android.systemui.recent.StatusBarTouchProxy;
import com.android.systemui.statusbar.BaseStatusBar;
import com.android.systemui.statusbar.CommandQueue;
import com.android.systemui.statusbar.phone.PhoneStatusBar;
import com.android.systemui.statusbar.tablet.StatusBarPanel;
import com.android.systemui.statusbar.tablet.TabletStatusBar;
import com.android.internal.widget.multiwaveview.TargetDrawable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.net.URISyntaxException;


public class SearchPanelView extends FrameLayout implements
        StatusBarPanel, ActivityOptions.OnAnimationStartedListener {
    private static final int SEARCH_PANEL_HOLD_DURATION = 0;
    static final String TAG = "SearchPanelView";
    static final boolean DEBUG = TabletStatusBar.DEBUG || PhoneStatusBar.DEBUG || false;
    private static final String ASSIST_ICON_METADATA_NAME =
            "com.android.systemui.action_assist_icon";
    private final Context mContext;
    private BaseStatusBar mBar;
    private StatusBarTouchProxy mStatusBarTouchProxy;

    private boolean mShowing;
    private View mSearchTargetsContainer;
    private GlowPadView mGlowPadView;

    private PackageManager mPackageManager;
    private Resources mResources;
    private TargetObserver mTargetObserver;
    private ContentResolver mContentResolver;
    private List<String> targetList;
    private int startPosOffset;

    private int mNavRingAmount;
    private boolean mTabletui;
    private boolean mLefty;

    //need to make an intent list and an intent counter
    String[] intent;
    ArrayList<String> intentList = new ArrayList<String>();
    String mEmpty = "assist";

    public SearchPanelView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SearchPanelView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        mPackageManager = mContext.getPackageManager();
        mResources = mContext.getResources();

        mContentResolver = mContext.getContentResolver();
        mTargetObserver = new TargetObserver(new Handler());

        mTabletui = Settings.System.getBoolean(mContext.getContentResolver(),
                        Settings.System.MODE_TABLET_UI, false);

        mLefty = (Settings.System.getBoolean(mContext.getContentResolver(),
                Settings.System.NAVIGATION_BAR_LEFTY_MODE, false));

        mNavRingAmount = Settings.System.getInt(mContext.getContentResolver(),
                         Settings.System.SYSTEMUI_NAVRING_AMOUNT, 1);

        targetList = Arrays.asList(Settings.System.SYSTEMUI_NAVRING_1, Settings.System.SYSTEMUI_NAVRING_2,
                                   Settings.System.SYSTEMUI_NAVRING_3, Settings.System.SYSTEMUI_NAVRING_4,
                                   Settings.System.SYSTEMUI_NAVRING_5);

        for (int i = 0; i < targetList.size(); i++) {
            mContentResolver.registerContentObserver(Settings.System.getUriFor(targetList.get(i)), false, mTargetObserver);
        }
    }

    private void startAssistActivity() {
        // Close Recent Apps if needed
        mBar.animateCollapse(CommandQueue.FLAG_EXCLUDE_SEARCH_PANEL);
        // Launch Assist
        Intent intent = SearchManager.getAssistIntent(mContext);
        if (intent == null) return;
        try {
            ActivityOptions opts = ActivityOptions.makeCustomAnimation(mContext,
                    R.anim.search_launch_enter, R.anim.search_launch_exit,
                    getHandler(), this);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(intent, opts.toBundle());
        } catch (ActivityNotFoundException e) {
            Slog.w(TAG, "Activity not found for " + intent.getAction());
            onAnimationStarted();
        }
    }

    private boolean launchTarget(int target) {
        String targetKey;
        Intent intent = SearchManager.getAssistIntent(mContext);

        if (target < intentList.size()) {
              targetKey = intentList.get(target);
        } else {
            return false;
        }

        if (targetKey == null || targetKey.equals("")) {
            return false;
        }

        if (targetKey.equals("screenoff")) {
            vibrate();
            screenOff();
            return true;
        }

        if (targetKey.equals("ime_switcher")) {
            vibrate();
            getContext().sendBroadcast(new Intent("android.settings.SHOW_INPUT_METHOD_PICKER"));
            return true;
        }

        if (targetKey.equals("assist")) {
            vibrate();
            startAssistActivity();
            return true;
        }

        if (targetKey.equals("ring_vib")) {
            AudioManager am = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
            if(am != null){
                if(am.getRingerMode() != AudioManager.RINGER_MODE_VIBRATE) {
                    am.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
                    Vibrator vib = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
                    if(vib != null){
                        vib.vibrate(50);
                    }
                }else{
                    am.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                    ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, (int)(ToneGenerator.MAX_VOLUME * 0.85));
                    if(tg != null){
                        tg.startTone(ToneGenerator.TONE_PROP_BEEP);
                    }
                }
            }
            return true;
        }
        if (targetKey.equals("ring_silent")) {
            AudioManager am = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
            if(am != null){
                if(am.getRingerMode() != AudioManager.RINGER_MODE_SILENT) {
                    am.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                }else{
                    am.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                    ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, (int)(ToneGenerator.MAX_VOLUME * 0.85));
                    if(tg != null){
                        tg.startTone(ToneGenerator.TONE_PROP_BEEP);
                    }
                }
            }
            return true;
        }
        if (targetKey.equals("ring_vib_silent")) {
            AudioManager am = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
            if(am != null){
                if(am.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) {
                    am.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
                    Vibrator vib = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
                    if(vib != null){
                        vib.vibrate(50);
                    }
                } else if(am.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE) {
                    am.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                } else {
                    am.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                    ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, (int)(ToneGenerator.MAX_VOLUME * 0.85));
                    if(tg != null){
                        tg.startTone(ToneGenerator.TONE_PROP_BEEP);
                    }
                }
            }
            return true;
        }

        if (targetKey.equals("killcurrent")) {
            vibrate();
            killProcess();
            return true;
        }

        if (targetKey.equals("screenshot")) {
            vibrate();
            takeScreenshot();
            return true;
        }

        if (targetKey.equals("power")) {
            vibrate();
            powerMenu();
            return true;
        }


            /* Try to launch the activity from history, if available.*/
            ActivityManager activityManager = (ActivityManager) mContext
                    .getSystemService(Context.ACTIVITY_SERVICE);
            for (ActivityManager.RecentTaskInfo task : activityManager.getRecentTasks(20,
                    ActivityManager.RECENT_IGNORE_UNAVAILABLE)) {
                if (task != null && task.origActivity != null &&
                        task.origActivity.equals(targetKey)) {
                        if (task.id > 0) {
                           activityManager.moveTaskToFront(task.id, ActivityManager.MOVE_TASK_WITH_HOME);
                    return true;
                    }
                }
            }

            vibrate();
            try {
                intent = Intent.parseUri(targetKey, 0);
                } catch (URISyntaxException e) {
                }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(intent);
            return true;
    }

    class GlowPadTriggerListener implements GlowPadView.OnTriggerListener {
        boolean mWaitingForLaunch;

        public void onGrabbed(View v, int handle) {
        }

        public void onReleased(View v, int handle) {
        }

        public void onGrabbedStateChange(View v, int handle) {
            if (!mWaitingForLaunch && OnTriggerListener.NO_HANDLE == handle) {
                mBar.hideSearchPanel();
            }
        }

        public void onTrigger(View v, final int target) {
            final int resId = mGlowPadView.getResourceIdForTarget(target);

            boolean launch = launchTarget(target);

            switch (resId) {
                case com.android.internal.R.drawable.ic_action_assist_generic:
                    mWaitingForLaunch = true;
                    startAssistActivity();
                    vibrate();
                    break;
            }
        }

        public void onFinishFinalAnimation() {
        }
    }
    final GlowPadTriggerListener mGlowPadViewListener = new GlowPadTriggerListener();

    @Override
    public void onAnimationStarted() {
        postDelayed(new Runnable() {
            public void run() {
                mGlowPadViewListener.mWaitingForLaunch = false;
                mBar.hideSearchPanel();
            }
        }, SEARCH_PANEL_HOLD_DURATION);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mSearchTargetsContainer = findViewById(R.id.search_panel_container);
        mStatusBarTouchProxy = (StatusBarTouchProxy) findViewById(R.id.status_bar_touch_proxy);
        // TODO: fetch views
        mGlowPadView = (GlowPadView) findViewById(R.id.glow_pad_view);
        mGlowPadView.setOnTriggerListener(mGlowPadViewListener);

        setDrawables();
    }

    private void setDrawables() {
        String target3 = Settings.System.getString(mContext.getContentResolver(), Settings.System.SYSTEMUI_NAVRING_3);
        if (target3 == null || target3.equals("")) {
            Settings.System.putString(mContext.getContentResolver(), Settings.System.SYSTEMUI_NAVRING_3, "assist");
        }

        // Custom Targets
        ArrayList<TargetDrawable> storedDraw = new ArrayList<TargetDrawable>();

        int endPosOffset;
        int middleBlanks = 0;

       // lets try this for all devices in TabletUI
        if (mTabletui) {

            if (mLefty) { // either lefty or... (Ring is actually on right side of screen
                startPosOffset =  (mNavRingAmount) + 1;
                endPosOffset =  (mNavRingAmount *2) + 1;
            } else { // righty... (Ring actually on left side of tablet)
                    startPosOffset =  1;
                    endPosOffset = (mNavRingAmount * 3) + 1;
                    // this creates a 'quadrant' where NavRingQtry is 1/4 of the total (plus a spacer on either side)
            }
         // this is for nexus 7 type devices not in TabletUI since the navbar stays at bottom, or phones in Portrait
         } else if (screenLayout() == Configuration.SCREENLAYOUT_SIZE_LARGE || isScreenPortrait()) {
             startPosOffset =  1;
             endPosOffset =  (mNavRingAmount) + 1;
         } else {
            // next is landscape for lefty navbar is on left
                if (mLefty) {
                    startPosOffset =  1 - (mNavRingAmount % 2);
                    middleBlanks = mNavRingAmount + 2;
                    endPosOffset = 0;

                } else {
                //lastly the standard landscape with navbar on right
                    startPosOffset =  (Math.min(1,mNavRingAmount / 2)) + 2;
                    endPosOffset =  startPosOffset - 1;
                }
        }



         int middleStart = mNavRingAmount;
         int tqty = middleStart;
         int middleFinish = 0;

         if (middleBlanks > 0) {
             middleStart = (tqty/2) + (tqty%2);
             middleFinish = (tqty/2);
         }

         List<String> targetActivities = Arrays.asList(
                 Settings.System.getString(mContext.getContentResolver(), targetList.get(0)),
                 Settings.System.getString(mContext.getContentResolver(), targetList.get(1)),
                 Settings.System.getString(mContext.getContentResolver(), targetList.get(2)),
                 Settings.System.getString(mContext.getContentResolver(), targetList.get(3)),
                 Settings.System.getString(mContext.getContentResolver(), targetList.get(4)));

        intentList.clear();

         // Add Initial Place Holder Targets
        for (int i = 0; i < startPosOffset; i++) {
            storedDraw.add(getTargetDrawable(""));
            intentList.add(mEmpty);
        }
        // Add User Targets
        for (int i = 0; i < middleStart; i++) {
            intentList.add(targetActivities.get(i));
            storedDraw.add(getTargetDrawable(targetActivities.get(i)));
        }

        // Add middle Place Holder Targets
        for (int j = 0; j < middleBlanks; j++) {
            storedDraw.add(getTargetDrawable(""));
            intentList.add(mEmpty);
        }

        // Add Rest of User Targets for leftys
        for (int j = 0; j < middleFinish; j++) {
            int i = j + middleStart;
            intentList.add(targetActivities.get(i));
            storedDraw.add(getTargetDrawable(targetActivities.get(i)));
        }

        // Add End Place Holder Targets
        for (int i = 0; i < endPosOffset; i++) {
            storedDraw.add(getTargetDrawable(""));
            intentList.add(mEmpty);
        }

        mGlowPadView.setTargetResources(storedDraw);
    }

    private TargetDrawable getTargetDrawable (String action){

        TargetDrawable cDrawable = new TargetDrawable(mResources, mResources.getDrawable(com.android.internal.R.drawable.ic_lockscreen_camera));
        cDrawable.setEnabled(false);

        if (action == null || action.equals("") || action.equals("none"))
            return cDrawable;
        if (action.equals("screenshot"))
            return new TargetDrawable(mResources, mResources.getDrawable(R.drawable.ic_action_screenshot));
        if (action.equals("ime_switcher"))
            return new TargetDrawable(mResources, mResources.getDrawable(R.drawable.ic_action_ime_switcher));
        if (action.equals("ring_vib"))
            return new TargetDrawable(mResources, mResources.getDrawable(R.drawable.ic_action_vib));
        if (action.equals("ring_silent"))
            return new TargetDrawable(mResources, mResources.getDrawable(R.drawable.ic_action_silent));
        if (action.equals("ring_vib_silent"))
            return new TargetDrawable(mResources, mResources.getDrawable(R.drawable.ic_action_ring_vib_silent));
        if (action.equals("killcurrent"))
            return new TargetDrawable(mResources, mResources.getDrawable(R.drawable.ic_action_killtask));
        if (action.equals("power"))
            return new TargetDrawable(mResources, mResources.getDrawable(R.drawable.ic_action_power));
        if (action.equals("screenoff"))
            return new TargetDrawable(mResources, mResources.getDrawable(R.drawable.ic_action_power));
        if (action.equals("assist"))
            return new TargetDrawable(mResources, com.android.internal.R.drawable.ic_action_assist_generic);
        try {
            Intent in = Intent.parseUri(action, 0);
            ActivityInfo aInfo = in.resolveActivityInfo(mPackageManager, PackageManager.GET_ACTIVITIES);
            Drawable activityIcon = aInfo.loadIcon(mPackageManager);
            Drawable iconBg = mResources.getDrawable(R.drawable.ic_navbar_blank);
            Drawable iconBgActivated = mResources.getDrawable(R.drawable.ic_navbar_blank_activated);
            int margin = (int)(iconBg.getIntrinsicHeight() / 3);
            LayerDrawable icon = new LayerDrawable (new Drawable[] {iconBg, activityIcon});
            icon.setLayerInset(1, margin, margin, margin, margin);
            LayerDrawable iconActivated = new LayerDrawable (new Drawable[] {iconBgActivated, activityIcon});
            iconActivated.setLayerInset(1, margin, margin, margin, margin);
            StateListDrawable selector = new StateListDrawable();
            selector.addState(new int[] {android.R.attr.state_enabled, -android.R.attr.state_active, -android.R.attr.state_focused}, icon);
            selector.addState(new int[] {android.R.attr.state_enabled, android.R.attr.state_active, -android.R.attr.state_focused}, iconActivated);
            selector.addState(new int[] {android.R.attr.state_enabled, -android.R.attr.state_active, android.R.attr.state_focused}, iconActivated);
            return new TargetDrawable(mResources, selector);
        } catch (Exception e) {
            return cDrawable;
        }
    }

    private void maybeSwapSearchIcon() {
        Intent intent = SearchManager.getAssistIntent(mContext);
        if (intent != null) {
            ComponentName component = intent.getComponent();
            if (component == null || !mGlowPadView.replaceTargetDrawablesIfPresent(component,
                    ASSIST_ICON_METADATA_NAME,
                    com.android.internal.R.drawable.ic_action_assist_generic)) {
                if (DEBUG) Slog.v(TAG, "Couldn't grab icon for component " + component);
            }
        }
    }

    private boolean pointInside(int x, int y, View v) {
        final int l = v.getLeft();
        final int r = v.getRight();
        final int t = v.getTop();
        final int b = v.getBottom();
        return x >= l && x < r && y >= t && y < b;
    }

    public boolean isInContentArea(int x, int y) {
        if (pointInside(x, y, mSearchTargetsContainer)) {
            return true;
        } else if (mStatusBarTouchProxy != null &&
                pointInside(x, y, mStatusBarTouchProxy)) {
            return true;
        } else {
            return false;
        }
    }

    private final OnPreDrawListener mPreDrawListener = new ViewTreeObserver.OnPreDrawListener() {
        public boolean onPreDraw() {
            getViewTreeObserver().removeOnPreDrawListener(this);
            mGlowPadView.resumeAnimations();
            return false;
        }
    };

    private void vibrate() {
        Context context = getContext();
        if (Settings.System.getInt(context.getContentResolver(),
                Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) != 0) {
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            vibrator.vibrate(mResources.getInteger(R.integer.config_search_panel_view_vibration_duration));
        }
    }

    public void show(final boolean show, boolean animate) {
        if (!show) {
            final LayoutTransition transitioner = animate ? createLayoutTransitioner() : null;
            ((ViewGroup) mSearchTargetsContainer).setLayoutTransition(transitioner);
        }
        mShowing = show;
        if (show) {
            maybeSwapSearchIcon();
            if (getVisibility() != View.VISIBLE) {
                setVisibility(View.VISIBLE);
                // Don't start the animation until we've created the layer, which is done
                // right before we are drawn
                mGlowPadView.suspendAnimations();
                mGlowPadView.ping();
                getViewTreeObserver().addOnPreDrawListener(mPreDrawListener);
                vibrate();
            }
            setFocusable(true);
            setFocusableInTouchMode(true);
            requestFocus();
        } else {
            setVisibility(View.INVISIBLE);
        }
    }

    public void hide(boolean animate) {
        if (mBar != null) {
            // This will indirectly cause show(false, ...) to get called
            mBar.animateCollapse(CommandQueue.FLAG_EXCLUDE_NONE);
        } else {
            setVisibility(View.INVISIBLE);
        }
    }

    /**
     * We need to be aligned at the bottom.  LinearLayout can't do this, so instead,
     * let LinearLayout do all the hard work, and then shift everything down to the bottom.
     */
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        // setPanelHeight(mSearchTargetsContainer.getHeight());
    }

    @Override
    public boolean dispatchHoverEvent(MotionEvent event) {
        // Ignore hover events outside of this panel bounds since such events
        // generate spurious accessibility events with the panel content when
        // tapping outside of it, thus confusing the user.
        final int x = (int) event.getX();
        final int y = (int) event.getY();
        if (x >= 0 && x < getWidth() && y >= 0 && y < getHeight()) {
            return super.dispatchHoverEvent(event);
        }
        return true;
    }

    /**
     * Whether the panel is showing, or, if it's animating, whether it will be
     * when the animation is done.
     */
    public boolean isShowing() {
        return mShowing;
    }

    public void setBar(BaseStatusBar bar) {
        mBar = bar;
    }

    public void setStatusBarView(final View statusBarView) {
        if (mStatusBarTouchProxy != null) {
            mStatusBarTouchProxy.setStatusBar(statusBarView);
//            mGlowPadView.setOnTouchListener(new OnTouchListener() {
//                public boolean onTouch(View v, MotionEvent event) {
//                    return statusBarView.onTouchEvent(event);
//                }
//            });
        }
    }

    private LayoutTransition createLayoutTransitioner() {
        LayoutTransition transitioner = new LayoutTransition();
        transitioner.setDuration(200);
        transitioner.setStartDelay(LayoutTransition.CHANGE_DISAPPEARING, 0);
        transitioner.setAnimator(LayoutTransition.DISAPPEARING, null);
        return transitioner;
    }

    public boolean isAssistantAvailable() {
        return SearchManager.getAssistIntent(mContext) != null;
    }

    private void screenOff() {
        PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        pm.goToSleep(SystemClock.uptimeMillis() + 1);
    }

    private void killProcess() {
        try {
            final Intent intent = new Intent(Intent.ACTION_MAIN);
            String defaultHomePackage = "com.android.launcher";
            intent.addCategory(Intent.CATEGORY_HOME);
            final ResolveInfo res = mContext.getPackageManager().resolveActivity(intent, 0);
            if (res.activityInfo != null && !res.activityInfo.packageName.equals("android")) {
                defaultHomePackage = res.activityInfo.packageName;
            }
            boolean targetKilled = false;
            IActivityManager am = ActivityManagerNative.getDefault();
            List<RunningAppProcessInfo> apps = am.getRunningAppProcesses();
            for (RunningAppProcessInfo appInfo : apps) {
                int uid = appInfo.uid;
                // Make sure it's a foreground user application (not system,
                // root, phone, etc.)
                if (uid >= Process.FIRST_APPLICATION_UID && uid <= Process.LAST_APPLICATION_UID
                        && appInfo.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                    if (appInfo.pkgList != null && (appInfo.pkgList.length > 0)) {
                        for (String pkg : appInfo.pkgList) {
                            if (!pkg.equals("com.android.systemui") && !pkg.equals(defaultHomePackage)) {
                                am.forceStopPackage(pkg);
                                targetKilled = true;
                                break;
                            }
                        }
                    } else {
                        Process.killProcess(appInfo.pid);
                        targetKilled = true;
                    }
                }
                if (targetKilled) {
                    Toast.makeText(mContext, R.string.app_killed_message, Toast.LENGTH_SHORT).show();
                    break;
                }
            }
        } catch (RemoteException remoteException) {
            // Do nothing; just let it go.
        }
    }

    /**
     * functions needed for taking screenhots. This leverages the built in ICS
     * screenshot functionality
     */
    final Object mScreenshotLock = new Object();
    ServiceConnection mScreenshotConnection = null;

    final Runnable mScreenshotTimeout = new Runnable() {
        @Override
        public void run() {
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
                        Handler h = new Handler(H.getLooper()) {
                            @Override
                            public void handleMessage(Message msg) {
                                synchronized (mScreenshotLock) {
                                    if (mScreenshotConnection == myConn) {
                                        mContext.unbindService(mScreenshotConnection);
                                        mScreenshotConnection = null;
                                        H.removeCallbacks(mScreenshotTimeout);
                                    }
                                }
                            }
                        };
                        msg.replyTo = new Messenger(h);
                        msg.arg1 = msg.arg2 = 0;

                        /*
                         * remove for the time being if (mStatusBar != null &&
                         * mStatusBar.isVisibleLw()) msg.arg1 = 1; if
                         * (mNavigationBar != null &&
                         * mNavigationBar.isVisibleLw()) msg.arg2 = 1;
                         */

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
                public void onServiceDisconnected(ComponentName name) {
                }
            };
            if (mContext.bindService(intent, conn, Context.BIND_AUTO_CREATE)) {
                mScreenshotConnection = conn;
                H.postDelayed(mScreenshotTimeout, 10000);
            }
        }
    }

    private void powerMenu() {
        final CharSequence[] item_entries = {"Shutdown", "Reboot", "Recovery", "Bootloader"};
        final CharSequence[] item_values = {"shutdown", "", "recovery", "bootloader"};
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle("Power Menu");
        builder.setItems(item_entries, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
                if (which == 1
                        || which == 2
                        || which == 3) {
                        PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
                        pm.reboot((String)item_values[which]);
                }
            }
        });
        builder.setNegativeButton("Cancel", new Dialog.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
                dialog.dismiss();
            }
        });
        builder.create().show();
     }

    private Handler H = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {

            }
        }
    };

    public int screenLayout() {
        final int screenSize = Resources.getSystem().getConfiguration().screenLayout &
                Configuration.SCREENLAYOUT_SIZE_MASK;
        return screenSize;
    }

    public boolean isScreenPortrait() {
        return mResources.getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
    }

    public class TargetObserver extends ContentObserver {
        public TargetObserver(Handler handler) {
            super(handler);
        }

        @Override
        public boolean deliverSelfNotifications() {
            return super.deliverSelfNotifications();
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            setDrawables();
        }
    }

}
