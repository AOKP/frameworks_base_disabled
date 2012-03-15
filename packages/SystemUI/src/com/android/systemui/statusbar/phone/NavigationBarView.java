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

import java.io.FileDescriptor;
import java.io.PrintWriter;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Rect;
import android.os.Handler;
import android.os.ServiceManager;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Slog;
import android.view.Display;
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
import com.android.systemui.statusbar.policy.buttons.BackKeyWithKillButtonView;
import com.android.systemui.statusbar.policy.buttons.HomeKeyWithTasksButtonView;
import com.android.systemui.statusbar.policy.buttons.RecentsKey;
import com.android.systemui.statusbar.policy.buttons.SearchKeyButtonView;

public class NavigationBarView extends LinearLayout {
    final static boolean DEBUG = false;
    final static String TAG = "PhoneStatusBar/NavigationBarView";

    final static boolean DEBUG_DEADZONE = false;

    final static boolean NAVBAR_ALWAYS_AT_RIGHT = true;

    final static boolean ANIMATE_HIDE_TRANSITION = false; // turned off because it introduces
                                                          // unsightly delay when videos goes to
                                                          // full screen

    protected IStatusBarService mBarService;
    final Display mDisplay;
    View mCurrentView = null;
    View[] mRotatedViews = new View[4];

    int mBarSize;
    boolean mVertical;

    boolean mHidden, mLowProfile, mShowMenu;
    int mDisabledFlags = 0;

    public final static int SHOW_LEFT_MENU = 1;
    public final static int SHOW_RIGHT_MENU = 0;
    public final static int SHOW_BOTH_MENU = 2;
    public final static int SHOW_DONT = 4;

    public final static int VISIBILITY_SYSTEM = 0;
    public final static int VISIBILITY_SYSTEM_AND_INVIZ = 3;
    public final static int VISIBILITY_NEVER = 1;
    public final static int VISIBILITY_ALWAYS = 2;

    // constants to split from user setting
    public static final String NAV_BACK = "BACK";
    public static final String NAV_HOME = "HOME";
    public static final String NAV_MENU = "MENU";
    public static final String NAV_MENU_BIG = "MENU_BIG";
    public static final String NAV_SEARCH = "SEARCH";
    public static final String NAV_TASKS = "TASKS";
    public static final String DELIMITER = "|";

    private static final String STOCK_NAVBAR = NAV_BACK + DELIMITER + NAV_HOME + DELIMITER
            + NAV_TASKS;

    private String userNavBarButtons = STOCK_NAVBAR;

    // constants to generate keys
    public static final int KEY_BACK = 0;
    public static final int KEY_HOME = 1;
    public static final int KEY_MENU_RIGHT = 2;
    public static final int KEY_MENU_LEFT = 5;
    public static final int KEY_SEARCH = 3;
    public static final int KEY_TASKS = 4;
    public static final int KEY_MENU_BIG = 6;

    public View getSearchButton() {
        return mCurrentView.findViewById(R.id.search);
    }

    public View getRecentsButton() {
        return mCurrentView.findViewById(R.id.recent_apps);
    }

    public View getLeftMenuButton() {
        return mCurrentView.findViewById(R.id.menu_left);
    }

