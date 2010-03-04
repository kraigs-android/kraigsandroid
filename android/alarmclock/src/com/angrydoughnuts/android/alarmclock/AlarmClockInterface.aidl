package com.angrydoughnuts.android.alarmclock;

import com.angrydoughnuts.android.alarmclock.CalendarParcel;

interface AlarmClockInterface {
  void createAlarm(in CalendarParcel calendar);
  void deleteAlarm(long alarmId);
  void scheduleAlarm(long alarmId);
  void dismissAlarm(long alarmId);
  void snoozeAlarm(long alarmId);
}