package com.angrydoughnuts.android.alarmclock;

interface AlarmClockInterface {
  void newAlarm(int minutesAfterMidnight);
  void scheduleAlarm(long alarmId);
  void dismissAlarm(long alarmId);
}