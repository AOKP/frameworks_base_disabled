
package com.android.systemui.statusbar.policy.buttons;

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
import com.android.systemui.statusbar.policy.KeyButtonView;

public class SearchKeyButtonView extends KeyButtonView {

    public SearchKeyButtonView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SearchKeyButtonView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs);

        mSupportsLongpress = true; // a.getBoolean(R.styleable.KeyButtonView_keyRepeat, true);
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