    public View getRightMenuButton() {
        return mCurrentView.findViewById(R.id.menu);
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

    int numKeys = 3;

    KeyButtonView largeMenuButton;

    private void makeBar(String navKeys) {
        if (navKeys == null)
            navKeys = STOCK_NAVBAR;

        ((LinearLayout) rot0.findViewById(R.id.nav_buttons)).removeAllViews();
        ((LinearLayout) rot0.findViewById(R.id.lights_out)).removeAllViews();
        ((LinearLayout) rot90.findViewById(R.id.nav_buttons)).removeAllViews();
        ((LinearLayout) rot90.findViewById(R.id.lights_out)).removeAllViews();

        String[] split = navKeys.split("\\" + DELIMITER);
        numKeys = split.length;

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

            for (int j = 0; j < split.length; j++) {
                Log.i(TAG, "split: " + split[j]);
                View v = null;

                boolean notFound = false;
                if (split[j].equals(NAV_BACK))
                    v = generateKey(landscape, KEY_BACK);
                else if (split[j].equals(NAV_HOME))
                    v = generateKey(landscape, KEY_HOME);
                else if (split[j].equals(NAV_SEARCH))
                    v = generateKey(landscape, KEY_SEARCH);
                else if (split[j].equals(NAV_TASKS))
                    v = generateKey(landscape, KEY_TASKS);
                else if (split[j].equals(NAV_MENU_BIG))
                    v = generateKey(landscape, KEY_MENU_BIG);
                else {
                    // if we get here, this is baaaaaaaaaaaaaaaad, revert to stock setup
                    notFound = true;
                    Settings.System.putString(mContext.getContentResolver(),
                            Settings.System.NAVIGATION_BAR_BUTTONS, STOCK_NAVBAR);
                }

                if (!notFound) {
                    addButton(navButtonLayout, v, landscape);
                    addLightsOutButton(lightsOut, v, landscape, false);

                    if (j == (split.length - 1)) {
                        // which to skip
                    } else if (numKeys == 3) {
                        // add separator view here
                        View separator = new View(mContext);
                        separator.setLayoutParams(getSeparatorLayoutParams(landscape));
                        addButton(navButtonLayout, separator, landscape);
                        addLightsOutButton(lightsOut, separator, landscape, true);
                    }
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
     * TODO we can probably inflate each key from an XML would also be extremely useful to themers,
     * they may hate this for now
     */
    private View generateKey(boolean landscape, int keyId) {
        KeyButtonView v = null;
        Resources r = getResources();

        int btnWidth = 80;

        switch (keyId) {
            case KEY_BACK:
                v = new BackKeyWithKillButtonView(mContext, null);
                v.setLayoutParams(getLayoutParams(landscape, btnWidth));

                v.setId(R.id.back);
                v.setCode(4);
                v.setImageResource(landscape ? R.drawable.ic_sysbar_back_land
                        : R.drawable.ic_sysbar_back);
                v.setContentDescription(r.getString(R.string.accessibility_back));
                v.setGlowBackground(landscape ? R.drawable.ic_sysbar_highlight_land
                        : R.drawable.ic_sysbar_highlight);
                return v;

            case KEY_HOME:
                v = new HomeKeyWithTasksButtonView(mContext, null);
                v.setLayoutParams(getLayoutParams(landscape, btnWidth));

                v.setId(R.id.home);
                v.setCode(3);
                v.setImageResource(landscape ? R.drawable.ic_sysbar_home_land
                        : R.drawable.ic_sysbar_home);
                v.setContentDescription(r.getString(R.string.accessibility_home));
                v.setGlowBackground(landscape ? R.drawable.ic_sysbar_highlight_land
                        : R.drawable.ic_sysbar_highlight);
                return v;

            case KEY_SEARCH:
                v = new SearchKeyButtonView(mContext, null);
                v.setLayoutParams(getLayoutParams(landscape, btnWidth));

                v.setId(R.id.search);
                v.setCode(84);
                v.setImageResource(landscape ? R.drawable.ic_sysbar_search_land
                        : R.drawable.ic_sysbar_search);
                v.setGlowBackground(landscape ? R.drawable.ic_sysbar_highlight_land
                        : R.drawable.ic_sysbar_highlight);
                return v;

            case KEY_TASKS:
                v = new RecentsKey(mContext, null);
                v.setLayoutParams(getLayoutParams(landscape, btnWidth));

                v.setId(R.id.recent_apps);
                v.setImageResource(landscape ? R.drawable.ic_sysbar_recent_land
                        : R.drawable.ic_sysbar_recent);
                v.setContentDescription(r.getString(R.string.accessibility_recent));
                v.setGlowBackground(landscape ? R.drawable.ic_sysbar_highlight_land
                        : R.drawable.ic_sysbar_highlight);
                return v;

            case KEY_MENU_BIG:
                v = new KeyButtonView(mContext, null);
                v.setLayoutParams(getLayoutParams(landscape, btnWidth));

                v.setCode(82);
                v.setImageResource(landscape ? R.drawable.ic_sysbar_menu_land_big
                        : R.drawable.ic_sysbar_menu_big);
                v.setId(R.id.menu_big);
                v.setContentDescription(r.getString(R.string.accessibility_menu));
                v.setGlowBackground(landscape ? R.drawable.ic_sysbar_highlight_land
                        : R.drawable.ic_sysbar_highlight);
                return v;

            case KEY_MENU_RIGHT:
                v = new KeyButtonView(mContext, null);
                v.setLayoutParams(getLayoutParams(landscape, 40));

                v.setId(R.id.menu);
                v.setCode(82);
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
                v.setCode(82);
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
                // even though setting the systemUI visibility below will turn these views
                // on, we need them to come up faster so that they can catch this motion
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

        try {
            getBackButton().setVisibility(disableBack ? View.INVISIBLE : View.VISIBLE);
        } catch (NullPointerException e) {
        }
        try {
            getHomeButton().setVisibility(disableHome ? View.INVISIBLE : View.VISIBLE);
        } catch (NullPointerException e) {
        }
        try {
            getRecentsButton().setVisibility(disableRecent ? View.INVISIBLE : View.VISIBLE);
        } catch (NullPointerException e) {
        }
        try {
            getSearchButton().setVisibility(disableHome ? View.INVISIBLE : View.VISIBLE);
        } catch (NullPointerException e) {
        }
        try {
            getBigMenuButton().setVisibility(disableHome ? View.INVISIBLE : View.VISIBLE);
        } catch (NullPointerException e) {
        }

        final boolean hideBar = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.LOCKSCREEN_HIDE_NAV, 0) == 1;
        if (hideBar && disableHome && disableRecent && disableBack) {
            
        } else {
           
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
        Slog.d(TAG, (hide ? "HIDING" : "SHOWING") + " navigation bar");

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
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.NAVIGATION_BAR_BUTTONS), false,
                    this);
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.MENU_LOCATION), false,
                    this);
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.MENU_VISIBILITY), false,
                    this);
            updateSettings();
        }

        @Override
        public void onChange(boolean selfChange) {
            updateSettings();
        }
    }

    protected void updateSettings() {
        ContentResolver resolver = mContext.getContentResolver();

        userNavBarButtons = Settings.System.getString(resolver,
                Settings.System.NAVIGATION_BAR_BUTTONS);

        currentSetting = Settings.System.getInt(getContext().getContentResolver(),
                Settings.System.MENU_LOCATION, SHOW_RIGHT_MENU);

        currentVisibility = Settings.System.getInt(getContext().getContentResolver(),
                Settings.System.MENU_VISIBILITY, VISIBILITY_SYSTEM);

        makeBar(userNavBarButtons);

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
