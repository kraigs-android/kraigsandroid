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
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.ArrayMap;
import android.util.Log;

import java.util.Calendar;
import java.util.TimeZone;

public class AlarmNotificationService extends Service {
  public static void scheduleAlarmNotification(Context c, int id, long tsUTC) {
    // Intents are considered equal if they have the same action, data, type,
    // class, and categories.  In order to schedule multiple alarms, every
    // pending intent must be different.  This means that we must encode
    // the alarm id in the request code.
    PendingIntent schedule = PendingIntent.getBroadcast(
        c, id, new Intent(c, AlarmTriggerReceiver.class), 0);

    ((AlarmManager)c.getSystemService(Context.ALARM_SERVICE))
        .setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, tsUTC, schedule);
  }

  public static void dismissAllNotifications(Context c) {
    c.stopService(new Intent(c, AlarmNotificationService.class));
  }

  public static final String COMMAND = "command";
  public static final int TRIGGER_ALARM_NOTIFICATION = 1;
  public static final int ALARM_NOTIFICATION_ID = 69;
  private PowerManager.WakeLock wakelock = null;

  @Override
  public int onStartCommand(Intent i, int flags, int startId) {
    switch (i.hasExtra(COMMAND) ? i.getExtras().getInt(COMMAND) : -1) {
    case TRIGGER_ALARM_NOTIFICATION:
      handleTriggerAlarm(i);
      break;
    default:
      if (wakelock != null) {
        stopSelf(startId);
      }
    }

    return START_NOT_STICKY;
  }

  private void handleTriggerAlarm(Intent i) {
    PowerManager.WakeLock w = null;
    if (i.hasExtra(AlarmTriggerReceiver.WAKELOCK_ID)) {
      w = AlarmTriggerReceiver.consumeLock(
          i.getExtras().getInt(AlarmTriggerReceiver.WAKELOCK_ID));
    }
    if (w != null) {
      if (wakelock == null) {
        wakelock = w;
      } else {
        Log.i("ServiceAlarmclock", "Already wake-locked, releasing");
        w.release();
      }
    } else {
      Log.w("ServiceAlarmClock",
            "No wake lock present for TRIGGER_ALARM_NOTIFICATION");
    }

    Intent notify = new Intent(this, AlarmNotificationActivity.class)
      .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

    startForeground(
        ALARM_NOTIFICATION_ID,
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

    startActivity(notify);
  }

  @Override
  public void onDestroy() {
    if (wakelock != null) {
      Log.i("ServiceAlarmclock", "Releasing wake lock");
      wakelock.release();
      wakelock = null;
    } else {
      // TODO, this will need to go if other commands are added to the service.
      Log.w("ServiceAlarmClock", "No wake lock found when dismissing alarm");
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
      PowerManager.WakeLock w =
        ((PowerManager)context.getSystemService(Context.POWER_SERVICE))
        .newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK |
                     PowerManager.ACQUIRE_CAUSES_WAKEUP, "wake id " + nextid);
      w.setReferenceCounted(false);
      w.acquire();
      locks.put(nextid, w);
      Log.i("AlarmTriggerReceiver", "Acquired lock " + nextid);

      context.startService(new Intent(context, AlarmNotificationService.class)
                           .putExtra(COMMAND, TRIGGER_ALARM_NOTIFICATION)
                           .putExtra(WAKELOCK_ID, nextid++));
    }

    public static PowerManager.WakeLock consumeLock(int id) {
      return locks.remove(id);
    }
  }
}
