package com.angrydoughnuts.android.alarmclock;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;

public class AlarmClockService extends Service {
  // TODO(cgallek): Move this to a utility file?
  static public boolean debug(Context c) {
    return (c.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) > 0;
  }

  public final static String COMMAND_EXTRA = "command";
  public final static int COMMAND_UNKNOWN = 1;
  public final static int COMMAND_NOTIFICATION_REFRESH = 1;


  private final int NOTIFICATION_ID = 1;
  private DbAccessor db;
  private PendingAlarmList pendingAlarms;
  private Notification notification;

  public static Uri alarmIdToUri(long alarmId) {
    return Uri.parse("alarm_id:" + alarmId);
  }

  public static long alarmUriToId(Uri uri) {
    return Long.parseLong(uri.getSchemeSpecificPart());
  }

  @Override
  public void onCreate() {
    super.onCreate();
    if (debug(getApplicationContext())) {
      Thread.setDefaultUncaughtExceptionHandler(new LoggingUncaughtExceptionHandler("/sdcard"));
    }

    db = new DbAccessor(getApplicationContext());
    pendingAlarms = new PendingAlarmList(getApplicationContext());

    // TODO(cgallek): add a better notification icon.
    notification = new Notification(R.drawable.icon, null, 0);
    notification.flags |= Notification.FLAG_ONGOING_EVENT;

    refreshNotification();
    NotificationRefreshReceiver.startRefreshing(getApplicationContext());
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    db.closeConnections();

    NotificationRefreshReceiver.stopRefreshing(getApplicationContext());

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
    final Intent selfIntent = new Intent(getApplicationContext(),
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

  // TODO(cgallek): This method breaks compatibility with SDK version < 5.
  // Is there anyway around this?
  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    if (intent.hasExtra(COMMAND_EXTRA)) {
      Bundle extras = intent.getExtras();
      int command = extras.getInt(COMMAND_EXTRA, COMMAND_UNKNOWN);
      switch (command) {
        case COMMAND_NOTIFICATION_REFRESH:
          refreshNotification();
      }
    }
    return super.onStartCommand(intent, flags, startId);
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
    pendingAlarms.put(alarmId, db.alarmTime(alarmId));

    // Mark the alarm as enabled in the database.
    db.enableAlarm(alarmId, true);

    refreshNotification();
  }

  public void dismissAlarm(long alarmId) {
    db.enableAlarm(alarmId, false);
    pendingAlarms.remove(alarmId);
    refreshNotification();
  }

  public void snoozeAlarm(long alarmId) {
    // Clear the snoozed alarm.
    pendingAlarms.remove(alarmId);

    // Calculate the time for the next alarm.
    AlarmTime time = AlarmTime.snoozeInMillisUTC(db.readAlarmSettings(alarmId).getSnoozeMinutes());

    // Schedule it.
    pendingAlarms.put(alarmId, time);
    refreshNotification();
  }
}