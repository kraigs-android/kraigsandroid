  package com.angrydoughnuts.android.alarmclock;
  
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.widget.Toast;
  
  public class AlarmClockService extends Service {
    private final int NOTIFICATION_ID = 1;
  
    @Override
    public void onStart(Intent intent, int startId) {
      super.onStart(intent, startId);
    }
  
    @Override
    public void onCreate() {
      super.onCreate();

      Toast.makeText(this, "Alarm Clock Service Start", Toast.LENGTH_SHORT).show();
      NotificationManager manager =
        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
      Notification notification = new Notification(
          R.drawable.icon, "Alarm Clock Service", System.currentTimeMillis());
      Intent notificationIntent = new Intent(this, AlarmClockActivity.class);
      PendingIntent launch = PendingIntent.getActivity(
          this, 0, notificationIntent, 0);
      notification.setLatestEventInfo(
          getApplicationContext(), "title", "text", launch);
      manager.notify(NOTIFICATION_ID, notification);
    }
  
    @Override
    public void onDestroy() {
      super.onDestroy();
      Toast.makeText(this, "Alarm Clock Service Stop", Toast.LENGTH_SHORT).show();
      NotificationManager manager =
        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
      manager.cancel(NOTIFICATION_ID);
    }
  
    @Override
    public IBinder onBind(Intent intent) {
      // Explicitly start this service when a client binds.  This will cause
      // the service to outlive all of its binders.
      Intent selfIntent = new Intent(getApplication(), AlarmClockService.class);
      startService(selfIntent);

      final AlarmClockInterface.Stub binder = new AlarmClockInterface.Stub() {};
      return binder;
    }
  
    @Override
    public boolean onUnbind(Intent intent) {
      // Decide if we need to explicitly shut down this service.  Normally,
      // the service would shutdown after the last un-bind.  However, since
      // we explicitly started the service in onBind (to remain persistent),
      // we must explicitly stop here when there are no alarms set.
      if (!AlarmClockActivity.getAlarmOn()) {
        stopSelf();
      }
      return super.onUnbind(intent);
    }
  }
