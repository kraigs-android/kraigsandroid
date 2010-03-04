package com.angrydoughnuts.android.alarmclock;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;

public class AlarmClockService extends Service {
  // TODO(cgallek): Move this to a utility file?
  static public boolean debug(Context c) {
    return (c.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) > 0;
  }

  private final int NOTIFICATION_ID = 1;
  private DbAccessor db;
  private PendingAlarmList pendingAlarms;
  private Notification notification;
  private Handler handler;
  private Runnable alarmStatusCallback;

  public static Uri alarmIdToUri(long alarmId) {
    return Uri.parse("alarm_id:" + alarmId);
  }

  public static long alarmUriToId(Uri uri) {
    return Long.parseLong(uri.getSchemeSpecificPart());
  }

  @Override
  public void onStart(Intent intent, int startId) {
    super.onStart(intent, startId);
  }

  @Override
  public void onCreate() {
    super.onCreate();
    if (debug(getApplicationContext())) {
      Thread.setDefaultUncaughtExceptionHandler(new LoggingUncaughtExceptionHandler("/sdcard"));
    }

    db = new DbAccessor(getApplicationContext());
    pendingAlarms = new PendingAlarmList();

    // TODO(cgallek): add a better notification icon.
    notification = new Notification(R.drawable.icon, null, 0);
    notification.flags |= Notification.FLAG_ONGOING_EVENT;

    handler = new Handler();
    alarmStatusCallback = new Runnable() {
      @Override
      public void run() {
        refreshNotification();
        int intervalMillis = 1000 * 60;  // every minute
        long now = System.currentTimeMillis();
        long next = intervalMillis - now % intervalMillis;
        handler.postDelayed(alarmStatusCallback, next);
      }
    };
    handler.post(alarmStatusCallback);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    db.closeConnections();

    handler.removeCallbacks(alarmStatusCallback);

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

  private void refreshNotification() {
    AlarmTime nextTime = pendingAlarms.nextAlarmTime();
    String nextString;
    if (nextTime != null) {
      nextString = "Next Alarm: " + nextTime.timeUntilString();
    } else {
      nextString = "No Alarms Pending";
    }

    // Make the notification launch the UI Activity when clicked.
    Intent notificationIntent = new Intent(this, AlarmClockActivity.class);
    final PendingIntent launch = PendingIntent.getActivity(this, 0,
        notificationIntent, 0);

    // TODO(cgallek); Figure out how to get the application name
    // programatically.
    notification.setLatestEventInfo(
        getApplicationContext(), "Alarm Clock", nextString, launch);

    final NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    manager.notify(NOTIFICATION_ID, notification);
  }

  public AlarmTime[] pendingAlarmTimes() {
    return pendingAlarms.pendingTimes();
  }

  public void createAlarm(AlarmTime time) {
    // TODO(cgallek): validate params??
    // Store the alarm in the persistent database.
    long alarmId = db.newAlarm(time);
    scheduleAlarm(alarmId);
  }

  public void deleteAlarm(long alarmId) {
    dismissAlarm(alarmId);
    db.deleteAlarm(alarmId);
  }

  public void scheduleAlarm(long alarmId) {
    // Schedule the next alarm.
    setAlarm(alarmId, db.alarmTime(alarmId));

    // Mark the alarm as enabled in the database.
    db.enableAlarm(alarmId, true);
  }

  public void dismissAlarm(long alarmId) {
    db.enableAlarm(alarmId, false);
    PendingIntent alarm = pendingAlarms.remove(alarmId);
    if (alarm != null) {
      AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
      alarmManager.cancel(alarm);
      alarm.cancel();
    }
    refreshNotification();
  }

  public void snoozeAlarm(long alarmId) {
    AlarmTime time = AlarmTime.snoozeInMillisUTC(db.readAlarmSettings(alarmId).getSnoozeMinutes());
    setAlarm(alarmId, time);
  }

  private void setAlarm(long alarmId, AlarmTime time) {
    // Intents are considered equal if they have the same action, data, type,
    // class, and categories.  In order to schedule multiple alarms, every
    // pending intent must be different.  This means that we must encode
    // the alarm id in the data section of the intent rather than in
    // the extras bundle.
    Intent notifyIntent = new Intent(getApplicationContext(), AlarmBroadcastReceiver.class);
    notifyIntent.setData(alarmIdToUri(alarmId));
    PendingIntent scheduleIntent =
      PendingIntent.getBroadcast(getApplicationContext(), 0, notifyIntent, 0);

    // Previous instances of this intent will be overwritten in both
    // the alarm manager and the pendingAlarms list.
    AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
    alarmManager.set(AlarmManager.RTC_WAKEUP, time.calendar().getTimeInMillis(), scheduleIntent);
    // Keep track of all scheduled alarms.
    pendingAlarms.put(alarmId, time, scheduleIntent);
    refreshNotification();
  }
}
