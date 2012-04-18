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

package com.android.systemui.statusbar.phone;

import java.io.File;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.net.URISyntaxException;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.ServiceManager;
import android.provider.Settings;
import android.provider.Settings.System;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Slog;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.android.internal.statusbar.IStatusBarService;
import com.android.systemui.R;
import com.android.systemui.statusbar.policy.KeyButtonView;
//import com.android.systemui.statusbar.policy.buttons.BackKeyWithKillButtonView;
//import com.android.systemui.statusbar.policy.buttons.HomeKeyWithTasksButtonView;
//import com.android.systemui.statusbar.policy.buttons.RecentsKey;
//import com.android.systemui.statusbar.policy.buttons.SearchKeyButtonView;
import com.android.systemui.statusbar.policy.buttons.ExtensibleKeyButtonView;

public class NavigationBarView extends LinearLayout {
    final static boolean DEBUG = false;
    final static String TAG = "PhoneStatusBar/NavigationBarView";

    final static boolean DEBUG_DEADZONE = false;

    final static boolean NAVBAR_ALWAYS_AT_RIGHT = true;

    final static boolean ANIMATE_HIDE_TRANSITION = false; // turned off because
                                                          // it introduces
                                                          // unsightly delay
                                                          // when videos goes to
                                                          // full screen

    protected IStatusBarService mBarService;
    final Display mDisplay;
    View mCurrentView = null;
    View[] mRotatedViews = new View[4];

    int mBarSize;
    boolean mVertical;

    boolean mHidden, mLowProfile, mShowMenu;
    int mDisabledFlags = 0;

    final static String ACTION_HOME = "**home**";
    final static String ACTION_BACK = "**back**";
    final static String ACTION_SEARCH = "**search**";
    final static String ACTION_MENU = "**menu**";
    final static String ACTION_POWER = "**power**";
    final static String ACTION_RECENTS = "**recents**";
    final static String ACTION_KILL = "**kill**";
    final static String ACTION_NULL = "**null**";

    int mNumberOfButtons = 3;

    public String[] mClickActions = new String[5];
    public String[] mLongpressActions = new String[5];
    public String[] mPortraitIcons = new String[5];

    public final static int StockButtonsQty = 3;
    public final static String[] StockClickActions = {
            "**back**", "**home**", "**recents**", "**null**", "**null**"
    };

    public final static String[] StockLongpress = {
            "**null**", "**null**", "**null**", "**null**", "**null**"
    };

    public final static int SHOW_LEFT_MENU = 1;
    public final static int SHOW_RIGHT_MENU = 0;
    public final static int SHOW_BOTH_MENU = 2;
    public final static int SHOW_DONT = 4;

    public final static int VISIBILITY_SYSTEM = 0;
    public final static int VISIBILITY_SYSTEM_AND_INVIZ = 3;
    public final static int VISIBILITY_NEVER = 1;
    public final static int VISIBILITY_ALWAYS = 2;

    public static final int KEY_MENU_RIGHT = 2;
    public static final int KEY_MENU_LEFT = 5;

    // workaround for LayoutTransitions leaving the nav buttons in a weird state
    // (bug 5549288)
    final static boolean WORKAROUND_INVALID_LAYOUT = true;
    final static int MSG_CHECK_INVALID_LAYOUT = 8686;

    private class H extends Handler {
        public void handleMessage(Message m) {
            switch (m.what) {
                case MSG_CHECK_INVALID_LAYOUT:
                    final String how = "" + m.obj;
                    final int w = getWidth();
                    final int h = getHeight();
                    final int vw = mCurrentView.getWidth();
                    final int vh = mCurrentView.getHeight();

                    if (h != vh || w != vw) {
                        Slog.w(TAG, String.format(
                                "*** Invalid layout in navigation bar (%s this=%dx%d cur=%dx%d)",
                                how, w, h, vw, vh));
                        if (WORKAROUND_INVALID_LAYOUT) {
                            requestLayout();
                        }
                    }
                    break;
            }
        }
    }

    private H mHandler = new H();

    public View getLeftMenuButton() {
        return mCurrentView.findViewById(R.id.menu_left);
    }

    public View getRightMenuButton() {
        return mCurrentView.findViewById(R.id.menu);
    }
    
