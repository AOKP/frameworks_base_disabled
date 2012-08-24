package com.android.internal.policy.impl;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.android.internal.R;

public class CalendarEntry extends LinearLayout {

    private static final String TAG = "CalendarEntry";
    TextView mTitleView;
    TextView mDetailsView;
    Context mContext;
    int mWidth;
    
    public CalendarEntry(Context context, String title, String details, int width) {
        super(context);
        mContext = context;
        mWidth = width;
        View view = LayoutInflater.from(mContext).inflate(R.layout.calendar_entry, this, true);
        mTitleView = (TextView) view.findViewById(R.id.event_title);
        mDetailsView = (TextView) view.findViewById(R.id.event_details);
        setLayoutParams(new FrameLayout.LayoutParams(-2, -2, 5));
        mTitleView.setText(title);
        mDetailsView.setText(details);
    }

    public void setColor(int color) {
        Log.d(TAG, "setting color?");
        if (mTitleView != null) {
            Log.d(TAG, "SETTING COLOR!");
            mTitleView.setTextColor(color);
        }
        if (mDetailsView != null) {
            mDetailsView.setTextColor(color);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Adjust width as necessary
        int measuredWidth = MeasureSpec.getSize(widthMeasureSpec);
        if(mWidth > 0 && mWidth < measuredWidth) {
            int measureMode = MeasureSpec.getMode(widthMeasureSpec);
            widthMeasureSpec = MeasureSpec.makeMeasureSpec(mWidth, measureMode);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
