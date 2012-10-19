package com.android.systemui.statusbar.policy;

import java.net.URISyntaxException;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.ServiceConnection;
import android.hardware.input.InputManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Toast;

import com.android.internal.statusbar.IStatusBarService;
import com.android.systemui.statusbar.phone.NavigationBarView;
import com.android.systemui.statusbar.policy.KeyButtonView;
import com.android.systemui.R;


public class ExtensibleKeyButtonView extends KeyButtonView {

    final static String ACTION_HOME = "**home**";
    final static String ACTION_BACK = "**back**";
    final static String ACTION_SEARCH = "**search**";
    final static String ACTION_SCREENSHOT = "**screenshot**";
    final static String ACTION_MENU = "**menu**";
    final static String ACTION_POWER = "**power**";
    final static String ACTION_NOTIFICATIONS = "**notifications**";
    final static String ACTION_RECENTS = "**recents**";
    final static String ACTION_IME = "**ime**";
    final static String ACTION_KILL = "**kill**";
    final static String ACTION_NULL = "**null**";
    final static String ACTION_WIDGETS = "**widgets**";

    private static final String TAG = "Key.Ext";

    IStatusBarService mBarService;

    public String mClickAction, mLongpress;

    public Handler mHandler;

    public ActivityManager mActivityManager;

    public int mInjectKeycode;

    public ExtensibleKeyButtonView(Context context, AttributeSet attrs) {
        this(context, attrs, ACTION_NULL,ACTION_NULL);
    }

    public ExtensibleKeyButtonView(Context context, AttributeSet attrs, String ClickAction, String Longpress) {
        super(context, attrs);

        mHandler = new Handler();
        mActivityManager = (ActivityManager) context.getSystemService(Activity.ACTIVITY_SERVICE);
        mBarService = IStatusBarService.Stub.asInterface(
                ServiceManager.getService(Context.STATUS_BAR_SERVICE));
        mClickAction = ClickAction;
        mLongpress = Longpress;
        setCode(0);
        if (ClickAction != null){
        	if (ClickAction.equals(ACTION_HOME)) {
        		setCode(KeyEvent.KEYCODE_HOME);
        		setId(R.id.home);
        	} else if (ClickAction.equals(ACTION_BACK)) {
        		setCode (KeyEvent.KEYCODE_BACK);
        		setId(R.id.back);
        	} else if (ClickAction.equals(ACTION_SEARCH)) {
        		setCode (KeyEvent.KEYCODE_SEARCH);
        	} else if (ClickAction.equals(ACTION_MENU)) {
        		setCode (KeyEvent.KEYCODE_MENU);
        		setId(R.id.menu);
        	} else if (ClickAction.equals(ACTION_POWER)) {
        		setCode (KeyEvent.KEYCODE_POWER);
            } else { // the remaining options need to be handled by OnClick;
        		setOnClickListener(mClickListener);
        		if (ClickAction.equals(ACTION_RECENTS))
        			setId(R.id.recent_apps);
        	}
        }
        setSupportsLongPress (false);
        if (Longpress != null)
        	if ((!Longpress.equals(ACTION_NULL)) || (getCode() !=0)) {
        		// I want to allow long presses for defined actions, or if 
        		// primary action is a 'key' and long press isn't defined otherwise
        		setSupportsLongPress(true);
        		setOnLongClickListener(mLongPressListener);
        	}
    }

    public void injectKeyDelayed(int keycode){
        mInjectKeycode = keycode;
        mHandler.removeCallbacks(onInjectKey_Down);
        mHandler.removeCallbacks(onInjectKey_Up);
        mHandler.post(onInjectKey_Down);
        mHandler.postDelayed(onInjectKey_Up,10); // introduce small delay to handle key press
    }

    final Runnable onInjectKey_Down = new Runnable() {
    	public void run() {
    	    final KeyEvent ev = new KeyEvent(mDownTime, SystemClock.uptimeMillis(), KeyEvent.ACTION_DOWN, mInjectKeycode, 0,
                    0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
                    KeyEvent.FLAG_FROM_SYSTEM | KeyEvent.FLAG_VIRTUAL_HARD_KEY,
                    InputDevice.SOURCE_KEYBOARD);
            InputManager.getInstance().injectInputEvent(ev,
                    InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);
        }
    };

