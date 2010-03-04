package com.angrydoughnuts.android.alarmclock;

interface AlarmClockInterface {
  void notifyDialog(int alarmId);
  void scheduleAlarmIn(int seconds);
  void clearAlarm(int alarmId);
  void clearAllAlarms();
}