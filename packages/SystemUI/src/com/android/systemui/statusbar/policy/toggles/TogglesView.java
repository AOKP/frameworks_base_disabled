
package com.android.systemui.statusbar.policy.toggles;

import java.util.ArrayList;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.HorizontalScrollView;

import com.android.systemui.R;

public class TogglesView extends LinearLayout {

    private static final String TAG = "ToggleView";

    ArrayList<LinearLayout> rows = new ArrayList<LinearLayout>();
    ArrayList<Toggle> toggles = new ArrayList<Toggle>();

    private static final String TOGGLE_DELIMITER = "|";

    public static final int STYLE_NONE = 1;
    public static final int STYLE_ICON = 2;
    public static final int STYLE_TEXT = 3;
    public static final int STYLE_TEXT_AND_ICON = 4;

    private int mToggleStyle = STYLE_TEXT;

    public static final int BRIGHTNESS_LOC_TOP = 1;
    public static final int BRIGHTNESS_LOC_BOTTOM = 2;
    public static final int BRIGHTNESS_LOC_NONE = 3;

    private int mBrightnessLocation = BRIGHTNESS_LOC_TOP;

    private static final String TOGGLE_AUTOROTATE = "ROTATE";
    private static final String TOGGLE_BLUETOOTH = "BT";
    private static final String TOGGLE_GPS = "GPS";
    private static final String TOGGLE_LTE = "LTE";
    private static final String TOGGLE_DATA = "DATA";
    private static final String TOGGLE_WIFI = "WIFI";
    private static final String TOGGLE_2G = "2G";
    private static final String TOGGLE_WIFI_AP = "AP";
    private static final String TOGGLE_AIRPLANE = "AIRPLANE_MODE";
    private static final String TOGGLE_VIBRATE = "VIBRATE";
    private static final String TOGGLE_SILENT = "SILENT";

    private int mWidgetsPerRow = 2;
    
    private boolean useAltButtonLayout = false;

    public static final String STOCK_TOGGLES = TOGGLE_WIFI + TOGGLE_DELIMITER
            + TOGGLE_BLUETOOTH + TOGGLE_DELIMITER
            + TOGGLE_GPS + TOGGLE_DELIMITER
            + TOGGLE_DATA + TOGGLE_DELIMITER
            + TOGGLE_AUTOROTATE;

    View mBrightnessSlider;

    private static final LinearLayout.LayoutParams PARAMS_BRIGHTNESS = new LinearLayout.LayoutParams(
            LayoutParams.MATCH_PARENT,
            90);