    public View getSearchButton() {
        return mCurrentView.findViewById(R.id.search);
    }
	
    public View getRecentsButton() {
        return mCurrentView.findViewById(R.id.recent_apps);
    }
    
    public View getBackButton() {
        return mCurrentView.findViewById(R.id.back);
    }

    public View getHomeButton() {
        return mCurrentView.findViewById(R.id.home);
    }

    public View getBigMenuButton() {
        return mCurrentView.findViewById(R.id.menu_big);
    }
	
    public NavigationBarView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mHidden = false;

        mDisplay = ((WindowManager) context.getSystemService(
                Context.WINDOW_SERVICE)).getDefaultDisplay();
        mBarService = IStatusBarService.Stub.asInterface(
                ServiceManager.getService(Context.STATUS_BAR_SERVICE));

        final Resources res = mContext.getResources();
        mBarSize = res.getDimensionPixelSize(R.dimen.navigation_bar_size);
        mVertical = false;
        mShowMenu = false;
        originalHeight = getHeight();
    }

    FrameLayout rot0;
    FrameLayout rot90;

    private void makeBar() {

        ((LinearLayout) rot0.findViewById(R.id.nav_buttons)).removeAllViews();
        ((LinearLayout) rot0.findViewById(R.id.lights_out)).removeAllViews();
        ((LinearLayout) rot90.findViewById(R.id.nav_buttons)).removeAllViews();
        ((LinearLayout) rot90.findViewById(R.id.lights_out)).removeAllViews();

        for (int i = 0; i <= 1; i++) {
            boolean landscape = (i == 1);

            LinearLayout navButtonLayout = (LinearLayout) (landscape ? rot90
                    .findViewById(R.id.nav_buttons) : rot0
                    .findViewById(R.id.nav_buttons));

            LinearLayout lightsOut = (LinearLayout) (landscape ? rot90
                    .findViewById(R.id.lights_out) : rot0
                    .findViewById(R.id.lights_out));

            // add left menu
            if (currentSetting != SHOW_DONT) {
                View leftmenuKey = generateKey(landscape, KEY_MENU_LEFT);
                addButton(navButtonLayout, leftmenuKey, landscape);
                addLightsOutButton(lightsOut, leftmenuKey, landscape, true);
            }

            for (int j = 0; j < mNumberOfButtons; j++) {
                ExtensibleKeyButtonView v = null;

                v = generateKey(landscape, mClickActions[j], mLongpressActions[j],
                         mPortraitIcons[j]);
                v.setTag((landscape ? "key_land_" : "key_") + j);

                addButton(navButtonLayout, v, landscape);
                addLightsOutButton(lightsOut, v, landscape, false);

                if (j == (mNumberOfButtons - 1)) {
                    // which to skip
                } else if (mNumberOfButtons == 3) {
                    // add separator view here
                    View separator = new View(mContext);
                    separator.setLayoutParams(getSeparatorLayoutParams(landscape));
                    addButton(navButtonLayout, separator, landscape);
                    addLightsOutButton(lightsOut, separator, landscape, true);
                }
            }

            if (currentSetting != SHOW_DONT) {
                View rightMenuKey = generateKey(landscape, KEY_MENU_RIGHT);
                addButton(navButtonLayout, rightMenuKey, landscape);
                addLightsOutButton(lightsOut, rightMenuKey, landscape, true);
            }
        }

    }

    private void addLightsOutButton(LinearLayout root, View v, boolean landscape, boolean empty) {

        ImageView addMe = new ImageView(mContext);
        addMe.setLayoutParams(v.getLayoutParams());
        addMe.setImageResource(empty ? R.drawable.ic_sysbar_lights_out_dot_large
                : R.drawable.ic_sysbar_lights_out_dot_small);
        addMe.setScaleType(ImageView.ScaleType.CENTER);
        addMe.setVisibility(empty ? View.INVISIBLE : View.VISIBLE);

        if (landscape)
            root.addView(addMe, 0);
        else
            root.addView(addMe);

    }

    private void addButton(ViewGroup root, View addMe, boolean landscape) {
        if (landscape)
            root.addView(addMe, 0);
        else
            root.addView(addMe);
    }

    /*
     * TODO we can probably inflate each key from an XML would also be extremely
     * useful to themers, they may hate this for now
     */
    private View generateKey(boolean landscape, int keyId) {
        KeyButtonView v = null;
        Resources r = getResources();

        int btnWidth = 80;

        switch (keyId) {

            case KEY_MENU_RIGHT:
                v = new KeyButtonView(mContext, null);
                v.setLayoutParams(getLayoutParams(landscape, 40));

                v.setId(R.id.menu);
                v.setCode(KeyEvent.KEYCODE_MENU);
                v.setImageResource(landscape ? R.drawable.ic_sysbar_menu_land
                        : R.drawable.ic_sysbar_menu);
                v.setVisibility(View.INVISIBLE);
                v.setContentDescription(r.getString(R.string.accessibility_menu));
                v.setGlowBackground(landscape ? R.drawable.ic_sysbar_highlight_land
                        : R.drawable.ic_sysbar_highlight);
                return v;

            case KEY_MENU_LEFT:
                v = new KeyButtonView(mContext, null);
                v.setLayoutParams(getLayoutParams(landscape, 40));

                v.setId(R.id.menu_left);
                v.setCode(KeyEvent.KEYCODE_MENU);
                v.setImageResource(landscape ? R.drawable.ic_sysbar_menu_land
                        : R.drawable.ic_sysbar_menu);
                v.setVisibility(View.INVISIBLE);
                v.setContentDescription(r.getString(R.string.accessibility_menu));
                v.setGlowBackground(landscape ? R.drawable.ic_sysbar_highlight_land
                        : R.drawable.ic_sysbar_highlight);
                return v;

        }

        return null;
    }

    private ExtensibleKeyButtonView generateKey(boolean landscape, String ClickAction,
            String Longpress,
            String IconUri) {
        ExtensibleKeyButtonView v = null;
        Resources r = getResources();

        int btnWidth = 80;

        v = new ExtensibleKeyButtonView(mContext, null, ClickAction, Longpress);
        v.setLayoutParams(getLayoutParams(landscape, btnWidth));
        v.setGlowBackground(landscape ? R.drawable.ic_sysbar_highlight_land
                : R.drawable.ic_sysbar_highlight);

        // the rest is for setting the icon (or custom icon)
        if (IconUri != null && IconUri.length() > 0) {
            File f = new File(Uri.parse(IconUri).getPath());
            if (f.exists())
                v.setImageDrawable(new BitmapDrawable(r, f.getAbsolutePath()));
        }

        if (IconUri != null && !IconUri.equals("")
                && IconUri.startsWith("file")) {
            // it's an icon the user chose from the gallery here
            File icon = new File(Uri.parse(IconUri).getPath());
            if (icon.exists())
                v.setImageDrawable(new BitmapDrawable(getResources(), icon.getAbsolutePath()));

        } else if (IconUri != null && !IconUri.equals("")) {
            // here they chose another app icon
            try {
                PackageManager pm = getContext().getPackageManager();
                v.setImageDrawable(pm.getActivityIcon(Intent.parseUri(IconUri, 0)));
            } catch (NameNotFoundException e) {
                e.printStackTrace();
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        } else {
            // ok use default icons here
            v.setImageDrawable(getNavbarIconImage(landscape, ClickAction));
        }

        return v;
    }

    private LayoutParams getLayoutParams(boolean landscape, float dp) {
        float px = dp * getResources().getDisplayMetrics().density;
        return landscape ?
                new LayoutParams(LayoutParams.MATCH_PARENT, (int) px, 1f) :
                new LayoutParams((int) px, LayoutParams.MATCH_PARENT, 1f);
    }

    private LayoutParams getSeparatorLayoutParams(boolean landscape) {
        float px = 25 * getResources().getDisplayMetrics().density;
        return landscape ?
                new LayoutParams(LayoutParams.MATCH_PARENT, (int) px) :
                new LayoutParams((int) px, LayoutParams.MATCH_PARENT);
    }

    View.OnTouchListener mLightsOutListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent ev) {
            if (ev.getAction() == MotionEvent.ACTION_DOWN) {
                // even though setting the systemUI visibility below will turn
                // these views
                // on, we need them to come up faster so that they can catch
                // this motion
                // event
                setLowProfile(false, false, false);

                try {
                    mBarService.setSystemUiVisibility(0);
                } catch (android.os.RemoteException ex) {
                }
            }
            return false;
        }
    };
    private int currentVisibility;
    private int currentSetting;

    public void setDisabledFlags(int disabledFlags) {
        setDisabledFlags(disabledFlags, false);
    }

    public void setDisabledFlags(int disabledFlags, boolean force) {
        if (!force && mDisabledFlags == disabledFlags)
            return;

        mDisabledFlags = disabledFlags;

        final boolean disableHome = ((disabledFlags & View.STATUS_BAR_DISABLE_HOME) != 0);
        final boolean disableRecent = ((disabledFlags & View.STATUS_BAR_DISABLE_RECENT) != 0);
        final boolean disableBack = ((disabledFlags & View.STATUS_BAR_DISABLE_BACK) != 0);

        for (int j = 0; j < mNumberOfButtons; j++) {
            View v = (View) findViewWithTag((mVertical ? "key_land_" : "key_") + j);
            if (v != null) {
                int vid = v.getId();
                if (vid == R.id.back) {
                    v.setVisibility(disableBack ? View.INVISIBLE : View.VISIBLE);
                } else if (vid == R.id.recent_apps) {
                    v.setVisibility(disableRecent ? View.INVISIBLE : View.VISIBLE);
                } else { // treat all other buttons as same rule as home
                    v.setVisibility(disableHome ? View.INVISIBLE : View.VISIBLE);
                }

            }
        }
    }

    int originalHeight = 0;

    public void setMenuVisibility(final boolean show) {
        setMenuVisibility(show, false);
    }

    public void setMenuVisibility(final boolean show, final boolean force) {
        if (!force && mShowMenu == show)
            return;

        if (currentSetting == SHOW_DONT) {
            return;
        }

        mShowMenu = show;
        boolean localShow = show;

        ImageView leftButton = (ImageView) getLeftMenuButton();
        ImageView rightButton = (ImageView) getRightMenuButton();

        switch (currentVisibility) {
            case VISIBILITY_SYSTEM:
                leftButton
                        .setImageResource(mVertical ? R.drawable.ic_sysbar_menu_land
                                : R.drawable.ic_sysbar_menu);
                rightButton
                        .setImageResource(mVertical ? R.drawable.ic_sysbar_menu_land
                                : R.drawable.ic_sysbar_menu);
                break;
            case VISIBILITY_ALWAYS:
                leftButton
                        .setImageResource(mVertical ? R.drawable.ic_sysbar_menu_land
                                : R.drawable.ic_sysbar_menu);
                rightButton
                        .setImageResource(mVertical ? R.drawable.ic_sysbar_menu_land
                                : R.drawable.ic_sysbar_menu);
                localShow = true;
                break;
            case VISIBILITY_NEVER:
                leftButton
                        .setImageResource(R.drawable.ic_sysbar_menu_inviz);
                rightButton
                        .setImageResource(R.drawable.ic_sysbar_menu_inviz);
                localShow = true;
                break;
            case VISIBILITY_SYSTEM_AND_INVIZ:
                if (localShow) {
                    leftButton
                            .setImageResource(mVertical ? R.drawable.ic_sysbar_menu_land
                                    : R.drawable.ic_sysbar_menu);
                    ((ImageView) getRightMenuButton())
                            .setImageResource(mVertical ? R.drawable.ic_sysbar_menu_land
                                    : R.drawable.ic_sysbar_menu);
                } else {
                    localShow = true;
                    leftButton
                            .setImageResource(R.drawable.ic_sysbar_menu_inviz);
                    rightButton
                            .setImageResource(R.drawable.ic_sysbar_menu_inviz);
                }
                break;
        }

        // do this after just in case show was changed
        switch (currentSetting) {
            case SHOW_BOTH_MENU:
                getLeftMenuButton().setVisibility(localShow ? View.VISIBLE : View.INVISIBLE);
                getRightMenuButton().setVisibility(localShow ? View.VISIBLE : View.INVISIBLE);
                break;
            case SHOW_LEFT_MENU:
                getLeftMenuButton().setVisibility(localShow ? View.VISIBLE : View.INVISIBLE);
                getRightMenuButton().setVisibility(View.INVISIBLE);
                break;
            default:
            case SHOW_RIGHT_MENU:
                getLeftMenuButton().setVisibility(View.INVISIBLE);
                getRightMenuButton().setVisibility(localShow ? View.VISIBLE : View.INVISIBLE);
                break;
        }
    }

    public void setLowProfile(final boolean lightsOut) {
        setLowProfile(lightsOut, true, false);
    }

    public void setLowProfile(final boolean lightsOut, final boolean animate, final boolean force) {
        // if (!force && lightsOut == mLowProfile)
        // return;

        mLowProfile = lightsOut;

        if (DEBUG)
            Slog.d(TAG, "setting lights " + (lightsOut ? "out" : "on"));

        final View navButtons = mCurrentView.findViewById(R.id.nav_buttons);
        final View lowLights = mCurrentView.findViewById(R.id.lights_out);

        // ok, everyone, stop it right there
        navButtons.animate().cancel();
        lowLights.animate().cancel();

        if (!animate) {
            navButtons.setAlpha(lightsOut ? 0f : 1f);

            lowLights.setAlpha(lightsOut ? 1f : 0f);
            lowLights.setVisibility(lightsOut ? View.VISIBLE : View.GONE);
        } else {
            navButtons.animate()
                    .alpha(lightsOut ? 0f : 1f)
                    .setDuration(lightsOut ? 600 : 200)
                    .start();

            lowLights.setOnTouchListener(mLightsOutListener);
            if (lowLights.getVisibility() == View.GONE) {
                lowLights.setAlpha(0f);
                lowLights.setVisibility(View.VISIBLE);
            }
            lowLights.animate()
                    .alpha(lightsOut ? 1f : 0f)
                    .setStartDelay(lightsOut ? 500 : 0)
                    .setDuration(lightsOut ? 1000 : 300)
                    .setInterpolator(new AccelerateInterpolator(2.0f))
                    .setListener(lightsOut ? null : new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator _a) {
                            lowLights.setVisibility(View.GONE);
                        }
                    })
                    .start();
        }
    }

    public void setHidden(final boolean hide) {
        if (hide == mHidden)
            return;

        mHidden = hide;
        Slog.d(TAG,
                (hide ? "HIDING" : "SHOWING") + " navigation bar");

        // bring up the lights no matter what
        setLowProfile(false);
    }

    public void onFinishInflate() {
        rot0 = (FrameLayout) findViewById(R.id.rot0);
        rot90 = (FrameLayout) findViewById(R.id.rot90);

        // this takes care of making the buttons
        SettingsObserver settingsObserver = new SettingsObserver(new Handler());
        settingsObserver.observe();

        mRotatedViews[Surface.ROTATION_0] =
                mRotatedViews[Surface.ROTATION_180] = findViewById(R.id.rot0);

        mRotatedViews[Surface.ROTATION_90] = findViewById(R.id.rot90);

        mRotatedViews[Surface.ROTATION_270] = NAVBAR_ALWAYS_AT_RIGHT
                ? findViewById(R.id.rot90)
                : findViewById(R.id.rot270);

        for (View v : mRotatedViews) {
            // this helps avoid drawing artifacts with glowing navigation keys
            ViewGroup group = (ViewGroup) v.findViewById(R.id.nav_buttons);
            group.setMotionEventSplittingEnabled(false);
        }
        mCurrentView = mRotatedViews[Surface.ROTATION_0];

    }

    public void reorient() {
        final int rot = mDisplay.getRotation();
        for (int i = 0; i < 4; i++) {
            mRotatedViews[i].setVisibility(View.GONE);
        }
        mCurrentView = mRotatedViews[rot];
        mCurrentView.setVisibility(View.VISIBLE);
        mVertical = (rot == Surface.ROTATION_90 || rot == Surface.ROTATION_270);

        // force the low profile & disabled states into compliance
        setLowProfile(mLowProfile, false, true /* force */);
        setDisabledFlags(mDisabledFlags, true /* force */);
        setMenuVisibility(mShowMenu, true /* force */);

        if (DEBUG_DEADZONE) {
            mCurrentView.findViewById(R.id.deadzone).setBackgroundColor(0x808080FF);
        }

        if (DEBUG) {
            Slog.d(TAG, "reorient(): rot=" + mDisplay.getRotation());
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (DEBUG)
            Slog.d(TAG, String.format(
                    "onSizeChanged: (%dx%d) old: (%dx%d)", w, h, oldw, oldh));
        postCheckForInvalidLayout("sizeChanged");
        super.onSizeChanged(w, h, oldw, oldh);
    }

    /*
     * @Override protected void onLayout (boolean changed, int left, int top,
     * int right, int bottom) { if (DEBUG) Slog.d(TAG, String.format(
     * "onLayout: %s (%d,%d,%d,%d)", changed?"changed":"notchanged", left, top,
     * right, bottom)); super.onLayout(changed, left, top, right, bottom); } //
     * uncomment this for extra defensiveness in WORKAROUND_INVALID_LAYOUT
     * situations: if all else // fails, any touch on the display will fix the
     * layout.
     * @Override public boolean onInterceptTouchEvent(MotionEvent ev) { if
     * (DEBUG) Slog.d(TAG, "onInterceptTouchEvent: " + ev.toString()); if
     * (ev.getAction() == MotionEvent.ACTION_DOWN) {
     * postCheckForInvalidLayout("touch"); } return
     * super.onInterceptTouchEvent(ev); }
     */

    private String getResourceName(int resId) {
        if (resId != 0) {
            final android.content.res.Resources res = mContext.getResources();
            try {
                return res.getResourceName(resId);
            } catch (android.content.res.Resources.NotFoundException ex) {
                return "(unknown)";
            }
        } else {
            return "(null)";
        }
    }

    private void postCheckForInvalidLayout(final String how) {
        mHandler.obtainMessage(MSG_CHECK_INVALID_LAYOUT, 0, 0, how).sendToTarget();
    }

    private static String visibilityToString(int vis) {
        switch (vis) {
            case View.INVISIBLE:
                return "INVISIBLE";
            case View.GONE:
                return "GONE";
            default:
                return "VISIBLE";
        }
    }

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            // resolver.registerContentObserver(
            // Settings.System.getUriFor(Settings.System.NAVIGATION_BAR_BUTTONS),
            // false,
            // this);
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.MENU_LOCATION), false,
                    this);
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.MENU_VISIBILITY), false,
                    this);

            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.NAVIGATION_BAR_BUTTONS_QTY), false,
                    this);

            for (int j = 0; j < 5; j++) { // watch all 5 settings for changes.
                resolver.registerContentObserver(
                        Settings.System.getUriFor(Settings.System.NAVIGATION_CUSTOM_ACTIVITIES[j]),
                        false,
                        this);
                resolver.registerContentObserver(
                        Settings.System
                                .getUriFor(Settings.System.NAVIGATION_LONGPRESS_ACTIVITIES[j]),
                        false,
                        this);
                resolver.registerContentObserver(
                        Settings.System.getUriFor(Settings.System.NAVIGATION_CUSTOM_APP_ICONS[j]),
                        false,
                        this);
            }
            updateSettings();
        }

        @Override
        public void onChange(boolean selfChange) {
            updateSettings();
        }
    }

    protected void updateSettings() {
        ContentResolver resolver = mContext.getContentResolver();

        // userNavBarButtons = Settings.System.getString(resolver,
        // Settings.System.NAVIGATION_BAR_BUTTONS);

        currentSetting = Settings.System.getInt(resolver,
                Settings.System.MENU_LOCATION, SHOW_RIGHT_MENU);

        currentVisibility = Settings.System.getInt(resolver,
                Settings.System.MENU_VISIBILITY, VISIBILITY_SYSTEM);

        mNumberOfButtons = Settings.System.getInt(resolver,
                Settings.System.NAVIGATION_BAR_BUTTONS_QTY, 0);
        if (mNumberOfButtons == 0){
        	mNumberOfButtons = StockButtonsQty;
        	Settings.System.putInt(resolver,
            		Settings.System.NAVIGATION_BAR_BUTTONS_QTY, StockButtonsQty);    	
        }

        for (int j = 0; j < mNumberOfButtons; j++) {
            mClickActions[j] = Settings.System.getString(resolver,
                    Settings.System.NAVIGATION_CUSTOM_ACTIVITIES[j]);
            if (mClickActions[j] == null){
                mClickActions[j] = StockClickActions[j];
                Settings.System.putString(resolver,
                		Settings.System.NAVIGATION_CUSTOM_ACTIVITIES[j], mClickActions[j]);
            }

            mLongpressActions[j] = Settings.System.getString(resolver,
                    Settings.System.NAVIGATION_LONGPRESS_ACTIVITIES[j]);
            if (mLongpressActions[j] == null) {
                mLongpressActions[j] = StockLongpress[j];
                Settings.System.putString(resolver,
                		Settings.System.NAVIGATION_LONGPRESS_ACTIVITIES[j], mLongpressActions[j]);
            }
            mPortraitIcons[j] = Settings.System.getString(resolver,
                    Settings.System.NAVIGATION_CUSTOM_APP_ICONS[j]);
            if (mPortraitIcons[j] == null) {
                mPortraitIcons[j] = "";
                Settings.System.putString(resolver,
                		Settings.System.NAVIGATION_CUSTOM_APP_ICONS[j], "");
            }
        }
        makeBar();

    }

    private Drawable getNavbarIconImage(boolean landscape, String uri) {

        if (uri == null)
            return getResources().getDrawable(R.drawable.ic_sysbar_null);

        if (uri.startsWith("**")) {
            if (uri.equals(ACTION_HOME)) {

                return getResources().getDrawable(R.drawable.ic_sysbar_home);
            } else if (uri.equals(ACTION_BACK)) {

                return getResources().getDrawable(R.drawable.ic_sysbar_back);
            } else if (uri.equals(ACTION_RECENTS)) {

                return getResources().getDrawable(R.drawable.ic_sysbar_recent);
            } else if (uri.equals(ACTION_SEARCH)) {

                return getResources().getDrawable(R.drawable.ic_sysbar_search);
            } else if (uri.equals(ACTION_MENU)) {

                return getResources().getDrawable(R.drawable.ic_sysbar_menu_big);
            } else if (uri.equals(ACTION_KILL)) {

                return getResources().getDrawable(R.drawable.ic_sysbar_killtask);
            } else if (uri.equals(ACTION_POWER)) {

                return getResources().getDrawable(R.drawable.ic_sysbar_power);
            }
        } else {
            try {
                return mContext.getPackageManager().getActivityIcon(Intent.parseUri(uri, 0));
            } catch (NameNotFoundException e) {
                e.printStackTrace();
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }

        return getResources().getDrawable(R.drawable.ic_sysbar_null);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (true)
            return;

        pw.println("NavigationBarView {");
        final Rect r = new Rect();

        pw.println(String.format("      this: " + PhoneStatusBar.viewInfo(this)
                + " " + visibilityToString(getVisibility())));

        getWindowVisibleDisplayFrame(r);
        final boolean offscreen = r.right > mDisplay.getRawWidth()
                || r.bottom > mDisplay.getRawHeight();
        pw.println("      window: "
                + r.toShortString()
                + " " + visibilityToString(getWindowVisibility())
                + (offscreen ? " OFFSCREEN!" : ""));

        pw.println(String.format("      mCurrentView: id=%s (%dx%d) %s",
                getResourceName(mCurrentView.getId()),
                mCurrentView.getWidth(), mCurrentView.getHeight(),
                visibilityToString(mCurrentView.getVisibility())));

        pw.println(String.format("      disabled=0x%08x vertical=%s hidden=%s low=%s menu=%s",
                mDisabledFlags,
                mVertical ? "true" : "false",
                mHidden ? "true" : "false",
                mLowProfile ? "true" : "false",
                mShowMenu ? "true" : "false"));

        final View back = getBackButton();
        final View home = getHomeButton();
        final View recent = getRecentsButton();
        final View menu = getRightMenuButton();

        pw.println("      back: "
                + PhoneStatusBar.viewInfo(back)
                + " " + visibilityToString(back.getVisibility())
                );
        pw.println("      home: "
                + PhoneStatusBar.viewInfo(home)
                + " " + visibilityToString(home.getVisibility())
                );
        pw.println("      rcnt: "
                + PhoneStatusBar.viewInfo(recent)
                + " " + visibilityToString(recent.getVisibility())
                );
        pw.println("      menu: "
                + PhoneStatusBar.viewInfo(menu)
                + " " + visibilityToString(menu.getVisibility())
                );
        pw.println("    }");
    }
}
