
package com.android.internal.policy.impl;

import java.io.File;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

class LockscreenWallpaper extends FrameLayout {

    private final String TAG = "LockscreenWallpaperUpdater";

    private final String WALLPAPER_IMAGE_PATH = "/data/data/com.roman.romcontrol/files/lockscreen_wallpaper.jpg";

    private ImageView mLockScreenWallpaperImage;

    Bitmap bitmap = null;

    public LockscreenWallpaper(Context context, AttributeSet attrs) {
        super(context);

        setLockScreenWallpaper();
    }

    public void setLockScreenWallpaper() {
        File file = new File(WALLPAPER_IMAGE_PATH);

        if (file.exists()) {
            mLockScreenWallpaperImage = new ImageView(getContext());
            mLockScreenWallpaperImage.setScaleType(ScaleType.CENTER_CROP);
            addView(mLockScreenWallpaperImage, -1, -1);
            Bitmap lockb = BitmapFactory.decodeFile(WALLPAPER_IMAGE_PATH);
            mLockScreenWallpaperImage.setImageDrawable(new BitmapDrawable(getResources(), lockb));
        } else {
            removeAllViews();
        }
    }
}