    private static final LinearLayout.LayoutParams PARAMS_TOGGLE = new LinearLayout.LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT, 1f);

    public TogglesView(Context context, AttributeSet attrs) {
        super(context, attrs);

        SettingsObserver settingsObserver = new SettingsObserver(new Handler());
        settingsObserver.observe();
    }

    private void addToggles(String userToggles) {
        if (userToggles == null)
            userToggles = STOCK_TOGGLES;

        Log.e(TAG, userToggles);
        String[] split = userToggles.split("\\" + TOGGLE_DELIMITER);
        toggles.clear();

        for (String splitToggle : split) {
            Log.e(TAG, "split: " + splitToggle);
            Toggle newToggle = null;

            if (splitToggle.equals(TOGGLE_AUTOROTATE))
                newToggle = new AutoRotateToggle(mContext);
            else if (splitToggle.equals(TOGGLE_BLUETOOTH))
                newToggle = new BluetoothToggle(mContext);
            else if (splitToggle.equals(TOGGLE_DATA))
                newToggle = new NetworkToggle(mContext);
            else if (splitToggle.equals(TOGGLE_GPS))
                newToggle = new GpsToggle(mContext);
            else if (splitToggle.equals(TOGGLE_LTE))
                newToggle = new LteToggle(mContext);
            else if (splitToggle.equals(TOGGLE_WIFI))
                newToggle = new WifiToggle(mContext);
            else if (splitToggle.equals(TOGGLE_2G))
                newToggle = new TwoGToggle(mContext);
            else if (splitToggle.equals(TOGGLE_WIFI_AP))
                newToggle = new WifiAPToggle(mContext);
            else if (splitToggle.equals(TOGGLE_AIRPLANE))
                newToggle = new AirplaneModeToggle(mContext);
            else if (splitToggle.equals(TOGGLE_VIBRATE))
                newToggle = new VibrateToggle(mContext);
            else if (splitToggle.equals(TOGGLE_SILENT))
                newToggle = new SilentToggle(mContext);

            if (newToggle == null)
                return;

            toggles.add(newToggle);
        }

    }

    private void addBrightness() {
        rows.add(new LinearLayout(mContext));
        rows.get(rows.size() - 1).addView(new BrightnessSlider(mContext).getView(),
                PARAMS_BRIGHTNESS);
    }

    private void addViews() {
        removeViews();
        rows = new ArrayList<LinearLayout>();

        if (mBrightnessLocation == BRIGHTNESS_LOC_TOP)
            addBrightness();

        for (int i = 0; i < toggles.size(); i++) {
            if (i % mWidgetsPerRow == 0) {
                // new row
            	 rows.add(new LinearLayout(mContext));
            }
            rows.get(rows.size() - 1).addView(toggles.get(i).getView(), PARAMS_TOGGLE);
        }
        if (useAltButtonLayout) {   
        	HorizontalScrollView sv = new HorizontalScrollView(mContext);
        	sv.addView(rows.get(rows.size() - 1), PARAMS_TOGGLE);
        	LinearLayout ll = new LinearLayout(mContext);
        	ll.setOrientation(LinearLayout.HORIZONTAL);
        	ll.addView(sv,PARAMS_TOGGLE);
        	rows.remove(rows.size() - 1);
        	rows.add(ll);
        	
        }
        if (mBrightnessLocation == BRIGHTNESS_LOC_BOTTOM)
            addBrightness();

        addSeparator();

        for (LinearLayout row : rows)
            this.addView(row);

    }

    private void removeViews() {
        for (LinearLayout row : rows) {
            this.removeView(row);
        }
    }

    private void addSeparator() {
        View sep = new View(mContext);
        sep.setBackgroundResource(R.drawable.status_bar_hr);

        DisplayMetrics metrics = getContext().getResources().getDisplayMetrics();
        float dp = 2f;
        float fpixels = metrics.density * dp;
        int pixels = (int) (metrics.density * dp + 0.5f);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                pixels);

        sep.setLayoutParams(params);

        rows.add(new LinearLayout(mContext));
        rows.get(rows.size() - 1).addView(sep);
    }

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.STATUSBAR_TOGGLES), false,
                    this);
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.STATUSBAR_TOGGLES_STYLE), false, this);
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.STATUSBAR_TOGGLES_BRIGHTNESS_LOC),
                    false, this);
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.STATUSBAR_TOGGLES_NUMBER_PER_ROW),
                    false, this);
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.STATUSBAR_TOGGLES_USE_BUTTONS),
                    false, this);
            updateSettings();
        }

        @Override
        public void onChange(boolean selfChange) {
            updateSettings();
        }
    }

    protected void updateSettings() {
        ContentResolver resolver = mContext.getContentResolver();

        addToggles(Settings.System.getString(resolver, Settings.System.STATUSBAR_TOGGLES));

        mToggleStyle = Settings.System.getInt(resolver, Settings.System.STATUSBAR_TOGGLES_STYLE,
                STYLE_TEXT);

        mBrightnessLocation = Settings.System.getInt(resolver,
                Settings.System.STATUSBAR_TOGGLES_BRIGHTNESS_LOC,
                BRIGHTNESS_LOC_TOP);

        useAltButtonLayout = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.STATUSBAR_TOGGLES_USE_BUTTONS, 0) == 1;

        // mWidgetsPerRow = Settings.System.getInt(resolver,
        // Settings.System.STATUSBAR_TOGGLES_NUMBER_PER_ROW,
        // 2);
        // use 2 for regular layout, 6 for buttons
        // TODO: make buttons scrollable so we can have more than 6
        // ZB Edit - Temp make mWidgetsPerRow 100 for altWidgetLayout.  A more elegant solution
        // will need to be implemented in the main loop.
        
        mWidgetsPerRow = useAltButtonLayout ? 100 : 2;

        boolean addText = false;
        boolean addIcon = false;

        switch (mToggleStyle) {
            case STYLE_NONE:
                break;
            case STYLE_ICON:
                addIcon = true;
                break;
            default:
            case STYLE_TEXT:
                addText = true;
                break;
            case STYLE_TEXT_AND_ICON:
                addText = true;
                addIcon = true;
                break;
        }

        for (Toggle t : toggles)
            t.setupInfo(addIcon, addText);

        addViews();

    }

    public void onStatusbarExpanded() {
        for(Toggle t : toggles)
            t.onStatusbarExpanded();
    }
}
