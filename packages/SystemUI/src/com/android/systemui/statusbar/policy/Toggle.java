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

package com.android.systemui.statusbar.policy;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.systemui.R;

public abstract class Toggle extends RelativeLayout implements
        CompoundButton.OnCheckedChangeListener {
    protected static final String TAG = "Toggle";

    protected Context mContext;
    protected boolean mEnabled = false;
    protected boolean mSystemChanging = false;

    // widgets
    protected ImageView mIcon;
    protected TextView mText;
    protected CompoundButton mToggle;

    private int mTextContent;
    private int mImageContent;

    public Toggle(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Toggle(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs);

        mContext = context;
        View.inflate(context, R.layout.toggle, this);

        TypedArray a = mContext.obtainStyledAttributes(attrs,
                R.styleable.Toggle);

        final int N = a.getIndexCount();
        for (int i = 0; i < N; ++i)
        {
            int attr = a.getIndex(i);
            switch (attr)
            {
                case R.styleable.Toggle_labelText:
                    mTextContent = a.getResourceId(attr, 0);

                    break;
                case R.styleable.Toggle_labelImage:
                    mImageContent = a.getResourceId(attr, 0);
                    break;
            }
        }
        a.recycle();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mIcon = (ImageView) findViewById(R.id.icon);
        mToggle = (CompoundButton) findViewById(R.id.toggle);
        mText = (TextView) findViewById(R.id.label);

        if (mTextContent != 0)
            mText.setText(mTextContent);
        else
            mText.setVisibility(View.GONE);

        if (mImageContent != 0)
            mIcon.setImageResource(mImageContent);
        else
            mIcon.setVisibility(View.GONE);

        mToggle.setOnCheckedChangeListener(this);
    }

    protected abstract void updateInternalState();

    protected abstract void onCheckChanged(boolean isChecked);

    public void updateToggleState() {
        mSystemChanging = true;

        updateInternalState();

        mSystemChanging = false;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (mSystemChanging)
            return;

        onCheckChanged(isChecked);
    }

}
