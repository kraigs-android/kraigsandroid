package com.angrydoughnuts.android.alarmclock;

import java.util.Timer;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.PowerManager;
import android.os.RemoteException;
import android.widget.Toast;

public class AlarmClockInterfaceStub extends AlarmClockInterface.Stub {
  // TODO(cgallek) make this object a constructor parameter and ensure
  // that the handler is constructed in the UI thread.
  private Context context;
  private Handler uiHandler;
  private Timer timerThread;
  private AlarmClockTimerTask task;

  AlarmClockInterfaceStub(
      Context context, Handler uiHandler, Timer timerThread) {
    this.context = context;
    this.uiHandler = uiHandler;
    this.timerThread = timerThread;
  }

  @Override
  public void fire() throws RemoteException {
    Toast.makeText(context, "FIRE ALARM!", Toast.LENGTH_SHORT).show();
    Intent notifyIntent = new Intent(context, AlarmNotificationActivity.class);
    notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

    // TODO(cgallek) Currently, both this service and the Notification
    // Activity manage power settings.  It might make sense to move all
    // power management into the service.  This would require a callback
    // from the Notification application.  I'm not sure how to get a
    // response from an activity started from a service...
    // I think there also might be a race condition between the
    // startActivity call and the wake lock release call below (ie if
    // the lock is released before the activity actually starts).  Moving
    // all locking to this service would also fix that problem.
    PowerManager manager =
      (PowerManager) context.getSystemService(Context.POWER_SERVICE);
    PowerManager.WakeLock wakeLock = manager.newWakeLock(
        PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
        "Alarm Service CPU wake lock");
    wakeLock.acquire();

    context.startActivity(notifyIntent);

    wakeLock.release();
  }
  @Override
  public void alarmOn() throws RemoteException {
    // TODO(cgallek): This is just a test timer task.  Remove it.
    // Also, this is not the correct context to use.
    Toast.makeText(context, "SCHEDULE ALARM!", Toast.LENGTH_SHORT).show();
    AlarmClockService.setAlarmOn(true);
    if (task == null) {
      task = new AlarmClockTimerTask(context, uiHandler);
      timerThread.schedule(task, 5000, 5000);
    }
  }
  @Override
  public void alarmOff() throws RemoteException {
    Toast.makeText(context, "UNSCHEDULE ALARM!", Toast.LENGTH_SHORT).show();
    AlarmClockService.setAlarmOn(false);
      if (task != null) {
      task.cancel();
      task = null;
    }
  }
}
