package com.angrydoughnuts.android.alarmclock;

interface AlarmClockInterface {
  void newAlarm(int minutesAfterMidnight);
  void dismissAlarm(long alarmId);
}