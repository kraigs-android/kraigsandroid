/****************************************************************************
 * Copyright 2010 kraigs.android@gmail.com
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

import java.util.TreeMap;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

/**
 * This container holds a list of all currently scheduled alarms.
 * Adding/removing alarms to this container schedules/unschedules PendingIntents
 * with the android AlarmManager service.
 */
public final class PendingAlarmList {
  // Maps alarmId -> alarm.
  private TreeMap<Long, PendingAlarm> pendingAlarms;
  // Maps alarm time -> alarmId.
  private TreeMap<AlarmTime, Long> alarmTimes;
  private AlarmManager alarmManager;
  private Context context;

  public PendingAlarmList(Context context) {
    pendingAlarms = new TreeMap<Long, PendingAlarm>();
    alarmTimes = new TreeMap<AlarmTime, Long>();
    alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    this.context = context;
  }

  public int size() {
    if (pendingAlarms.size() != alarmTimes.size()) {
      throw new IllegalStateException("Inconsistent pending alarms: "
          + pendingAlarms.size() + " vs " + alarmTimes.size());
    }
    return pendingAlarms.size();
  }

  public void put(long alarmId, AlarmTime time) {
    // Remove this alarm if it exists already.
    remove(alarmId);

    // Intents are considered equal if they have the same action, data, type,
    // class, and categories.  In order to schedule multiple alarms, every
    // pending intent must be different.  This means that we must encode
    // the alarm id in the data section of the intent rather than in
    // the extras bundle.
    Intent notifyIntent = new Intent(context, ReceiverAlarm.class);
    notifyIntent.setData(AlarmUtil.alarmIdToUri(alarmId));
    PendingIntent scheduleIntent =
      PendingIntent.getBroadcast(context, 0, notifyIntent, 0);

    // Previous instances of this intent will be overwritten in
    // the alarm manager.
    alarmManager.set(AlarmManager.RTC_WAKEUP, time.calendar().getTimeInMillis(), scheduleIntent);

    // Keep track of all scheduled alarms.
    pendingAlarms.put(alarmId, new PendingAlarm(time, scheduleIntent));
    alarmTimes.put(time, alarmId);

    if (pendingAlarms.size() != alarmTimes.size()) {
      throw new IllegalStateException("Inconsistent pending alarms: "
          + pendingAlarms.size() + " vs " + alarmTimes.size());
    }
  }

  public boolean remove(long alarmId) {
    PendingAlarm alarm = pendingAlarms.remove(alarmId);
    if (alarm == null) {
      return false;
    }
    Long expectedAlarmId = alarmTimes.remove(alarm.time());
    alarmManager.cancel(alarm.pendingIntent());
    alarm.pendingIntent().cancel();

    if (expectedAlarmId != alarmId) {
      throw new IllegalStateException("Internal inconsistency in PendingAlarmList");
    }

    if (pendingAlarms.size() != alarmTimes.size()) {
      throw new IllegalStateException("Inconsistent pending alarms: "
          + pendingAlarms.size() + " vs " + alarmTimes.size());
    }

    return true;
  }

  public AlarmTime nextAlarmTime() {
    if (alarmTimes.size() == 0) {
      return null;
    }
    return alarmTimes.firstKey();
  }

  public AlarmTime pendingTime(long alarmId) {
    PendingAlarm alarm = pendingAlarms.get(alarmId);
    return alarm == null ? null : alarm.time();
  }

  public AlarmTime[] pendingTimes() {
    AlarmTime[] times = new AlarmTime[alarmTimes.size()];
    alarmTimes.keySet().toArray(times);
    return times;
  }

  public Long[] pendingAlarms() {
    Long[] alarmIds = new Long[pendingAlarms.size()];
    pendingAlarms.keySet().toArray(alarmIds);
    return alarmIds;
  }

  private class PendingAlarm {
    private AlarmTime time;
    private PendingIntent pendingIntent;

    PendingAlarm(AlarmTime time, PendingIntent pendingIntent) {
      this.time = time;
      this.pendingIntent = pendingIntent;
    }
    public AlarmTime time() {
      return time;
    }
    public PendingIntent pendingIntent() {
      return pendingIntent;
    }
  }
}
