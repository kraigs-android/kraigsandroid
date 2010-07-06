package com.angrydoughnuts.android.alarmclock;

import com.angrydoughnuts.android.alarmclock.AlarmTime;

interface AlarmClockInterface {
  void createAlarm(in AlarmTime time);
  void deleteAlarm(long alarmId);
  void deleteAllAlarms();
  void scheduleAlarm(long alarmId);
  void unscheduleAlarm(long alarmId);
  void acknowledgeAlarm(long alarmId);
  void snoozeAlarm(long alarmId);
  void snoozeAlarmFor(long alarmId, int minutes);
  AlarmTime pendingAlarm(long alarmId);
  AlarmTime[] pendingAlarmTimes();
}