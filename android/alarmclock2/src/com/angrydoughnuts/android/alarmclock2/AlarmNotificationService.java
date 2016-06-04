/****************************************************************************
 * Copyright 2016 kraigs.android@gmail.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ****************************************************************************/

package com.angrydoughnuts.android.alarmclock2;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.os.PowerManager;
import android.text.format.DateFormat;
import android.util.ArrayMap;
import android.util.Log;

import java.util.HashSet;
import java.util.Calendar;
import java.util.TimeZone;

public class AlarmNotificationService extends Service {
  public static final String ALARM_ID = "alarm_id";
  private static final String TIME_UTC = "time_utc";

  public static long newAlarm(Context c, int secondsPastMidnight) {

    ContentValues v = new ContentValues();
    v.put(AlarmClockProvider.AlarmEntry.TIME, secondsPastMidnight);
    Uri u = c.getContentResolver().insert(AlarmClockProvider.ALARMS_URI, v);
    long alarmid = ContentUris.parseId(u);
    Log.i(TAG, "New alarm: " + alarmid + " (" + u +")");

    // Inserted entry is ENABLED by default with no options.  Schedule the
    // first occurrence.
    Calendar ts = TimeUtil.nextOccurrence(secondsPastMidnight);
    scheduleAlarmTrigger(c, alarmid, ts.getTimeInMillis());

    return alarmid;
  }

  public static void scheduleAlarmTrigger(
      Context c, long alarmid, long tsUTC) {
    c.startService(new Intent(c, AlarmNotificationService.class)
                   .putExtra(COMMAND, SCHEDULE_TRIGGER)
                   .putExtra(ALARM_ID, alarmid)
                   .putExtra(TIME_UTC, tsUTC));
  }

  public static void removeAlarmTrigger(Context c, long alarmid) {
    c.startService(new Intent(c, AlarmNotificationService.class)
                   .putExtra(COMMAND, REMOVE_TRIGGER)
                   .putExtra(ALARM_ID, alarmid));
  }

  public static void dismissAllAlarms(Context c) {
    c.startService(new Intent(c, AlarmNotificationService.class)
                   .putExtra(COMMAND, DISMISS_ALL));
  }

  public static void snoozeAllAlarms(Context c, long snoozeUtc) {
    c.startService(new Intent(c, AlarmNotificationService.class)
                   .putExtra(COMMAND, SNOOZE_ALL)
                   .putExtra(TIME_UTC, snoozeUtc));
  }

  private ActiveAlarms activeAlarms = null;

  @Override
  public int onStartCommand(Intent i, int flags, int startId) {
    long alarmid;
    long ts;

    switch (i.hasExtra(COMMAND) ? i.getExtras().getInt(COMMAND) : -1) {
    case TRIGGER_ALARM_NOTIFICATION:
      handleTriggerAlarm(i);
      return START_NOT_STICKY;
    case DISMISS_ALL:
      dismissAll();
      stopSelf();
      return START_NOT_STICKY;
    case SNOOZE_ALL:
      ts = i.getLongExtra(TIME_UTC, -1);
      snoozeAll(ts);
      stopSelf();
      return START_NOT_STICKY;
    case SCHEDULE_TRIGGER:
      alarmid = i.getLongExtra(ALARM_ID, -1);
      ts = i.getLongExtra(TIME_UTC, -1);
      scheduleTrigger(alarmid, ts);
      break;
    case REMOVE_TRIGGER:
      alarmid = i.getLongExtra(ALARM_ID, -1);
      removeTrigger(alarmid);
      break;
    case REFRESH:
      refreshNotifyBar();
      break;
    }

    if (activeAlarms == null)
      stopSelf(startId);

    return START_NOT_STICKY;
  }

  @Override
  public void onDestroy() {
    if (activeAlarms == null)
      return;

    if (!activeAlarms.alarmids.isEmpty())
      Log.w(TAG, "Releasing wake lock with active alarms! (" +
            activeAlarms.alarmids.size() + ")");
    Log.i(TAG, "Releasing wake lock");
    activeAlarms.wakelock.release();
    activeAlarms = null;
  }

  @Override
  public IBinder onBind(Intent intent) { return null; }

