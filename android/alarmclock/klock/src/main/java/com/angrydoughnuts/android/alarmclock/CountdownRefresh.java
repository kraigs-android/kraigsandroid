/****************************************************************************
 * Copyright 2019 kraigs.android@gmail.com
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

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.util.Calendar;

public class CountdownRefresh extends BroadcastReceiver {
  public static final String DISPLAY_NOTIFICATION_PREF = "NOTIFICATION_ICON";
  private static final int JOB_ID = 759276657;
  private static final String TAG = CountdownRefresh.class.getSimpleName();

  public static void start(Context c) {
    if (showNotification(c)) {
      scheduleRefresh(c);
    } else {
      stop(c);
    }
  }

  public static void stop(Context c) {
    ((AlarmManager)c.getSystemService(Context.ALARM_SERVICE))
      .cancel(
          PendingIntent.getBroadcast(
              c, JOB_ID, new Intent(c, CountdownRefresh.class), 0));
    clearNotification(c);
  }

  @Override
  public void onReceive (Context c, Intent i) {
    Log.i(TAG, "Refresh wakeup");
    start(c);
  }

  private static void scheduleRefresh(Context c) {
    Calendar refresh = TimeUtil.nextMinute();
    Log.i(TAG, "Schedule refresh at: " + TimeUtil.formatDebug(refresh));
    ((AlarmManager)c.getSystemService(Context.ALARM_SERVICE))
      .setExact(AlarmManager.RTC, refresh.getTimeInMillis(),
                PendingIntent.getBroadcast(
                    c, JOB_ID, new Intent(c, CountdownRefresh.class), 0));
  }

  private static final int NEXT_ALARM_NOTIFICATION_ID = 69;
  private static final String NEXT_ALARM_NOTIFICATION_CHAN = "next";
  @SuppressWarnings("deprecation")  // PreferenceManager, NotificationBuilder
  private static boolean showNotification(Context c) {
    if (android.preference.PreferenceManager.getDefaultSharedPreferences(c)
        .getBoolean(DISPLAY_NOTIFICATION_PREF, true) == false) {
      clearNotification(c);
      return false;
    }

    final DbUtil.Alarm next = DbUtil.Alarm.getNextEnabled(c);
    if (next == null) {
      clearNotification(c);
      return false;
    }
    final Calendar when =
      TimeUtil.nextOccurrence(next.time, next.repeat, next.next_snooze);

    NotificationManager manager =
      (NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);

    // Create a notification channel on first use.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
        manager.getNotificationChannel(NEXT_ALARM_NOTIFICATION_CHAN) == null) {
      manager.createNotificationChannel(
          new NotificationChannel(
              NEXT_ALARM_NOTIFICATION_CHAN,
              c.getString(R.string.pending_alarm_notification),
              NotificationManager.IMPORTANCE_LOW)); // No sound.
    }

    manager.notify(
        NEXT_ALARM_NOTIFICATION_ID,
        (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
         new Notification.Builder(c, NEXT_ALARM_NOTIFICATION_CHAN) :
         new Notification.Builder(c))
        .setContentTitle(next.label.isEmpty() ?
                         c.getString(R.string.app_name) :
                         next.label)
        .setContentText(TimeUtil.formatLong(c, when) + " : " +
                        TimeUtil.until(c, when))
        .setSmallIcon(R.drawable.ic_alarm)
        .setCategory(Notification.CATEGORY_STATUS)
        .setVisibility(Notification.VISIBILITY_PUBLIC)
        .setOngoing(true)
        .setContentIntent(
            PendingIntent.getActivity(
                c, 0, new Intent(c, AlarmClockActivity.class), 0))
        .build());

    return true;
  }

  private static void clearNotification(Context c) {
    ((NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE))
      .cancel(NEXT_ALARM_NOTIFICATION_ID);
  }
}
