package com.angrydoughnuts.android.alarmclock;

interface AlarmClockInterface {
  void scheduleAlarmIn(int seconds);
  void acknowledgeAlarm(long alarmId);
  void clearAllAlarms();
}