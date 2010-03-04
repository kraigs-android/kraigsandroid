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
  public void fire(int id) throws RemoteException {
    Toast.makeText(context, "FIRE ALARM!", Toast.LENGTH_SHORT).show();
    service.triggerAlarm(id);
  }
  @Override
  public void alarmOn() throws RemoteException {
    Toast.makeText(context, "SCHEDULE ALARM!", Toast.LENGTH_SHORT).show();
    service.addAlarm();
  }
  @Override
  public void alarmOff() throws RemoteException {
    Toast.makeText(context, "UNSCHEDULE ALARM!", Toast.LENGTH_SHORT).show();
    service.clearAllAlarms();
  }

  @Override
  public void clearAlarm(int id) throws RemoteException {
    Toast.makeText(context, "HANDLE ALARM " + id, Toast.LENGTH_SHORT).show();
    service.removeAlarm(id);
  }
}
