
package com.android.systemui;



import android.content.Context;
import android.content.Intent;
import android.app.IntentService;
import android.app.PendingIntent;
import android.util.Log;


import android.content.SharedPreferences;



public class TorchService extends IntentService {

    public static final String TAG = "TorchService";
    public static final String KEY_TORCH_ON = "torch_on";
    public static final String INTENT_TORCH_ON = "com.aokp.romcontrol.INTENT_TORCH_ON";
    public static final String INTENT_TORCH_OFF = "com.aokp.romcontrol.INTENT_TORCH_OFF";

    Context mContext;
    SharedPreferences prefs;
    Torch mTorch;
    
    boolean mTorchOn = false;
    
    public TorchService() {
        super("TorchService");
    }
    
    @Override
    public void onCreate() {
    	mContext = getApplicationContext();
        prefs = getApplicationContext().getSharedPreferences("torch", MODE_WORLD_READABLE);
        Log.d(TAG, "service onCreate.");
        mTorchOn = prefs.getBoolean(KEY_TORCH_ON,false);
        if (mTorchOn){ 
        	mTorch = Torch.getTorch();  // Torch should be on, so we should get an object.
        }else {
        	mTorch = null;
        }
    	super.onCreate();
    }
    
    private void torchOn(){
    	Log.d(TAG,"Trying to turn Torch On");
    	if (!mTorchOn) { // Torch is already on, so we shouldn't need to start it
    		Intent intent = new Intent(mContext, Torch.class);
    		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    		mContext.startActivity(intent);
    	}
    	if (mTorch == null){
    		do {   // torch may not return a reference until it's properly set up, so let's loop until we get one.
    			mTorch = Torch.getTorch(); // need to put some type of timer in here to prevent runaway
    		}  	while (mTorch==null);
    	}
    	SharedPreferences.Editor editor = prefs.edit();
    	editor.putBoolean(KEY_TORCH_ON,true);
    	editor.commit();
    	mTorchOn = true;
    }
    
    private void torchOff(){
    	Log.d(TAG,"Trying to turn Torch Off");
    	if (mTorch == null) {
    		Log.d(TAG,"Trying to turn Torch Off == Null!!");
    		// uh oh .. how do we turn off a null torch.
    	} else {
    		mTorch.finish();	
    		SharedPreferences.Editor editor = prefs.edit();
    		editor.putBoolean(KEY_TORCH_ON,false);
    		editor.commit();
    	}
		mTorchOn = false;
		mTorch = null;
    }
    
    @Override
    protected void onHandleIntent(Intent intent) {
    	Log.d(TAG,"onHandleIntent");
        String extra = null;
        String action = intent.getAction();

        if (action != null && action.equals(INTENT_TORCH_ON)) {
        	torchOn();
        }else if (action != null && action.equals(INTENT_TORCH_OFF)){
        	torchOff();
        }
    }
}
