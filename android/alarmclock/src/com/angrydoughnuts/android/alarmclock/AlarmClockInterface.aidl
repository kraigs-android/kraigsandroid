package com.angrydoughnuts.android.alarmclock;

interface AlarmClockInterface {
  void newAlarm(int minutesAfterMidnight);
  void deleteAlarm(long alarmId);
  void scheduleAlarm(long alarmId);
  void dismissAlarm(long alarmId);
}