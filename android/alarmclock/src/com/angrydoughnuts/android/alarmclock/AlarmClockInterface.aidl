package com.angrydoughnuts.android.alarmclock;

interface AlarmClockInterface {
  void fire(int id);
  void alarmOn();
  void alarmOff();
  void clearAlarm(int id);
}