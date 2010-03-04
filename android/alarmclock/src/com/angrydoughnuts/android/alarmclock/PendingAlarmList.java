package com.angrydoughnuts.android.alarmclock;

import java.util.TreeMap;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

public class PendingAlarmList {
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
    notifyIntent.setData(AlarmClockService.alarmIdToUri(alarmId));
    PendingIntent scheduleIntent =
      PendingIntent.getBroadcast(context, 0, notifyIntent, 0);

    // Previous instances of this intent will be overwritten in
    // the alarm manager.
    alarmManager.set(AlarmManager.RTC_WAKEUP, time.calendar().getTimeInMillis(), scheduleIntent);

    // Keep track of all scheduled alarms.
    pendingAlarms.put(alarmId, new PendingAlarm(time, scheduleIntent));
    alarmTimes.put(time, alarmId);
  }

  public boolean remove(long alarmId) {
    PendingAlarm alarm = pendingAlarms.remove(alarmId);
    if (alarm == null) {
      return false;
    }
    Long expectedAlarmId = alarmTimes.remove(alarm.time());
    if (expectedAlarmId != alarmId) {
      throw new IllegalStateException("Internal inconsistency in PendingAlarmList");
    }

    if (alarm != null) {
      alarmManager.cancel(alarm.pendingIntent());
      alarm.pendingIntent().cancel();
    }
    return true;
  }

  public AlarmTime nextAlarmTime() {
    if (alarmTimes.size() == 0) {
      return null;
    }
    return alarmTimes.firstKey();
  }

  public AlarmTime[] pendingTimes() {
    AlarmTime[] times = new AlarmTime[alarmTimes.size()];
    alarmTimes.keySet().toArray(times);
    return times;
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
