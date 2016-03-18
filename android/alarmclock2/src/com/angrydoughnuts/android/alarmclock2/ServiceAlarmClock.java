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
import android.content.BroadcastReceiver;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.ArrayMap;
import android.util.Log;

public class ServiceAlarmClock extends Service {
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
        PowerManager.WakeLock w = null;
        if (intent.hasExtra(AlarmTriggerReceiver.WAKELOCK_ID)) {
          w = AlarmTriggerReceiver.consumeLock(
              intent.getExtras().getInt(AlarmTriggerReceiver.WAKELOCK_ID));
        }

        if (w == null) {
          Log.w("ServiceAlarmClock",
                "No wake lock present for TRIGGER_ALARM_NOTIFICATION");
        }

        startActivity(new Intent(this, ActivityAlarmNotification.class)
                      .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));

        // TODO: move this to the dismiss function
        if (w != null) {
          Log.i("ServiceAlarmClock", "Releasing wake lock");
          w.release();
        }
        break;
      }
    }

    stopSelf(startId);
    return START_NOT_STICKY;
  }

  // TODO: temp
  private static int id = 0;
  public void createTestAlarm() {
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
      .setExactAndAllowWhileIdle(
          AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 5000, schedule);
  }
}
