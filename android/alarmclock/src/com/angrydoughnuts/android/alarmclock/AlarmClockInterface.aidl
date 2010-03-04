package com.angrydoughnuts.android.alarmclock;

interface AlarmClockInterface {
  void newAlarm(int minutesAfterMidnight);
  void acknowledgeAlarm(long alarmId);
  void clearAllAlarms();
}