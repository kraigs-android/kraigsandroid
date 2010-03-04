package com.angrydoughnuts.android.alarmclock;

interface AlarmClockInterface {
  void newAlarm(int minutesAfterMidnight);
  void scheduleAlarmIn(int seconds);
  void acknowledgeAlarm(long alarmId);
  void clearAllAlarms();
}