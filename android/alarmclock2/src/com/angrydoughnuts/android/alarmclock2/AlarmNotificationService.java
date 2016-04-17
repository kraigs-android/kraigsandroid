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
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.ArrayMap;
import android.util.Log;

import java.util.HashSet;
import java.util.Calendar;
import java.util.TimeZone;

public class AlarmNotificationService extends Service {
  private static final String TAG =
    AlarmNotificationService.class.getSimpleName();
  public static void scheduleAlarmNotification(Context c, long alarmid, long tsUTC) {
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
  }

  public static void dismissAllNotifications(Context c) {
    c.stopService(new Intent(c, AlarmNotificationService.class));
  }

  public static final String COMMAND = "command";
  public static final int TRIGGER_ALARM_NOTIFICATION = 1;
  public static final int DISPLAY_NEXT_ALARM = 2;
  public static final int FIRING_ALARM_NOTIFICATION_ID = 42;
  public static final int NEXT_ALARM_NOTIFICATION_ID = 69;

  private ActiveAlarms activeAlarms = null;

  private boolean alarmFiring() { return activeAlarms != null; }

  private class ActiveAlarms {
    public PowerManager.WakeLock wakelock = null;
    public HashSet<Long> alarmids = new HashSet<Long>();
  }

  @Override
  public int onStartCommand(Intent i, int flags, int startId) {
    switch (i.hasExtra(COMMAND) ? i.getExtras().getInt(COMMAND) : -1) {
    case TRIGGER_ALARM_NOTIFICATION:
      handleTriggerAlarm(i);
      break;
    case DISPLAY_NEXT_ALARM:
      displayNextAlarm();
      if (!alarmFiring()) {
        stopSelf(startId);
      }
      break;
    default:
      if (!alarmFiring()) {
        stopSelf(startId);
      }
    }

    return START_NOT_STICKY;
  }

  private void handleTriggerAlarm(Intent i) {
    final long alarmid = i.getLongExtra(AlarmClockService.ALARM_ID, -1);

    PowerManager.WakeLock w = null;
    if (i.hasExtra(AlarmTriggerReceiver.WAKELOCK_ID)) {
      w = AlarmTriggerReceiver.consumeLock(
          i.getExtras().getInt(AlarmTriggerReceiver.WAKELOCK_ID));
    }

    if (w == null) {
      Log.w(TAG, "No wake lock present for TRIGGER_ALARM_NOTIFICATION");
    }

    if (alarmFiring()) {
        Log.i(TAG, "Already wake-locked, releasing");
        w.release();
    } else {
      activeAlarms = new ActiveAlarms();
      activeAlarms.wakelock = w;
    }
    if (!activeAlarms.alarmids.add(alarmid)) {
      Log.w(TAG, "Already received trigger for " + alarmid);
    }

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

    Intent notifyAct = (Intent) notify.clone();
    notifyAct.putExtra(AlarmClockService.ALARM_ID, alarmid);
    startActivity(notifyAct);
  }

  // TODO temp
  static int count = 0;
  private void displayNextAlarm() {
    final NotificationManager manager =
      (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    final PendingIntent tick = PendingIntent.getService(
        this, 0, new Intent(this, AlarmNotificationService.class)
        .putExtra(AlarmNotificationService.COMMAND,
                  AlarmNotificationService.DISPLAY_NEXT_ALARM), 0);

    // TODO async load?
    final Cursor c = getContentResolver().query(
        AlarmClockProvider.ALARMS_URI,
        new String[] { AlarmClockProvider.AlarmEntry.TIME },
        AlarmClockProvider.AlarmEntry.ENABLED + " == 1",
        null, null);

    if (c.getCount() == 0) {
      Log.i(TAG, "Disabled next alarm refresh");
      manager.cancel(NEXT_ALARM_NOTIFICATION_ID);
      c.close();
      ((AlarmManager)getSystemService(Context.ALARM_SERVICE)).cancel(tick);
      return;
    }

    final int index = c.getColumnIndex(AlarmClockProvider.AlarmEntry.TIME);
    long next = 0;
    while (c.moveToNext()) {
      long time = c.getLong(index);
      if (time > next) {
        next = time;
      }
    }
    c.close();

    manager.notify(
        NEXT_ALARM_NOTIFICATION_ID,
        new Notification.Builder(this)
        .setContentTitle("Next Alarm...")
        .setContentText("TS: " + next + " count: " + ++count + " now: " + System.currentTimeMillis())
        .setSmallIcon(R.drawable.ic_launcher)
        .setCategory(Notification.CATEGORY_STATUS)
        .setVisibility(Notification.VISIBILITY_PUBLIC)
        .setOngoing(true)
        .setContentIntent(
            PendingIntent.getActivity(
                this, 0, new Intent(this, AlarmClockActivity.class), 0))
        .build());

    final Calendar wake = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    // TODO
    // wake.set(Calendar.SECOND, 0);
    wake.set(Calendar.MILLISECOND, 0);
    wake.add(Calendar.SECOND, 1);
    // wake.add(Calendar.MINUTE, 1);

    ((AlarmManager)getSystemService(Context.ALARM_SERVICE)).setExact(
        AlarmManager.RTC, wake.getTimeInMillis(), tick);
  }

  @Override
  public void onDestroy() {
    if (alarmFiring()) {
      for (long alarmid : activeAlarms.alarmids) {
        // TODO, this should potentially reschedule instead of blindly disable.
        ContentValues v = new ContentValues();
        v.put(AlarmClockProvider.AlarmEntry.ENABLED, false);
        int r = getContentResolver().update(
            AlarmClockProvider.ALARMS_URI, v,
            AlarmClockProvider.AlarmEntry._ID + " == " + alarmid,
            null);
        if (r < 1) {
          Log.e(TAG, "Failed to disable " + alarmid);
        }
      }

      startService(new Intent(this, AlarmNotificationService.class)
                   .putExtra(AlarmNotificationService.COMMAND,
                             AlarmNotificationService.DISPLAY_NEXT_ALARM));

      Log.i(TAG, "Releasing wake lock");
      activeAlarms.wakelock.release();
      activeAlarms = null;
    }
  }

  @Override
  public IBinder onBind(Intent intent) { return null; }

  public static class AlarmTriggerReceiver extends BroadcastReceiver {
    public static final String WAKELOCK_ID = "wakelock_id";
    private static final ArrayMap<Integer, PowerManager.WakeLock> locks =
      new ArrayMap<Integer, PowerManager.WakeLock>();
    private static int nextid = 0;

    @Override
    public void onReceive(Context context, Intent intent) {
      final long alarmid = intent.getLongExtra(AlarmClockService.ALARM_ID, -1);

      @SuppressWarnings("deprecation")  // SCREEN_DIM_WAKE_LOCK
      PowerManager.WakeLock w =
        ((PowerManager)context.getSystemService(Context.POWER_SERVICE))
        .newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK |
                     PowerManager.ACQUIRE_CAUSES_WAKEUP, "wake id " + nextid);
      w.setReferenceCounted(false);
      w.acquire();
      locks.put(nextid, w);
      Log.i(TAG, "Acquired lock " + nextid + " for alarm " + alarmid);

      context.startService(new Intent(context, AlarmNotificationService.class)
                           .putExtra(AlarmClockService.ALARM_ID, alarmid)
                           .putExtra(COMMAND, TRIGGER_ALARM_NOTIFICATION)
                           .putExtra(WAKELOCK_ID, nextid++));
    }

    public static PowerManager.WakeLock consumeLock(int id) {
      return locks.remove(id);
    }
  }
}
