package com.angrydoughnuts.android.alarmclock;

import android.content.Context;
import android.os.RemoteException;
import android.widget.Toast;

public class AlarmClockInterfaceStub extends AlarmClockInterface.Stub {
  private Context context;
  private AlarmClockService service;
  
  AlarmClockInterfaceStub(Context context, AlarmClockService service) {
    this.context = context;
    this.service = service;
  }

  @Override
  public AlarmTime[] pendingAlarmTimes() throws RemoteException {
    return service.pendingAlarmTimes();
  }

  @Override
  public void createAlarm(AlarmTime time) throws RemoteException {
    debugToast("CREATE ALARM " + time.toString());
    service.createAlarm(time);
  }

  @Override
  public void deleteAlarm(long alarmId) throws RemoteException {
    debugToast("DELETE ALARM " + alarmId);
    service.deleteAlarm(alarmId);
  }

  @Override
  public void scheduleAlarm(long alarmId) throws RemoteException {
    debugToast("SCHEDULE ALARM " + alarmId);
    service.scheduleAlarm(alarmId);
  }

  @Override
  public void dismissAlarm(long alarmId) {
    debugToast("DISMISS ALARM " + alarmId);
    service.dismissAlarm(alarmId);
  }

  @Override
  public void snoozeAlarm(long alarmId) throws RemoteException {
    debugToast("SNOOZE ALARM " + alarmId);
    service.snoozeAlarm(alarmId);
  }

  @Override
  public void snoozeAlarmFor(long alarmId, int minutes) throws RemoteException {
    debugToast("SNOOZE ALARM " + alarmId + " for " + minutes);
    service.snoozeAlarmFor(alarmId, minutes);
  }

  private void debugToast(String message) {
    if (AlarmClockService.debug(context)) {
      Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
  }
}
