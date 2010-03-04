package com.angrydoughnuts.android.alarmclock;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.widget.Toast;

public class AlarmClockService extends Service {
  public final static String COMMAND_EXTRA = "command";
  public final static int COMMAND_UNKNOWN = 1;
  public final static int COMMAND_NOTIFICATION_REFRESH = 2;
  public final static int COMMAND_DEVICE_BOOT = 3;
  public final static int COMMAND_TIMEZONE_CHANGE = 4;

  private final int NOTIFICATION_ID = 1;
  private DbAccessor db;
  private PendingAlarmList pendingAlarms;
  private Notification notification;

  @Override
  public void onCreate() {
    super.onCreate();
    if (getPackageManager().checkPermission(
        "android.permission.WRITE_EXTERNAL_STORAGE", getPackageName()) ==
          PackageManager.PERMISSION_GRANTED) {
      Thread.setDefaultUncaughtExceptionHandler(
          new LoggingUncaughtExceptionHandler("/sdcard"));
    }

    db = new DbAccessor(getApplicationContext());
    pendingAlarms = new PendingAlarmList(getApplicationContext());

    // Schedule enabled alarms during initial startup.
    for (Long alarmId : db.getEnabledAlarms()) {
      if (pendingAlarms.pendingTime(alarmId) != null) {
        continue;
      }
      if (DebugUtil.isDebugMode(getApplicationContext())) {
        Toast.makeText(getApplicationContext(), "RENABLE " + alarmId, Toast.LENGTH_SHORT).show();
      }
      pendingAlarms.put(alarmId, db.readAlarmInfo(alarmId).getTime());
    }

    // TODO(cgallek): add a better notification icon.
    notification = new Notification(R.drawable.icon, null, 0);
    notification.flags |= Notification.FLAG_ONGOING_EVENT;

    ReceiverNotificationRefresh.startRefreshing(getApplicationContext());
  }

  // OnStart was depreciated in SDK 5.  It is here for backwards compatibility.
  // http://android-developers.blogspot.com/2010/02/service-api-changes-starting-with.html
  @Override
  public void onStart(Intent intent, int startId) {
    handleStart(intent, startId);
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    handleStart(intent, startId);
    return START_STICKY;
  }

  private void handleStart(Intent intent, int startId) {
    if (intent != null && intent.hasExtra(COMMAND_EXTRA)) {
      Bundle extras = intent.getExtras();
      int command = extras.getInt(COMMAND_EXTRA, COMMAND_UNKNOWN);

      final Handler handler = new Handler();
      final Runnable maybeShutdown = new Runnable() {
        @Override
        public void run() {
          if (pendingAlarms.size() == 0) {
            stopSelf();
          }
        }
      };

      switch (command) {
        case COMMAND_NOTIFICATION_REFRESH:
          refreshNotification();
          handler.post(maybeShutdown);
          break;
        case COMMAND_DEVICE_BOOT:
          handler.post(maybeShutdown);
          break;
        case COMMAND_TIMEZONE_CHANGE:
          if (DebugUtil.isDebugMode(getApplicationContext())) {
            Toast.makeText(getApplicationContext(), "TIMEZONE CHANGE, RESCHEDULING...", Toast.LENGTH_SHORT).show();
          }
          for (long alarmId : pendingAlarms.pendingAlarms()) {
            scheduleAlarm(alarmId);
            if (DebugUtil.isDebugMode(getApplicationContext())) {
              Toast.makeText(getApplicationContext(), "ALARM " + alarmId, Toast.LENGTH_SHORT).show();
            }
          }
          handler.post(maybeShutdown);
          break;
        default:
          throw new IllegalArgumentException("Unknown service command.");
      }
    }
  }

  private void refreshNotification() {
    AlarmTime nextTime = pendingAlarms.nextAlarmTime();
    String nextString;
    if (nextTime != null) {
      nextString = getString(R.string.next_alarm)
        + " " + nextTime.localizedString(getApplicationContext())
        + " (" + nextTime.timeUntilString(getApplicationContext()) + ")";
    } else {
      nextString = getString(R.string.no_pending_alarms);
    }

    // Make the notification launch the UI Activity when clicked.
    final Intent notificationIntent = new Intent(this, ActivityAlarmClock.class);
    final PendingIntent launch = PendingIntent.getActivity(this, 0,
        notificationIntent, 0);

    Context c = getApplicationContext();
    notification.setLatestEventInfo(c, getString(R.string.app_name), nextString, launch);

    final NotificationManager manager =
      (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    manager.notify(NOTIFICATION_ID, notification);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    db.closeConnections();

    ReceiverNotificationRefresh.stopRefreshing(getApplicationContext());

    final NotificationManager manager =
      (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    manager.cancel(NOTIFICATION_ID);
  }

  @Override
  public IBinder onBind(Intent intent) {
    return new AlarmClockInterfaceStub(getApplicationContext(), this);
  }

  @Override
  public boolean onUnbind(Intent intent) {
    // Decide if we need to explicitly shut down this service. Normally,
    // the service would shutdown after the last un-bind. If there are still pending
    // alarms, explicitly start the service.  If there are none, explicitly stop it.
    if (pendingAlarms.size() == 0) {
      stopSelf();
    } else {
      final Intent self = new Intent(getApplicationContext(), AlarmClockService.class);
      startService(self);
    }
    return false;
  }

  public AlarmTime pendingAlarm(long alarmId) {
    return pendingAlarms.pendingTime(alarmId);
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
    pendingAlarms.remove(alarmId);
    db.deleteAlarm(alarmId);
  }

  public void scheduleAlarm(long alarmId) {
    // Schedule the next alarm.
    pendingAlarms.put(alarmId, db.readAlarmInfo(alarmId).getTime());

    // Mark the alarm as enabled in the database.
    db.enableAlarm(alarmId, true);

    refreshNotification();
  }

  public void acknowledgeAlarm(long alarmId) {
    pendingAlarms.remove(alarmId);

    AlarmTime time = db.readAlarmInfo(alarmId).getTime();
    if (time.repeats()) {
      pendingAlarms.put(alarmId, time);
    } else {
      db.enableAlarm(alarmId, false);
    }
    refreshNotification();
  }

  public void dismissAlarm(long alarmId) {
    pendingAlarms.remove(alarmId);
    db.enableAlarm(alarmId, false);

    refreshNotification();
  }

  public void snoozeAlarm(long alarmId) {
    snoozeAlarmFor(alarmId, db.readAlarmSettings(alarmId).getSnoozeMinutes());
  }

  public void snoozeAlarmFor(long alarmId, int minutes) {
    // Clear the snoozed alarm.
    pendingAlarms.remove(alarmId);

    // Calculate the time for the next alarm.
    AlarmTime time = AlarmTime.snoozeInMillisUTC(minutes);

    // Schedule it.
    pendingAlarms.put(alarmId, time);
    refreshNotification();
  }
}