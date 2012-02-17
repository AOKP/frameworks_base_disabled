
package uk.co.lilhermit.android.TrackballAlert.service;

import android.app.Notification;

/**
@hide
**/
interface iTAservice {
    Notification process_notification(in Notification notification, in String packagename, in int identifier );

}
