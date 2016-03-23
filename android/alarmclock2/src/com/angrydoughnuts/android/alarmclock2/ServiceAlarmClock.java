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

public class ServiceAlarmClock extends Service {
  // TODO: this isn't handled properly if the service is destroyed
  // while holding a wake lock.  Create a separate service which is
  // start()-ed ?
  private PowerManager.WakeLock wakelock = null;

  public class IdentityBinder extends Binder {
    public ServiceAlarmClock getService() {
      return ServiceAlarmClock.this;
    }
  }

  @Override
  public IBinder onBind(Intent intent) {
    return new IdentityBinder();
  }

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

      context.startService(new Intent(context, ServiceAlarmClock.class)
                           .putExtra(COMMAND, TRIGGER_ALARM_NOTIFICATION)
                           .putExtra(WAKELOCK_ID, nextid++));
    }

    public static PowerManager.WakeLock consumeLock(int id) {
      return locks.remove(id);
    }
  }

  public static final String COMMAND = "command";
  public static final int TRIGGER_ALARM_NOTIFICATION = 1;

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    if (intent.hasExtra(COMMAND)) {
      switch (intent.getExtras().getInt(COMMAND)) {
      case TRIGGER_ALARM_NOTIFICATION:
        handleTriggerAlarm(intent);
        break;
      }
    }

    stopSelf(startId);
    return START_NOT_STICKY;
  }

  // TODO: temp
  private static int id = 0;
  public void createTestAlarm() {
    final long timeUTC = System.currentTimeMillis() + 5000;

    final Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    c.setTimeInMillis(timeUTC);
    c.set(Calendar.HOUR_OF_DAY, 0);
    c.set(Calendar.MINUTE, 0);
    c.set(Calendar.SECOND, 0);
    c.set(Calendar.MILLISECOND, 0);

    ContentValues v = new ContentValues();
    v.put(
        ProviderAlarmClock.AlarmEntry.TIME,
        (timeUTC - c.getTimeInMillis()) / 1000);
    // TODO handle error ??
    Uri u = getContentResolver().insert(ProviderAlarmClock.ALARMS_URI, v);
    Log.i("ServiceAlarmClock", "New alarm: " + u);

    // Intents are considered equal if they have the same action, data, type,
    // class, and categories.  In order to schedule multiple alarms, every
    // pending intent must be different.  This means that we must encode
    // the alarm id in the request code.
    PendingIntent schedule = PendingIntent.getBroadcast(
        this, id++,
        new Intent(this, AlarmTriggerReceiver.class), 0);

    // TODO, implement other alarm calls?  This one only works for api 23.
    // changed again.  Need to use setExactAndAllowWhileIdle
    ((AlarmManager)getSystemService(Context.ALARM_SERVICE))
      .setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeUTC, schedule);
  }

  public void dismissAllAlarms() {
    if (wakelock != null) {
      Log.i("ServiceAlarmclock", "Releasing wake lock");
      wakelock.release();
      wakelock = null;
    } else {
      Log.w("ServiceAlarmClock", "No wake lock found when dismissing alarm");
    }

    ((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE))
      .cancel(TRIGGER_ALARM_NOTIFICATION);
  }

  private void handleTriggerAlarm(Intent intent) {
    PowerManager.WakeLock w = null;
    if (intent.hasExtra(AlarmTriggerReceiver.WAKELOCK_ID)) {
      w = AlarmTriggerReceiver.consumeLock(
          intent.getExtras().getInt(AlarmTriggerReceiver.WAKELOCK_ID));
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

    Intent i = new Intent(this, ActivityAlarmNotification.class)
      .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

    ((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE))
      .notify(
          TRIGGER_ALARM_NOTIFICATION,
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
          .setContentIntent(PendingIntent.getActivity(this, 0, i, 0))
          .build());

    startActivity(i);
  }
}
