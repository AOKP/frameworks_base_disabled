
package com.android.systemui.statusbar.policy.toggles;

import java.util.ArrayList;

import com.android.systemui.R;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

public class TogglesView extends LinearLayout {

    private static final String TAG = "ToggleView";

    ArrayList<Toggle> toggles = new ArrayList<Toggle>();

    private static final String TOGGLE_DELIMITER = "|";
    private static final String INCLUDE_ICON = "+";
    private static final String INCLUDE_TEXT = "_";

    private static final String TOGGLE_AUTOROTATE = "ROTATE";
    private static final String TOGGLE_BLUETOOTH = "BT";
    private static final String TOGGLE_GPS = "GPS";
    private static final String TOGGLE_LTE = "LTE";
    private static final String TOGGLE_DATA = "DATA";
    private static final String TOGGLE_WIFI = "WIFI";

    View mBrightnessSlider;

    private static final LinearLayout.LayoutParams TOGGLE_BRIGHTNESS = new LinearLayout.LayoutParams(
            LayoutParams.MATCH_PARENT,
            80);

    private static final LinearLayout.LayoutParams TOGGLE_PARAMS = new LinearLayout.LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT, 1f);

    public TogglesView(Context context, AttributeSet attrs) {
        super(context, attrs);

        String stockSetup = TOGGLE_WIFI + INCLUDE_TEXT
                + TOGGLE_DELIMITER
                + TOGGLE_BLUETOOTH + INCLUDE_TEXT
                + TOGGLE_DELIMITER
                + TOGGLE_GPS + INCLUDE_TEXT
                + TOGGLE_DELIMITER
                + TOGGLE_DATA + INCLUDE_TEXT
                + TOGGLE_DELIMITER +
                TOGGLE_AUTOROTATE + INCLUDE_TEXT;

        addView(new BrightnessSlider(mContext).getView(), TOGGLE_BRIGHTNESS);

        addToggles(stockSetup);
        addViews();
    }

    private void addToggles(String userToggles) {
        Log.e(TAG, userToggles);
        String[] split = userToggles.split("\\" + TOGGLE_DELIMITER);
        toggles.clear();

        for (String splitToggle : split) {
            Log.e(TAG, "split: " + splitToggle);
            Toggle newToggle = null;

            if (splitToggle.startsWith(TOGGLE_AUTOROTATE))
                newToggle = new AutoRotateToggle(mContext);
            else if (splitToggle.startsWith(TOGGLE_BLUETOOTH))
                newToggle = new BluetoothToggle(mContext);
            else if (splitToggle.startsWith(TOGGLE_DATA))
                newToggle = new NetworkToggle(mContext);
            else if (splitToggle.startsWith(TOGGLE_GPS))
                newToggle = new GpsToggle(mContext);
            else if (splitToggle.startsWith(TOGGLE_LTE))
                newToggle = new LteToggle(mContext);
            else if (splitToggle.startsWith(TOGGLE_WIFI))
                newToggle = new WifiToggle(mContext);

            if (newToggle == null)
                return;

            newToggle.setupInfo(splitToggle.contains(INCLUDE_ICON),
                    splitToggle.contains(INCLUDE_TEXT));
            toggles.add(newToggle);
        }

    }

    public void addViews() {
        ArrayList<LinearLayout> rows = new ArrayList<LinearLayout>();

        for (int i = 0; i < toggles.size(); i++) {
            if (i % 2 == 0) {
                // new row
                rows.add(new LinearLayout(mContext));
            }
            rows.get(rows.size() - 1).addView(toggles.get(i).getView(), TOGGLE_PARAMS);
        }

        for (LinearLayout row : rows)
            this.addView(row);
    }

}