    final Runnable onInjectKey_Up = new Runnable() {
    	public void run() {
            final KeyEvent ev = new KeyEvent(mDownTime, SystemClock.uptimeMillis(), KeyEvent.ACTION_UP, mInjectKeycode, 0,
                    0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
                    KeyEvent.FLAG_FROM_SYSTEM | KeyEvent.FLAG_VIRTUAL_HARD_KEY,
                    InputDevice.SOURCE_KEYBOARD);
            InputManager.getInstance().injectInputEvent(ev,
                    InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);
        }
    };

    Runnable mKillTask = new Runnable() {
        public void run() {
            final Intent intent = new Intent(Intent.ACTION_MAIN);
            String defaultHomePackage = "com.android.launcher";
            intent.addCategory(Intent.CATEGORY_HOME);
            final ResolveInfo res = mContext.getPackageManager().resolveActivity(intent, 0);
            if (res.activityInfo != null && !res.activityInfo.packageName.equals("android")) {
                defaultHomePackage = res.activityInfo.packageName;
            }
            String packageName = mActivityManager.getRunningTasks(1).get(0).topActivity.getPackageName();
            if (!defaultHomePackage.equals(packageName)) {
                    mActivityManager.forceStopPackage(packageName);
                    Toast.makeText(mContext, R.string.app_killed_message, Toast.LENGTH_SHORT).show();
            }
        }
    };

    private OnClickListener mClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
        	// the other consts were handled by keycode.  
        	
