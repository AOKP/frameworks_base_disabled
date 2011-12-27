
package com.android.systemui.statusbar.policy;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.os.ServiceManager;
import android.util.AttributeSet;
import android.util.Slog;
import android.view.IWindowManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.View.OnLongClickListener;
import android.view.accessibility.AccessibilityEvent;

import com.android.systemui.R;

public class SearchKeyButtonView extends KeyButtonView {

    public SearchKeyButtonView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SearchKeyButtonView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.KeyButtonView,
                defStyle, 0);

        mCode = a.getInteger(R.styleable.KeyButtonView_keyCode, 0);

        mSupportsLongpress = true; // a.getBoolean(R.styleable.KeyButtonView_keyRepeat, true);

        mGlowBG = a.getDrawable(R.styleable.KeyButtonView_glowBackground);
        if (mGlowBG != null) {
            mDrawingAlpha = BUTTON_QUIESCENT_ALPHA;
        }

        a.recycle();

        mWindowManager = IWindowManager.Stub.asInterface(
                ServiceManager.getService(Context.WINDOW_SERVICE));

        setClickable(true);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        
        this.setOnLongClickListener(mSearchLongClickListener);
    }

    Runnable mCheckLongPress = new Runnable() {
        public void run() {
            if (isPressed()) {
                performLongClick();
            }
        }
    };
    
    private OnLongClickListener mSearchLongClickListener = new OnLongClickListener() {

        @Override
        public boolean onLongClick(View v) {
            v.getContext().sendBroadcast(new Intent(Intent.ACTION_SEARCH_LONG_PRESS));
            return true;
        }
    };

}
