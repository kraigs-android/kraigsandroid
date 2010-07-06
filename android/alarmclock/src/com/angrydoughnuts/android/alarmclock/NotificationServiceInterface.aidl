package com.angrydoughnuts.android.alarmclock;

interface NotificationServiceInterface {
  long currentAlarmId();
  float volume();
  void startNotification(long alarmId);
  void acknowledgeCurrentNotification(int snoozeMinutes);
}