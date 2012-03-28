
package com.android.internal.policy.impl;

import android.content.Context;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;

public class CalendarEntry extends LinearLayout {

    private static final String TAG = "CalendarEntry";
    TextView mTitle;
    TextView mDetails;

    public CalendarEntry(Context context, String title, String details, int width) {
        super(context);
        this.setOrientation(LinearLayout.HORIZONTAL);
        this.setGravity(android.view.Gravity.RIGHT);
        mTitle = new TextView(getContext());
        mTitle.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0, -2, 1));
        mTitle.setText(title);
        mTitle.setTextAppearance(getContext(), android.R.attr.textAppearanceMedium);
        mTitle.setSingleLine(true);
        mTitle.setEllipsize(android.text.TextUtils.TruncateAt.END);
        mTitle.setPadding(0, 0, 2, 0);
        mTitle.setGravity(android.view.Gravity.RIGHT);
        mDetails = new TextView(getContext());
        mDetails.setLayoutParams(new android.widget.LinearLayout.LayoutParams(-2, -2));
        mDetails.setText(details);
        mDetails.setTextAppearance(getContext(), android.R.attr.textAppearanceMedium);
        mDetails.setSingleLine(true);
        mDetails.setPadding(2, 0, 0, 0);
        this.addView(mTitle);
        this.addView(mDetails);
    }

    public void setColor(int color) {
        mTitle.setTextColor(color);
        mDetails.setTextColor(color);
    }
}
