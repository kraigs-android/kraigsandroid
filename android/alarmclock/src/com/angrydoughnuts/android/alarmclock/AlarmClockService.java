package com.angrydoughnuts.android.alarmclock;

import java.util.Calendar;
import java.util.TreeMap;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

public class AlarmClockService extends Service {
  private final int NOTIFICATION_ID = 1;
  private DbAccessor db;
  private TreeMap<Long, PendingIntent> pendingAlarms;

  @Override
  public void onStart(Intent intent, int startId) {
    super.onStart(intent, startId);
  }

  @Override
  public void onCreate() {
    super.onCreate();

    db = new DbAccessor(getApplicationContext());
    pendingAlarms = new TreeMap<Long, PendingIntent>();

    final NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    // TODO(cgallek): add a better notification icon.
    Notification notification = new Notification(R.drawable.icon, null, 0);
    notification.flags |= Notification.FLAG_ONGOING_EVENT;

    // Make the notification launch the UI Activity when clicked.
    Intent notificationIntent = new Intent(this, AlarmClockActivity.class);
    PendingIntent launch = PendingIntent.getActivity(this, 0,
        notificationIntent, 0);
    // TODO(cgallek): fill in the 'next alarm in/at' text.
    notification.setLatestEventInfo(getApplicationContext(), "Alarm Clock",
        "Next Alarm in ...", launch);

    manager.notify(NOTIFICATION_ID, notification);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    db.closeConnections();

    final NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    manager.cancel(NOTIFICATION_ID);

    if (pendingAlarms.size() != 0) {
      throw new IllegalStateException("Service shutdown with pending alarms.");
    }
  }

  @Override
  public IBinder onBind(Intent intent) {
    // Explicitly start this service when a client binds. This will cause
    // the service to outlive all of its binders.
    final Intent selfIntent = new Intent(getApplication(),
        AlarmClockService.class);
    startService(selfIntent);

    return new AlarmClockInterfaceStub(getApplicationContext(), this);
  }

  @Override
  public boolean onUnbind(Intent intent) {
    // Decide if we need to explicitly shut down this service. Normally,
    // the service would shutdown after the last un-bind. However, since
    // we explicitly started the service in onBind (to remain persistent),
    // we must explicitly stop here when there are no alarms set.
    if (pendingAlarms.size() == 0) {
      stopSelf();
      return false;
    } else {
      // Since we want the service to continue running in this case, return
      // true so that onRebind is called instead of onBind.
      return true;

    }
  }

  public void newAlarm(int minutesAfterMidnight) {
    // TODO(cgallek): validate params??
    // Store the alarm in the persistent database.
    long alarmId = db.newAlarm(minutesAfterMidnight);

    // Schedule the next alarm.
    // TODO(cgallek): this is actually interpreted as seconds after midnight right
    // now (for testing).  Switch it to minutes eventually.
    int hour = minutesAfterMidnight % 3600;
    int minutes = (minutesAfterMidnight - (hour * 3600)) % 60;
    int seconds = (minutesAfterMidnight- (hour * 3600 + minutes * 60));
    Calendar schedule = Calendar.getInstance();
    schedule.set(Calendar.HOUR_OF_DAY, hour);
    schedule.set(Calendar.MINUTE, minutes);
    schedule.set(Calendar.SECOND, seconds);

    Calendar now = Calendar.getInstance();
    if (schedule.before(now)) {
      schedule.add(Calendar.DATE, 1);
    }

    Intent notifyIntent = new Intent(getApplicationContext(), AlarmBroadcastReceiver.class);
    notifyIntent.putExtra("task_id", alarmId);
    PendingIntent scheduleIntent =
      PendingIntent.getBroadcast(getApplicationContext(), 0, notifyIntent, 0);

    AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
    alarmManager.set(AlarmManager.RTC_WAKEUP, schedule.getTimeInMillis(), scheduleIntent);
    // TODO(cgallek): make sure ID doesn't exist yet?
    // Keep track of all scheduled alarms.
    pendingAlarms.put(alarmId, scheduleIntent);
  }

  public boolean dismissAlarm(long alarmId) {
    PendingIntent alarm = pendingAlarms.remove(alarmId);
    if (alarm != null) {
      alarm.cancel();
      return true;
    } else {
      return false;
    }
  }
}
