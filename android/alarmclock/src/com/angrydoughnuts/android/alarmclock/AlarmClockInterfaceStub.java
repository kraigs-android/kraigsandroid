package com.angrydoughnuts.android.alarmclock;

import android.content.Context;
import android.os.RemoteException;
import android.widget.Toast;

// TODO(cgallek): Remove these toasts.
// TODO(cgallek): This class can probably move as an inner class
// of AlarmClockService.
public class AlarmClockInterfaceStub extends AlarmClockInterface.Stub {
  private Context context;
  private AlarmClockService service;
  
  AlarmClockInterfaceStub(Context context, AlarmClockService service) {
    this.context = context;
    this.service = service;
  }

  @Override
  public void newAlarm(int minutesAfterMidnight) throws RemoteException {
    Toast.makeText(context, "SCHEDULE ALARM! " + minutesAfterMidnight, Toast.LENGTH_SHORT).show();
    service.newAlarm(minutesAfterMidnight);
  }

  @Override
  public void scheduleAlarmIn(int seconds) {
    Toast.makeText(context, "SCHEDULE ALARM!", Toast.LENGTH_SHORT).show();
    service.scheduleAlarmIn(seconds);
  }

  @Override
  public void acknowledgeAlarm(long alarmId) {
    Toast.makeText(context, "CLEAR ALARM " + alarmId, Toast.LENGTH_SHORT).show();
    service.acknowledgeAlarm(alarmId);
  }

  @Override
  public void clearAllAlarms() {
    Toast.makeText(context, "CLEAR ALL ALARM!", Toast.LENGTH_SHORT).show();
    service.clearAllAlarms();
  }
}
