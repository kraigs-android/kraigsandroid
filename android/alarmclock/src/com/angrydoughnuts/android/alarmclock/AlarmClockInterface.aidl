package com.angrydoughnuts.android.alarmclock;

interface AlarmClockInterface {
  void scheduleAlarmIn(int seconds);
  void acknowledgeAlarm(int alarmId);
  void clearAllAlarms();
}