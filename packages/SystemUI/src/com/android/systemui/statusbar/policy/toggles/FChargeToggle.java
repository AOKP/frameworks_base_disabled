
package com.android.systemui.statusbar.policy.toggles;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.util.Log;

import com.android.systemui.R;

public class FChargeToggle extends Toggle {
	
	public static final String FAST_CHARGE_DIR = "/sys/kernel/fast_charge";
	public static final String FAST_CHARGE_FILE = "force_fast_charge";

    public FChargeToggle(Context context) {
        super(context);

        updateState();
        setLabel(R.string.toggle_fcharge);
        if (mToggle.isChecked())
        	setIcon(R.drawable.toggle_fcharge);
        else
        	setIcon(R.drawable.toggle_fcharge_off);
        IntentFilter f = new IntentFilter();
        f.addAction("com.liquid.control.FCHARGE_CHANGED");
        context.registerReceiver(mIntentReceiver, f);
        
    }

    @Override
    protected void updateInternalToggleState() {
    	mToggle.setChecked(isFastChargeOn());
        if (mToggle.isChecked())
        	setIcon(R.drawable.toggle_fcharge);
        else
        	setIcon(R.drawable.toggle_fcharge_off);
    }

    @Override
    protected void onCheckChanged(boolean isChecked) {
    	updateFastCharge(isChecked);
        if (isChecked)
        	setIcon(R.drawable.toggle_fcharge);
        else
        	setIcon(R.drawable.toggle_fcharge_off);
    }
    
    @Override
    protected boolean onLongPress() {
    	return false;
    }
    
    public boolean isFastChargeOn(){
    	try{
    		File fastcharge = new File(FAST_CHARGE_DIR,FAST_CHARGE_FILE);
    		FileReader reader = new FileReader(fastcharge);
    		BufferedReader breader = new BufferedReader(reader);
    		return (breader.readLine().equals("1"));
    	} catch (IOException e) {
    		Log.e("FChargeToggle","Couldn't read fast_charge file");
    		return false;
    	}
    }
    
    public void updateFastCharge(boolean on){
    	try{
    		File fastcharge = new File(FAST_CHARGE_DIR,FAST_CHARGE_FILE);
    		FileWriter fwriter = new FileWriter(fastcharge);
    		BufferedWriter bwriter = new BufferedWriter(fwriter);
    		bwriter.write(on ? "1" : "0");
    		bwriter.close();
    	} catch (IOException e) {
    		Log.e("FChargeToggle","Couldn't write fast_charge file");
    	}	
    	
    }
    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
    	@Override
    	public void onReceive(Context context, Intent intent) {
    		String action = intent.getAction();
    		if (action.equals("com.liquid.control.FCHARGE_CHANGED")) {
    			updateState();
    		}
    }
    };
}
