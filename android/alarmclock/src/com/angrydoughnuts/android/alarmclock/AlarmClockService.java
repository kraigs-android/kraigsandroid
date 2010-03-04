package com.angrydoughnuts.android.alarmclock;

import java.util.Timer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
  
  public class AlarmClockService extends Service {
    private final int NOTIFICATION_ID = 1;

    // TODO(cgallek): replace this with a data provider.
    private static boolean alarmOn = false;
    public static synchronized boolean getAlarmOn() { return alarmOn; }
    public static synchronized void setAlarmOn(boolean state) { alarmOn = state; }

    private Timer timerThread;
    private Handler uiHandler;
  
    @Override
    public void onStart(Intent intent, int startId) {
      super.onStart(intent, startId);
    }
  
    @Override
    public void onCreate() {
      super.onCreate();
      timerThread = new Timer();
      // TODO(cgallek): This must be created in the main thread. Is there a way
      // to ensure this?
      uiHandler = new Handler();

      final NotificationManager manager =
        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

      // TODO(cgallek): add a better notification icon.
      Notification notification = new Notification(R.drawable.icon, null, 0);
      notification.flags |= Notification.FLAG_ONGOING_EVENT;

      // Make the notification launch the UI Activity when clicked.
      Intent notificationIntent = new Intent(this, AlarmClockActivity.class);
      PendingIntent launch = PendingIntent.getActivity(
          this, 0, notificationIntent, 0);
      // TODO(cgallek): fill in the 'next alarm in/at' text.
      notification.setLatestEventInfo(
          getApplicationContext(), "Alarm Clock", "Next Alarm in ...", launch);

      manager.notify(NOTIFICATION_ID, notification);
    }
  
    @Override
    public void onDestroy() {
      super.onDestroy();
      timerThread.cancel();  // Just in case OnUnbind was never called.

      final NotificationManager manager =
        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
      manager.cancel(NOTIFICATION_ID);
    }
  
    @Override
    public IBinder onBind(Intent intent) {
      // Explicitly start this service when a client binds.  This will cause
      // the service to outlive all of its binders.
      final Intent selfIntent = new Intent(getApplication(), AlarmClockService.class);
      startService(selfIntent);

      return new AlarmClockInterfaceStub(
          getApplicationContext(), uiHandler, timerThread);
    }

    @Override
    public boolean onUnbind(Intent intent) {
      // Decide if we need to explicitly shut down this service.  Normally,
      // the service would shutdown after the last un-bind.  However, since
      // we explicitly started the service in onBind (to remain persistent),
      // we must explicitly stop here when there are no alarms set.
      if (getAlarmOn()) {
        // Since we want the service to continue running in this case, return
        // true so that onRebind is called instead of onBind.
        return true;
      } else {
        timerThread.cancel();
        stopSelf();
        return false;
      }
    }
  }
