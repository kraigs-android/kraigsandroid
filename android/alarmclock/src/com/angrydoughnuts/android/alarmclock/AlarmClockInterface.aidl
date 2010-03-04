package com.angrydoughnuts.android.alarmclock;

import com.angrydoughnuts.android.alarmclock.AlarmTime;

interface AlarmClockInterface {
  void createAlarm(in AlarmTime time);
  void deleteAlarm(long alarmId);
  void scheduleAlarm(long alarmId);
  void dismissAlarm(long alarmId);
  void snoozeAlarm(long alarmId);
}