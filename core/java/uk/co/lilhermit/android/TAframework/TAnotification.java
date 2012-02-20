package uk.co.lilhermit.android.TAframework;

import java.util.ArrayList;
import android.content.ComponentName;
import android.app.Notification;
import android.content.Intent;
import android.content.Context;
import android.os.RemoteException;
import android.os.IBinder;
import android.util.Log;
import android.content.ServiceConnection;
import android.app.INotificationManager;
import android.app.NotificationManager;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.DeadObjectException;

import uk.co.lilhermit.android.TrackballAlert.service.iTAservice;

/**
@hide
**/
public class TAnotification
{
	private String _packagename;

	private ArrayList<queue_object> queue = new ArrayList<queue_object>();
	private iTAservice mTAservice = null;
	private Context mContext;
	Intent serviceIntent;
	private final static int version = 23;
	private final static String LOGTAG = "TAframework";
	private final static int ACTION_NOTIFY 		=	0x01;
	private final static int ACTION_CANCEL 		=	0x02;
	private final static int ACTION_CANCEL_ALL 	=	0x03;

	private boolean connected = false;
	private boolean bound = false;
	private boolean bind_pending = false;
	
	final String ProPackageName = "uk.co.lilhermit.android.TrackballAlertPro";
	final String FreePackageName = "uk.co.lilhermit.android.TrackballAlert";
	private SharedPreferences Prefs = null;
	private Context TAcontext = null;

	public TAnotification(Context context)
	{
		mContext=context;
		_packagename = mContext.getPackageName();

		serviceIntent = new Intent("uk.co.lilhermit.android.TrackballAlert.service.iTAservice");
		startService();
		PackageInfo pInfo;
		try
		{
			TAcontext = context.createPackageContext(ProPackageName,Context.MODE_WORLD_READABLE|Context.CONTEXT_IGNORE_SECURITY);
			pInfo = TAcontext.getPackageManager().getPackageInfo(ProPackageName,PackageManager.GET_META_DATA);
			if(pInfo.versionCode<10203)
				LogE("Framework requires atleast Trackball Alert Pro v1.2.3");
		}
		catch (NameNotFoundException e1)
		{  // PRO Package not found - test for Free!
			try
			{
				TAcontext = context.createPackageContext(FreePackageName,Context.MODE_WORLD_READABLE|Context.CONTEXT_IGNORE_SECURITY);
				pInfo = TAcontext.getPackageManager().getPackageInfo(FreePackageName,PackageManager.GET_META_DATA);
				if(pInfo.versionCode<10103)
					LogE("Framework requires atleast Trackball Alert v1.1.3");

			}
			catch (NameNotFoundException e2)
			{
				// No package Found!!
			}

		}
		if(TAcontext!=null)
			Prefs = TAcontext.getSharedPreferences("settings", Context.MODE_WORLD_READABLE);
	}

	// Queue functions
	public void queueNotify(String tag, int id, Notification notification)
	{
		LogV(String.format("queueNotify(tag,id,notification) - %s %d %s",_packagename,id,notification.tickerText));

		if(!connected) // Server did not start so just send the notification as it is.
		{
			LogV("Just sending: Notify");
			Notify(tag,id,notification); 
		}
		else
		{
			if(!bound)
			{
				queue.add(new queue_object(ACTION_NOTIFY, tag, id, notification));
				Bind();
			}
			else
			{
				try {
					notification = mTAservice.process_notification(notification,_packagename,id);
					Notify(tag,id,notification); 
				} catch (DeadObjectException e) {
					bound = false;
					queueNotify(tag, id, notification);
				}
				catch (Exception e2){}
			}
		}
	}

	public void queueCancel(String tag, int id)
	{
		LogV(String.format("queueCancel(tag,id) - %s %d",_packagename,id));

		if(!connected) // Server did not start so just send the notification as it is.
		{
			LogV("Just sending: Cancel");
			Cancel(tag,id); 
		}
		else
		{
			if(!bound)
			{
				queue.add(new queue_object(ACTION_CANCEL, tag, id));
				Bind();
			}
			else
				Cancel(tag,id);

		}
	}

	public void queueCancelAll()
	{
		LogV(String.format("queueCancelAll() - %s",_packagename));

		if(!connected) // Server did not start so just send the notification as it is.
		{
			LogV("Just sending: CancelAll");
			CancelAll(); 
		}
		else
		{
			if(!bound)
			{
				queue.add(new queue_object(ACTION_CANCEL_ALL));
				Bind();
			}
			else
				CancelAll();
		}
	}

