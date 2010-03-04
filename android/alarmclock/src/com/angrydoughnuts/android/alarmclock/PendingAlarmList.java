package com.angrydoughnuts.android.alarmclock;

import java.util.TreeMap;

import android.app.PendingIntent;

public class PendingAlarmList {
  // Maps alarmId -> alarm.
  private TreeMap<Long, PendingAlarm> pendingAlarms;
  // Maps alarm time -> alarmId.
  private TreeMap<AlarmTime, Long> alarmTimes;

  public PendingAlarmList() {
    pendingAlarms = new TreeMap<Long, PendingAlarm>();
    alarmTimes = new TreeMap<AlarmTime, Long>();
  }

  public int size() {
    return pendingAlarms.size();
  }

  public void put(long alarmId, AlarmTime time, PendingIntent intent) {
    pendingAlarms.put(alarmId, new PendingAlarm(time, intent));
    alarmTimes.put(time, alarmId);
  }

  public PendingIntent remove(long alarmId) {
    PendingAlarm alarm = pendingAlarms.remove(alarmId);
    if (alarm == null) {
      return null;
    }
    Long expectedAlarmId = alarmTimes.remove(alarm.time());
    if (expectedAlarmId != alarmId) {
      throw new IllegalStateException("Internal inconsistency in PendingAlarmList");
    }
    return alarm.pendingIntent();
  }

  public AlarmTime nextAlarmTime() {
    if (alarmTimes.size() == 0) {
      return null;
    }
    return alarmTimes.firstKey();
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