  private void handleTriggerAlarm(Intent i) {
    final long alarmid = i.getLongExtra(ALARM_ID, -1);

    PowerManager.WakeLock w = null;
    if (i.hasExtra(AlarmTriggerReceiver.WAKELOCK_ID)) {
      w = AlarmTriggerReceiver.consumeLock(
          i.getExtras().getInt(AlarmTriggerReceiver.WAKELOCK_ID));
    }

    if (w == null)
      Log.e(TAG, "No wake lock present for TRIGGER_ALARM_NOTIFICATION");

    if (activeAlarms == null) {
      activeAlarms = new ActiveAlarms();
      activeAlarms.wakelock = w;
    } else {
      Log.i(TAG, "Already wake-locked, releasing extra lock");
      w.release();
    }
    activeAlarms.alarmids.add(alarmid);

    Intent notify = new Intent(this, AlarmNotificationActivity.class)
      .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

    final Notification notification =
      new Notification.Builder(this)
      .setContentTitle("Alarming...")
      .setContentText("Second line...")
      .setSmallIcon(R.drawable.ic_launcher)
      .setContentIntent(PendingIntent.getActivity(this, 0, notify, 0))
      .setCategory(Notification.CATEGORY_ALARM)
      .setPriority(Notification.PRIORITY_MAX)
      .setVisibility(Notification.VISIBILITY_PUBLIC)
      .setOngoing(true)
      // TODO replace each of these with an explicit below
      .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS)
      // TODO
      // .setLights()
      // .setSound()
      // .setVibrate()
      .build();
    notification.flags |= Notification.FLAG_INSISTENT;  // Loop sound
    startForeground(FIRING_ALARM_NOTIFICATION_ID, notification);

    refreshNotifyBar();

