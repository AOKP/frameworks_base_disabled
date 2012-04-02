
package com.android.systemui.statusbar.policy.buttons;

import java.net.URISyntaxException;
import java.util.List;

import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewConfiguration;

import com.android.internal.statusbar.IStatusBarService;
import com.android.systemui.statusbar.policy.KeyButtonView;



public class ExtensibleKeyButtonView extends KeyButtonView {
	
	final static String ACTION_HOME = "**home**";
	final static String ACTION_BACK = "**back**";
	final static String ACTION_SEARCH = "**search**";
	final static String ACTION_MENU = "**menu**";
	final static String ACTION_POWER = "**power**";
	final static String ACTION_RECENTS = "**recents**";
	final static String ACTION_KILL = "**kill**";
	final static String ACTION_NULL = "**null**";

    private static final String TAG = "Key.Ext";

    IStatusBarService mBarService;
    
    public String mClickAction, mLongpress;
    
    public Handler mHandler;
    
    public int mInjectKeycode;

    public ExtensibleKeyButtonView(Context context, AttributeSet attrs) {
        this(context, attrs, ACTION_NULL,ACTION_NULL);
    }

    public ExtensibleKeyButtonView(Context context, AttributeSet attrs, String ClickAction, String Longpress) {
        super(context, attrs);
        
        mHandler = new Handler();
        mBarService = IStatusBarService.Stub.asInterface(
                ServiceManager.getService(Context.STATUS_BAR_SERVICE));
        mClickAction = ClickAction;
        mLongpress = Longpress;
        setCode(0);
        if (ClickAction != null){
        	if (ClickAction.equals("") || ClickAction.equals(ACTION_NULL)) {
        		setCode(0);
        	} else if (ClickAction.equals(ACTION_HOME)) {
        		setCode(KeyEvent.KEYCODE_HOME);
        	} else if (ClickAction.equals(ACTION_BACK)) {
        		setCode (KeyEvent.KEYCODE_BACK);
        	} else if (ClickAction.equals(ACTION_SEARCH)) {
        		setCode (KeyEvent.KEYCODE_SEARCH);
        	} else if (ClickAction.equals(ACTION_MENU)) {
        		setCode (KeyEvent.KEYCODE_MENU);
        	} else if (ClickAction.equals(ACTION_POWER)) {
        		setCode (KeyEvent.KEYCODE_POWER);
        	} else { // the remaining options need to be handled by OnClick;
        		setOnClickListener(mClickListener);
        	}
        }
        setSupportsLongPress (false);	
        if (Longpress != null) 
        	if (!Longpress.equals(ACTION_NULL)) {
        		setSupportsLongPress(true);
        		setOnLongClickListener(mLongPressListener);
        	}
        Log.d(TAG,"New Key-Action:" + ClickAction + " Long:"+ Longpress);
        Log.d(TAG,"Supports LongPress:"+ mSupportsLongpress);
    }
        
    public void injectKeyDelayed(int keycode){
        mInjectKeycode = keycode;
        mHandler.removeCallbacks(onInjectKeyDelayed);
        mHandler.postDelayed(onInjectKeyDelayed, 50);
    }

    final Runnable onInjectKeyDelayed = new Runnable() {
    	public void run() {
        	try {
        		mWindowManager.injectKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, mInjectKeycode), true);
        		mWindowManager.injectKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, mInjectKeycode), true);
        	} catch (RemoteException e) {
        			e.printStackTrace();
        	}
        }
    };
 
    Runnable mKillTask = new Runnable() {
        public void run() {
            try {
                IActivityManager mgr = ActivityManagerNative.getDefault();
                List<RunningAppProcessInfo> apps = mgr.getRunningAppProcesses();
                for (RunningAppProcessInfo appInfo : apps) {
                    int uid = appInfo.uid;
                    // Make sure it's a foreground user application (not system,
                    // root, phone, etc.)
                    if (uid >= Process.FIRST_APPLICATION_UID && uid <= Process.LAST_APPLICATION_UID
                            && appInfo.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                        // Kill the entire pid
                        if (appInfo.pkgList != null && (apps.size() > 0)) {
                            mgr.forceStopPackage(appInfo.pkgList[0]);
                        } else {
                            Process.killProcess(appInfo.pid);
                        }
                        break;
                    }
                }
            } catch (RemoteException remoteException) {
                // Do nothing; just let it go.
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
        		
        	} else if (mClickAction.equals(ACTION_KILL)) {
        		
        		mHandler.postDelayed(mKillTask, ViewConfiguration.getGlobalActionKeyTimeout());
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

        	Log.d(TAG,"At onLongClick" + mLongpress);
        	if (mLongpress == null) {
        		return true;
        	}
        	
        	if (mLongpress.equals(ACTION_NULL)|| mLongpress.equals("")) {
        		return true;    	
        	} else if (mLongpress.equals(ACTION_HOME)) {
        		injectKeyDelayed(KeyEvent.KEYCODE_HOME);
        		//mHandler.sendEmptyMessage(MESSAGE_DISMISS);
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
        	} else if (mLongpress.equals(ACTION_KILL)) {        		
        		mHandler.postDelayed(mKillTask, ViewConfiguration.getGlobalActionKeyTimeout());  
        		return true;
        	} else if (mLongpress.equals(ACTION_RECENTS)) {
        		try {
                    mBarService.toggleRecentApps();
                } catch (RemoteException e) {            	
                }
                return true;
        	} else {  // we must have a custom uri
       		 	try {
       		 		Intent intent = Intent.parseUri(mClickAction, 0);
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

}
