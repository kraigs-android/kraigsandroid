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
  public static long newAlarm(Context c, int secondsPastMidnight) {

    ContentValues v = new ContentValues();
    v.put(AlarmClockProvider.AlarmEntry.TIME, secondsPastMidnight);
    // TODO handle error ??
    Uri u = c.getContentResolver().insert(AlarmClockProvider.ALARMS_URI, v);
    long alarmid = ContentUris.parseId(u);
    Log.i(TAG, "New alarm: " + alarmid + " (" + u +")");

    // Inserted entry is ENABLED by default with no options.  Schedule the
    // first occurrence.
    Calendar ts = TimeUtil.nextOccurrence(secondsPastMidnight);
    scheduleAlarmNotification(c, alarmid, ts.getTimeInMillis());
    refreshNotifyBar(c);

    return alarmid;
  }

  public static void scheduleAlarmNotification(
      Context c, long alarmid, long tsUTC) {
    // Intents are considered equal if they have the same action, data, type,
    // class, and categories.  In order to schedule multiple alarms, every
    // pending intent must be different.  This means that we must encode
    // the alarm id in the request code.
    PendingIntent schedule = PendingIntent.getBroadcast(
        c, (int)alarmid,
        new Intent(c, AlarmTriggerReceiver.class)
        .putExtra(AlarmClockService.ALARM_ID, alarmid), 0);

    ((AlarmManager)c.getSystemService(Context.ALARM_SERVICE))
        .setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, tsUTC, schedule);
    refreshNotifyBar(c);
  }

  public static void removeAlarmNotification(Context c, long alarmid) {
    PendingIntent schedule = PendingIntent.getBroadcast(
        c, (int)alarmid,
        new Intent(c, AlarmTriggerReceiver.class)
        .putExtra(AlarmClockService.ALARM_ID, alarmid), 0);
    ((AlarmManager)c.getSystemService(Context.ALARM_SERVICE)).cancel(schedule);
    refreshNotifyBar(c);
  }

  private static void refreshNotifyBar(Context c) {
    c.startService(new Intent(c, AlarmNotificationService.class)
                   .putExtra(AlarmNotificationService.COMMAND,
                             AlarmNotificationService.UPDATE_LOOP));
  }

  private ActiveAlarms activeAlarms = null;

  @Override
  public int onStartCommand(Intent i, int flags, int startId) {
    switch (i.hasExtra(COMMAND) ? i.getExtras().getInt(COMMAND) : -1) {
    case TRIGGER_ALARM_NOTIFICATION:
      handleTriggerAlarm(i);
      return START_NOT_STICKY;
    case UPDATE_LOOP:
      displayNextAlarm();
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

    for (long alarmid : activeAlarms.alarmids) {
      // TODO, this should potentially reschedule instead of blindly disable.
      ContentValues v = new ContentValues();
      v.put(AlarmClockProvider.AlarmEntry.ENABLED, false);
      int r = getContentResolver().update(
          ContentUris.withAppendedId(AlarmClockProvider.ALARMS_URI, alarmid),
          v, null, null);
      if (r < 1) {
        Log.e(TAG, "Failed to disable " + alarmid);
      }
    }

    refreshNotifyBar(this);

    Log.i(TAG, "Releasing wake lock");
    activeAlarms.wakelock.release();
    activeAlarms = null;
  }

  @Override
  public IBinder onBind(Intent intent) { return null; }

  private void handleTriggerAlarm(Intent i) {
    final long alarmid = i.getLongExtra(AlarmClockService.ALARM_ID, -1);

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
      Log.i(TAG, "Already wake-locked, releasing");
      w.release();
    }
    activeAlarms.alarmids.add(alarmid);

    Intent notify = new Intent(this, AlarmNotificationActivity.class)
      .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

    startForeground(
        FIRING_ALARM_NOTIFICATION_ID,
        new Notification.Builder(this)
          .setContentTitle("Alarming...")
          .setContentText("Second line...")
          .setSmallIcon(R.drawable.ic_launcher)
          .setCategory(Notification.CATEGORY_ALARM)
          .setPriority(Notification.PRIORITY_MAX)
          .setVisibility(Notification.VISIBILITY_PUBLIC)
          .setOngoing(true)
          // TODO
          // .setLights()
          // .setSound() ???
          .setContentIntent(PendingIntent.getActivity(this, 0, notify, 0))
          .build());

    refreshNotifyBar(this);

    Intent notifyAct = (Intent) notify.clone();
    notifyAct.putExtra(AlarmClockService.ALARM_ID, alarmid);
    startActivity(notifyAct);
  }

  private void displayNextAlarm() {
    final NotificationManager manager =
      (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    final PendingIntent tick = PendingIntent.getService(
        this, 0, new Intent(this, AlarmNotificationService.class)
        .putExtra(AlarmNotificationService.COMMAND,
                  AlarmNotificationService.UPDATE_LOOP), 0);

    final Cursor c = getContentResolver().query(
        AlarmClockProvider.ALARMS_URI,
        new String[] { AlarmClockProvider.AlarmEntry.TIME },
        AlarmClockProvider.AlarmEntry.ENABLED + " == 1",
        null, null);

    if (c.getCount() == 0 || activeAlarms != null) {
      Log.i(TAG, "Stopping notification refresh loop");
      manager.cancel(NEXT_ALARM_NOTIFICATION_ID);
      c.close();
      ((AlarmManager)getSystemService(Context.ALARM_SERVICE)).cancel(tick);
      return;
    }

    final int index = c.getColumnIndex(AlarmClockProvider.AlarmEntry.TIME);
    final Calendar now = Calendar.getInstance();
    Calendar next = null;
    while (c.moveToNext()) {
      Calendar n = TimeUtil.nextOccurrence(now, c.getInt(index));
      if (next == null || next.before(n))
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
  private static final int UPDATE_LOOP = 2;
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
      final long alarmid = i.getLongExtra(AlarmClockService.ALARM_ID, -1);

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
                     .putExtra(AlarmClockService.ALARM_ID, alarmid)
                     .putExtra(COMMAND, TRIGGER_ALARM_NOTIFICATION)
                     .putExtra(WAKELOCK_ID, nextid++));
    }

    public static PowerManager.WakeLock consumeLock(int id) {
      return locks.remove(id);
    }
  }
}