    Intent notifyAct = (Intent) notify.clone();
    notifyAct.putExtra(ALARM_ID, alarmid);
    startActivity(notifyAct);
  }

  private void dismissAll() {
    if (activeAlarms == null) {
      Log.w(TAG, "No active alarms when dismissed");
      return;
    }

    for (long alarmid : activeAlarms.alarmids) {
      ContentValues v = new ContentValues();
      v.put(AlarmClockProvider.AlarmEntry.ENABLED, false);
      v.put(AlarmClockProvider.AlarmEntry.NEXT_SNOOZE, 0);
      int r = getContentResolver().update(
          ContentUris.withAppendedId(AlarmClockProvider.ALARMS_URI, alarmid),
          v, null, null);
      if (r < 1) {
        Log.e(TAG, "Failed to disable " + alarmid);
      }
    }

    activeAlarms.alarmids.clear();
    refreshNotifyBar();
  }

  private void snoozeAll(long snoozeUtc) {
    if (activeAlarms == null) {
      Log.w(TAG, "No active alarms when snoozed");
      return;
    }

    for (long alarmid : activeAlarms.alarmids) {
      ContentValues v = new ContentValues();
      v.put(AlarmClockProvider.AlarmEntry.NEXT_SNOOZE, snoozeUtc);
      int r = getContentResolver().update(
          ContentUris.withAppendedId(AlarmClockProvider.ALARMS_URI, alarmid),
          v, null, null);
      if (r < 1) {
        Log.e(TAG, "Failed to snooze " + alarmid);
      }
      scheduleTrigger(alarmid, snoozeUtc);
    }

    activeAlarms.alarmids.clear();
    refreshNotifyBar();
  }

  private void scheduleTrigger(long alarmid, long tsUTC) {
    // Intents are considered equal if they have the same action, data, type,
    // class, and categories.  In order to schedule multiple alarms, every
    // pending intent must be different.  This means that we must encode
    // the alarm id in the request code.
    PendingIntent schedule = PendingIntent.getBroadcast(
        this, (int)alarmid, new Intent(this, AlarmTriggerReceiver.class)
        .putExtra(ALARM_ID, alarmid), 0);

    ((AlarmManager)getSystemService(Context.ALARM_SERVICE))
        .setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, tsUTC, schedule);
    refreshNotifyBar();
  }

  private void removeTrigger(long alarmid) {
    PendingIntent schedule = PendingIntent.getBroadcast(
        this, (int)alarmid, new Intent(this, AlarmTriggerReceiver.class)
        .putExtra(ALARM_ID, alarmid), 0);

    ((AlarmManager)getSystemService(Context.ALARM_SERVICE)).cancel(schedule);
    refreshNotifyBar();
  }

  private void refreshNotifyBar() {
    final NotificationManager manager =
      (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    final PendingIntent tick = PendingIntent.getService(
        this, 0, new Intent(this, AlarmNotificationService.class)
        .putExtra(AlarmNotificationService.COMMAND,
                  AlarmNotificationService.REFRESH), 0);

    final Cursor c = getContentResolver().query(
        AlarmClockProvider.ALARMS_URI,
        new String[] { AlarmClockProvider.AlarmEntry.TIME,
                       AlarmClockProvider.AlarmEntry.NEXT_SNOOZE },
        AlarmClockProvider.AlarmEntry.ENABLED + " == 1",
        null, null);

    if (c.getCount() == 0 ||
        (activeAlarms != null && !activeAlarms.alarmids.isEmpty())) {
      manager.cancel(NEXT_ALARM_NOTIFICATION_ID);
      c.close();
      ((AlarmManager)getSystemService(Context.ALARM_SERVICE)).cancel(tick);
      return;
    }

    final Calendar now = Calendar.getInstance();
    Calendar next = null;
    while (c.moveToNext()) {
      Calendar n = TimeUtil.nextOccurrence(
          now,
          c.getInt(c.getColumnIndex(AlarmClockProvider.AlarmEntry.TIME)),
          c.getLong(c.getColumnIndex(AlarmClockProvider.AlarmEntry.NEXT_SNOOZE)));
      if (next == null || n.before(next))
        next = n;
    }
    c.close();

    manager.notify(
        NEXT_ALARM_NOTIFICATION_ID,
        new Notification.Builder(this)
        .setContentTitle("Next Alarm...")
        .setContentText(TimeUtil.formatLong(this, next) + " in " + TimeUtil.until(next))
        .setSmallIcon(R.drawable.ic_launcher)
        .setCategory(Notification.CATEGORY_STATUS)
        .setVisibility(Notification.VISIBILITY_PUBLIC)
        .setOngoing(true)
        .setContentIntent(
            PendingIntent.getActivity(
                this, 0, new Intent(this, AlarmClockActivity.class), 0))
        .build());

    final Calendar wake = TimeUtil.nextMinute();
    ((AlarmManager)getSystemService(Context.ALARM_SERVICE)).setExact(
        AlarmManager.RTC, wake.getTimeInMillis(), tick);
  }

  private static final String TAG =
    AlarmNotificationService.class.getSimpleName();
  // Commands
  private static final String COMMAND = "command";
  private static final int TRIGGER_ALARM_NOTIFICATION = 1;
  private static final int SCHEDULE_TRIGGER = 2;
  private static final int REMOVE_TRIGGER = 3;
  private static final int DISMISS_ALL = 4;
  private static final int SNOOZE_ALL = 5;
  private static final int REFRESH = 6;
  // Notification ids
  private static final int FIRING_ALARM_NOTIFICATION_ID = 42;
  private static final int NEXT_ALARM_NOTIFICATION_ID = 69;

  private static class ActiveAlarms {
    public PowerManager.WakeLock wakelock = null;
    public HashSet<Long> alarmids = new HashSet<Long>();
  }

  public static class AlarmTriggerReceiver extends BroadcastReceiver {
    public static final String WAKELOCK_ID = "wakelock_id";
    private static final ArrayMap<Integer, PowerManager.WakeLock> locks =
      new ArrayMap<Integer, PowerManager.WakeLock>();
    private static int nextid = 0;

    @Override
    public void onReceive(Context c, Intent i) {
      final long alarmid = i.getLongExtra(ALARM_ID, -1);

      @SuppressWarnings("deprecation")  // SCREEN_DIM_WAKE_LOCK
      PowerManager.WakeLock w =
        ((PowerManager)c.getSystemService(Context.POWER_SERVICE))
        .newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK |
                     PowerManager.ACQUIRE_CAUSES_WAKEUP, "wake id " + nextid);
      w.setReferenceCounted(false);
      w.acquire();
      locks.put(nextid, w);
      Log.i(TAG, "Acquired lock " + nextid + " for alarm " + alarmid);

      c.startService(new Intent(c, AlarmNotificationService.class)
                     .putExtra(ALARM_ID, alarmid)
                     .putExtra(COMMAND, TRIGGER_ALARM_NOTIFICATION)
                     .putExtra(WAKELOCK_ID, nextid++));
    }

    public static PowerManager.WakeLock consumeLock(int id) {
      return locks.remove(id);
    }
  }
}