        	if (mClickAction.equals(ACTION_NULL)) {
        		// who would set a button with no ClickAction?  
        		// Stranger things have happened.
        		return;
        		
        	} else if (mClickAction.equals(ACTION_RECENTS)) {
        		try {
                    mBarService.toggleRecentApps();
                } catch (RemoteException e) {            	
                }
                return;

            } else if (mClickAction.equals(ACTION_NOTIFICATIONS)) {
                try {
                    mBarService.toggleNotificationShade();
                } catch (RemoteException e) {
                    // A RemoteException is like a cold
                    // Let's hope we don't catch one!
                }
                return;

                } else if (mClickAction.equals(ACTION_IME)) {

                        getContext().sendBroadcast(new Intent("android.settings.SHOW_INPUT_METHOD_PICKER"));
                        return;

            } else if (mClickAction.equals(ACTION_SCREENSHOT)) {
                takeScreenshot();
                return;

        	} else if (mClickAction.equals(ACTION_KILL)) {

        		mHandler.postDelayed(mKillTask,ViewConfiguration.getGlobalActionKeyTimeout());
        		return;

        	} else if (mClickAction.equals(ACTION_WIDGETS)) {
        		Intent toggleWidgets = new Intent(
                        NavigationBarView.WidgetReceiver.ACTION_TOGGLE_WIDGETS);
                mContext.sendBroadcast(toggleWidgets);
                return;
        	} else {  // we must have a custom uri
        		 try {
                     Intent intent = Intent.parseUri(mClickAction, 0);
                     intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                     getContext().startActivity(intent);
                 } catch (URISyntaxException e) {
                     Log.e(TAG, "URISyntaxException: [" + mClickAction + "]");
                 } catch (ActivityNotFoundException e){
        		 	 Log.e(TAG, "ActivityNotFound: [" + mClickAction + "]");
        		 }
        	}
            return;
        }
    };
    
    private OnLongClickListener mLongPressListener = new OnLongClickListener() {

        @Override
        public boolean onLongClick(View v) {
        	if (mLongpress == null) {
        		return true;
        	}
        	if (mLongpress.equals(ACTION_NULL)) {
        		// attempt to keep long press functionality of 'keys' if
        		// they haven't been overridden.
                return true;
        	} else if (mLongpress.equals(ACTION_HOME)) {
        		injectKeyDelayed(KeyEvent.KEYCODE_HOME);
        		return true;
        	} else if (mLongpress.equals(ACTION_BACK)) {
        		injectKeyDelayed(KeyEvent.KEYCODE_BACK);
        		return true;
        	} else if (mLongpress.equals(ACTION_SEARCH)) {
        		injectKeyDelayed(KeyEvent.KEYCODE_SEARCH);
        		return true;
        	} else if (mLongpress.equals(ACTION_MENU)) {
        		injectKeyDelayed(KeyEvent.KEYCODE_MENU);
        		return true;
        	} else if (mLongpress.equals(ACTION_POWER)) {
        		injectKeyDelayed(KeyEvent.KEYCODE_POWER);
        		return true;
                } else if (mLongpress.equals(ACTION_IME)) {
                        getContext().sendBroadcast(new Intent("android.settings.SHOW_INPUT_METHOD_PICKER"));
                        return true;
            } else if (mLongpress.equals(ACTION_SCREENSHOT)) {
                takeScreenshot();
                return true;
        	} else if (mLongpress.equals(ACTION_KILL)) {
        		mHandler.post(mKillTask);
        		return true;
            } else if (mLongpress.equals(ACTION_WIDGETS)) {
                Intent toggleWidgets = new Intent(
                        NavigationBarView.WidgetReceiver.ACTION_TOGGLE_WIDGETS);
                mContext.sendBroadcast(toggleWidgets);
                return true;
        	} else if (mLongpress.equals(ACTION_RECENTS)) {
        		try {
                    mBarService.toggleRecentApps();
                } catch (RemoteException e) {   
                	// let it go.
                }
                return true;
            } else if (mClickAction.equals(ACTION_NOTIFICATIONS)) {
                try {
                    mBarService.toggleNotificationShade();
                } catch (RemoteException e) {
                    // A RemoteException is like a cold
                    // Let's hope we don't catch one!
                }
                return true;
        	} else {  // we must have a custom uri
       		 	try {
       		 		Intent intent = Intent.parseUri(mLongpress, 0);
       		 		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
       		 		getContext().startActivity(intent);
       		 	} catch (URISyntaxException e) {
       		 		Log.e(TAG, "URISyntaxException: [" + mLongpress + "]");
       		 	} catch (ActivityNotFoundException e){
       		 		Log.e(TAG, "ActivityNotFound: [" + mLongpress + "]");
       		 	}
       		 	return true;
        	}        	           
        }
    };

    /**
     * functions needed for taking screenhots. This leverages the built in ICS
     * screenshot functionality
     */
    final Object mScreenshotLock = new Object();
    ServiceConnection mScreenshotConnection = null;

    final Runnable mScreenshotTimeout = new Runnable() {
        @Override
        public void run() {
            synchronized (mScreenshotLock) {
                if (mScreenshotConnection != null) {
                    mContext.unbindService(mScreenshotConnection);
                    mScreenshotConnection = null;
                }
            }
        }
    };

    private void takeScreenshot() {
        synchronized (mScreenshotLock) {
            if (mScreenshotConnection != null) {
                return;
            }
            ComponentName cn = new ComponentName("com.android.systemui",
                    "com.android.systemui.screenshot.TakeScreenshotService");
            Intent intent = new Intent();
            intent.setComponent(cn);
            ServiceConnection conn = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    synchronized (mScreenshotLock) {
                        if (mScreenshotConnection != this) {
                            return;
                        }
                        Messenger messenger = new Messenger(service);
                        Message msg = Message.obtain(null, 1);
                        final ServiceConnection myConn = this;
                        Handler h = new Handler(H.getLooper()) {
                            @Override
                            public void handleMessage(Message msg) {
                                synchronized (mScreenshotLock) {
                                    if (mScreenshotConnection == myConn) {
                                        mContext.unbindService(mScreenshotConnection);
                                        mScreenshotConnection = null;
                                        H.removeCallbacks(mScreenshotTimeout);
                                    }
                                }
                            }
                        };
                        msg.replyTo = new Messenger(h);
                        msg.arg1 = msg.arg2 = 0;

                        /*
                         * remove for the time being if (mStatusBar != null &&
                         * mStatusBar.isVisibleLw()) msg.arg1 = 1; if
                         * (mNavigationBar != null &&
                         * mNavigationBar.isVisibleLw()) msg.arg2 = 1;
                         */

                        /* wait for the dialog box to close */
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ie) {
                        }

                        /* take the screenshot */
                        try {
                            messenger.send(msg);
                        } catch (RemoteException e) {
                        }
                    }
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                }
            };
            if (mContext.bindService(intent, conn, Context.BIND_AUTO_CREATE)) {
                mScreenshotConnection = conn;
                H.postDelayed(mScreenshotTimeout, 10000);
            }
        }
    }
    private Handler H = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {

            }
        }
    };
}

