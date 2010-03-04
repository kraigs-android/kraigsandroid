package com.angrydoughnuts.android.alarmclock;

import java.util.TreeMap;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;

public class AlarmClockService extends Service {
  private final int NOTIFICATION_ID = 1;
  private DbAccessor db;
  private TreeMap<Long, PendingIntent> pendingAlarms;

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
    scheduleAlarm(alarmId);
  }

  public void deleteAlarm(long alarmId) {
    dismissAlarm(alarmId);
    db.deleteAlarm(alarmId);
  }

  public void scheduleAlarm(long alarmId) {
    // Schedule the next alarm.
    int minutesAfterMidnight = db.alarmTime(alarmId);
    if (minutesAfterMidnight < 0) {
      throw new IllegalStateException("Invalid timestamp stored in DB.");
    }
    long alarmTime = TimeUtil.nextLocalOccuranceInUTC(minutesAfterMidnight);
    setAlarm(alarmId, alarmTime);

    // Mark the alarm as enabled in the database.
    db.enableAlarm(alarmId, true);
  }

  public boolean dismissAlarm(long alarmId) {
    db.enableAlarm(alarmId, false);
    PendingIntent alarm = pendingAlarms.remove(alarmId);
    if (alarm != null) {
      alarm.cancel();
      return true;
    } else {
      return false;
    }
  }

  public void snoozeAlarm(long alarmId) {
    long alarmTime = TimeUtil.snoozeInUTC(db.readAlarmSettings(alarmId).getSnoozeMinutes());
    setAlarm(alarmId, alarmTime);
  }

  private void setAlarm(long alarmId, long millisUtc) {
    // Intents are considered equal if they have the same action, data, type,
    // class, and categories.  In order to schedule multiple alarms, every
    // pending intent must be different.  This means that we must encode
    // the alarm id in the data section of the intent rather than in
    // the extras bundle.
    Intent notifyIntent = new Intent(getApplicationContext(), AlarmBroadcastReceiver.class);
    notifyIntent.setData(alarmIdToUri(alarmId));
    PendingIntent scheduleIntent =
      PendingIntent.getBroadcast(getApplicationContext(), 0, notifyIntent, 0);

    AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
    alarmManager.set(AlarmManager.RTC_WAKEUP, millisUtc, scheduleIntent);
    // TODO(cgallek): make sure ID doesn't exist yet?
    // Keep track of all scheduled alarms.
    pendingAlarms.put(alarmId, scheduleIntent);
    
  }
}