	private boolean startService()
	{
		if(mContext.startService(serviceIntent)!=null)
			connected=true;
		return connected;
	}

	private void Bind()
	{
		if(!bind_pending)
		{
			bind_pending=true;
			mContext.bindService(serviceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);	
		}
	}

	public void finish()
	{
		if(bound)
		{
			try {
			mContext.unbindService(mServiceConnection);
			}
			catch (IllegalArgumentException e){}
		}
		LogV("Unbinding - "+_packagename);
	}

	static public int getVersion()
	{
		return version;
	}

	private void process_queue()
	{

		LogV("Processing queue for:"+_packagename);
		for(queue_object item : queue)
		{
			switch (item._action) {
			case ACTION_NOTIFY:
				try {
					item._notification = mTAservice.process_notification(item._notification,_packagename,item._id);
				} catch (Exception e) {}
				Notify(item._tag, item._id, item._notification);
				break;
			case ACTION_CANCEL:
				Cancel(item._tag, item._id);
				break;
			case ACTION_CANCEL_ALL:
				CancelAll();
				break;
			default:
				break;
			}
		}
		queue.clear();
		finish();
	}

	private void Notify(String tag, int id, Notification notification)
	{
		int[] idOut = new int[1];

		LogV(String.format("Notify - %s %d %s",_packagename,id,notification.tickerText));

		boolean localLOGV = false || android.util.Config.LOGV;

		if (localLOGV) Log.v("NotificationManager", _packagename + ": notify(" + id + ", " + notification + ")");
		try {
			INotificationManager _service = NotificationManager.getService();
			_service.enqueueNotificationWithTag(_packagename, tag, id, notification, idOut);
			if (id != idOut[0]) {
				Log.w("NotificationManager", "notify: id corrupted: sent " + id + ", got back " + idOut[0]);
			}
		} catch (RemoteException e) {
		}
	}

	private void Cancel(String tag, int id)
	{
		LogV(String.format("Cancel: - %s %d",_packagename,id));


		boolean localLOGV = false || android.util.Config.LOGV;
		if (localLOGV) Log.v("NotificationManager", _packagename + ": cancel(" + id + ")");
		try {
			INotificationManager _service = NotificationManager.getService();

			_service.cancelNotificationWithTag(_packagename, tag, id);
		} catch (RemoteException e) {
		}
	}

	private void CancelAll()
	{
		LogV(String.format("CancelAll - %s",_packagename));
		boolean localLOGV = false || android.util.Config.LOGV;
		if (localLOGV) Log.v("NotificationManager", _packagename + ": cancelAll()");
		try {
			INotificationManager _service = NotificationManager.getService();

			_service.cancelAllNotifications(_packagename);
		} catch (RemoteException e) {
		}
	}

	private ServiceConnection mServiceConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) 
		{ 
			LogV("ServiceConnected - "+_packagename);

			mTAservice = iTAservice.Stub.asInterface(service);
			bound = true;
			bind_pending=false;
			process_queue();
		}

		public void onServiceDisconnected(ComponentName className) {
			LogV("ServiceDisconnected - "+_packagename);

			mTAservice = null;
			bound = false;
		}
	};

	private void LogV(String msg) {
		msg=String.format("[framework] %s",msg);
		if(DEBUG())
			android.util.Log.v(LOGTAG, msg);
	}

	private void LogE(String msg) {
		msg=String.format("[framework] %s",msg);
		android.util.Log.e(LOGTAG, msg);
	}

	private void LogD(String msg) {
		msg=String.format("[framework] %s",msg);
		if(DEBUG())
			android.util.Log.d(LOGTAG, msg);
	}
	
	private boolean DEBUG()
	{
		if(Prefs!=null)
			return Prefs.getBoolean("FrameworkDEBUG", false);
		else
			return false;
	}

	private class queue_object
	{
		public int _action;
		public String _tag;
		public int _id;
		public Notification _notification;

		public queue_object(int action, String tag, int id, Notification notification)
		{
			_action=action;
			_tag=tag;
			_id=id;
			_notification=notification;
		}

		public queue_object(int action, String tag, int id)
		{
			_action=action;
			_tag=tag;
			_id=id;
		}

		public queue_object(int action)
		{
			_action=action;
		}

	}

}

