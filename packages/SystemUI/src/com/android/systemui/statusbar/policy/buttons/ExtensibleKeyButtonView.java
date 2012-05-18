
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
import com.android.systemui.R;



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
        	if (ClickAction.equals(ACTION_HOME)) {
        		setCode(KeyEvent.KEYCODE_HOME);
        		setId(R.id.home);
        	} else if (ClickAction.equals(ACTION_BACK)) {
        		setCode (KeyEvent.KEYCODE_BACK);
        		setId(R.id.back);
        	} else if (ClickAction.equals(ACTION_SEARCH)) {
        		setCode (KeyEvent.KEYCODE_SEARCH);
        		setId(R.id.search);
        	} else if (ClickAction.equals(ACTION_MENU)) {
        		setCode (KeyEvent.KEYCODE_MENU);
        		setId(R.id.menu_big);
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
        mHandler.removeCallbacks(onInjectKeyDelayed);
        mHandler.postDelayed(onInjectKeyDelayed, 0);
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
        	if (mLongpress == null) {
        		return true;
        	}
        	if (mLongpress.equals(ACTION_NULL)) {
        		// attempt to keep long press functionality of 'keys' if
        		// they haven't been overridden.
        		return false;    	
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
        	} else if (mLongpress.equals(ACTION_KILL)) {        		
        		mHandler.postDelayed(mKillTask, 0);  
        		return true;
        	} else if (mLongpress.equals(ACTION_RECENTS)) {
        		try {
                    mBarService.toggleRecentApps();
                } catch (RemoteException e) {   
                	// let it go.
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

}
