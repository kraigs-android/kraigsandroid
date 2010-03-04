package com.angrydoughnuts.android.alarmclock;

import java.util.TreeMap;

import android.app.PendingIntent;

public class PendingAlarmList {
  // Maps alarmId -> alarm.
  private TreeMap<Long, PendingAlarm> pendingAlarms;
  // Maps alarm time -> alarmId.
  private TreeMap<Long, Long> alarmTimes;

  public PendingAlarmList() {
    pendingAlarms = new TreeMap<Long, PendingAlarm>();
    alarmTimes = new TreeMap<Long, Long>();
  }

  public int size() {
    return pendingAlarms.size();
  }

  public void put(long alarmId, long alarmTimeMillisUTC, PendingIntent intent) {
    pendingAlarms.put(alarmId, new PendingAlarm(alarmTimeMillisUTC, intent));
    alarmTimes.put(alarmTimeMillisUTC, alarmId);
  }

  public PendingIntent remove(long alarmId) {
    PendingAlarm alarm = pendingAlarms.remove(alarmId);
    if (alarm == null) {
      return null;
    }
    Long expectedAlarmId = alarmTimes.remove(alarm.alarmTimeMillisUTC());
    if (expectedAlarmId != alarmId) {
      throw new IllegalStateException("Internal inconsistency in PendingAlarmList");
    }
    return alarm.pendingIntent();
  }

  private class PendingAlarm {
    private long alarmTimeMillisUTC;
    private PendingIntent pendingIntent;

    PendingAlarm(long alarmTimeMillisUTC, PendingIntent pendingIntent) {
      this.alarmTimeMillisUTC = alarmTimeMillisUTC;
      this.pendingIntent = pendingIntent;
    }
    public long alarmTimeMillisUTC() {
      return alarmTimeMillisUTC;
    }
    public PendingIntent pendingIntent() {
      return pendingIntent;
    }
  }
}
