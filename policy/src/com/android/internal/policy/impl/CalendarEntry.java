
package com.android.internal.policy.impl;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.internal.R;

public class CalendarEntry extends LinearLayout {

    private static final String TAG = "CalendarEntry";
    TextView mTitle;
    TextView mDetails;

    public CalendarEntry(Context context, String title, String details, int width) {
        super(context);
        View view = LayoutInflater.from(context).inflate(R.layout.calendar_entry, this);
        
        this.mTitle = (TextView) view.findViewById(R.id.event_title);
        this.mDetails = (TextView) view.findViewById(R.id.event_details);
        mTitle.setWidth(width);
        mTitle.setText(title);
        mDetails.setText(details);
    }

    public void setColor(int color) {
        mTitle.setTextColor(color);
        mDetails.setTextColor(color);
    }
}
