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

package com.angrydoughnuts.android.alarmclock;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import java.util.LinkedList;
import java.util.List;

public class SystemMessageReceiver extends BroadcastReceiver {
  @Override
  public void onReceive(Context c, Intent i) {
    int command;
    if (i.getAction().equals(Intent.ACTION_TIMEZONE_CHANGED)) {
      Log.i(TAG, "Timezone change...");
      command = RescheduleService.RESCHEDULE;
    } else if (i.getAction().equals(Intent.ACTION_MY_PACKAGE_REPLACED)) {
      Log.i(TAG, "Installed...");
      command = RescheduleService.SCHEDULE;
    } else if (i.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
      Log.i(TAG, "Boot complete...");
      command = RescheduleService.SCHEDULE;
    } else {
      Log.i(TAG, "Unknown broadcast" + i.toString());
      return;
    }

    Intent reschedule = new Intent(c, RescheduleService.class)
      .putExtra(RescheduleService.COMMAND, command);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      c.startForegroundService(reschedule);
    } else {
      c.startService(reschedule);
    }
  }

  private static class Alarm {
    final long id;
    final long tsUTC;
    public Alarm(Cursor c) {
      this.id = c.getLong(c.getColumnIndex(AlarmClockProvider.AlarmEntry._ID));
      this.tsUTC = TimeUtil.nextOccurrence(
          c.getInt(c.getColumnIndex(AlarmClockProvider.AlarmEntry.TIME)),
          c.getInt(c.getColumnIndex(AlarmClockProvider.AlarmEntry.DAY_OF_WEEK)),
          c.getInt(c.getColumnIndex(AlarmClockProvider.AlarmEntry.NEXT_SNOOZE)))
        .getTimeInMillis();
    }
  }

  // Hacky foreground service that delegates to the original (background)
  // service.  Newer Androids can't start background services from
  // BroadcastReceivers.
  public static class RescheduleService extends Service {
    public static final String COMMAND = "command";
    public static final int RESCHEDULE = 1;
    public static final int SCHEDULE = 2;

    @Override
    public IBinder onBind(Intent intent) { return null; }
    @Override
    public int onStartCommand(Intent i, int flags, int startId) {
      final String CHAN = "reschedule";
      final NotificationManager manager =
        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
          manager.getNotificationChannel(CHAN) == null) {
        manager.createNotificationChannel(
            new NotificationChannel(
                CHAN, CHAN, NotificationManager.IMPORTANCE_LOW)); // No sound.
      }
      final Notification notification =
        (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
         new Notification.Builder(this, CHAN) :
         new Notification.Builder(this))
        .setSmallIcon(R.drawable.ic_alarm_on)
        .setCategory(Notification.CATEGORY_ALARM)
        .setPriority(Notification.PRIORITY_MIN)
        .setVisibility(Notification.VISIBILITY_PUBLIC)
        .build();
      startForeground(6969, notification);

      switch (i.getExtras().getInt(COMMAND)) {
      case RESCHEDULE:
        clearSnooze(this);
        for (Alarm a : enabledAlarms(getContentResolver())) {
          Log.i(TAG, "Rescheduling alarm " + a.id);
          AlarmNotificationService.removeAlarmTrigger(this, a.id);
          AlarmNotificationService.scheduleAlarmTrigger(this, a.id, a.tsUTC);
        }
        break;
      case SCHEDULE:
        clearSnooze(this);
        for (Alarm a : enabledAlarms(getContentResolver())) {
          Log.i(TAG, "Scheduling alarm " + a.id);
          AlarmNotificationService.scheduleAlarmTrigger(this, a.id, a.tsUTC);
    }
        break;
      }

      stopSelf();
      return START_NOT_STICKY;
    }

    private void clearSnooze(Context c) {
      ContentValues val = new ContentValues();
      val.put(AlarmClockProvider.AlarmEntry.NEXT_SNOOZE, 0);
      c.getContentResolver().update(
          AlarmClockProvider.ALARMS_URI, val, null, null);
    }

    private List<Alarm> enabledAlarms(ContentResolver r) {
      LinkedList<Alarm> ids = new LinkedList<Alarm>();
      Cursor c = r.query(
          AlarmClockProvider.ALARMS_URI,
          new String[] {
            AlarmClockProvider.AlarmEntry._ID,
            AlarmClockProvider.AlarmEntry.TIME,
            AlarmClockProvider.AlarmEntry.DAY_OF_WEEK,
            AlarmClockProvider.AlarmEntry.NEXT_SNOOZE },
          AlarmClockProvider.AlarmEntry.ENABLED + " == 1",
          null, null);
      while (c.moveToNext())
        ids.add(new Alarm(c));
      c.close();
      return ids;
    }
  }

  private static final String TAG = SystemMessageReceiver.class.getSimpleName();
}
