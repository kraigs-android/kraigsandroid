package com.angrydoughnuts.android.alarmclock;

import java.util.TimerTask;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;

public class AlarmClockTimerTask extends TimerTask {

  private Context context;
  private Handler handler;
  private Intent serviceIntent;
  private AlarmClockInterface clock;

  final private ServiceConnection serviceConnection = new ServiceConnection() {
    public void onServiceConnected(ComponentName name, IBinder service) {
      clock = AlarmClockInterface.Stub.asInterface(service);
    }
    public void onServiceDisconnected(ComponentName name) {
      // TODO(cgallek): This shouldn't happen.  Throw an exception??
      clock = null;
    }
  };

  public AlarmClockTimerTask(Context context, Handler handler) {
    this.context = context;
    this.handler = handler;
    this.serviceIntent = new Intent(context, AlarmClockService.class);
  }

  @Override
  public void run() {
    // TODO(cgallek): This currently re-binds to the service on every
    // run.  Figure out how to reference count threads and only
    // bind as necessary in the Timer thread.
    if (!context.bindService(serviceIntent, serviceConnection, 0)) {
      throw new IllegalStateException("Unable to bind to AlarmClock service.");
    }

    handler.post(new Runnable() {
      @Override
      public void run() {
        try {
          clock.fire();
        } catch (RemoteException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    });

    context.unbindService(serviceConnection);
  }
}
