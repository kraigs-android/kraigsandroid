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
  private synchronized void setClock(AlarmClockInterface clock) {
    this.clock = clock;
    notify();
  }

  final private ServiceConnection serviceConnection = new ServiceConnection() {
    public void onServiceConnected(ComponentName name, IBinder service) {
      setClock(AlarmClockInterface.Stub.asInterface(service));
    }
    public void onServiceDisconnected(ComponentName name) {
      // TODO(cgallek): This shouldn't happen.  Throw an exception??
      setClock(null);
    }
  };

  final private Runnable work = new Runnable() {
    @Override
    public void run() {
      try {
        clock.fire();
      } catch (RemoteException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  };

  public AlarmClockTimerTask(Context context, Handler handler) {
    this.context = context;
    this.handler = handler;
    this.serviceIntent = new Intent(context, AlarmClockService.class);
  }

  @Override
  public synchronized void run() {
    // TODO(cgallek): This currently re-binds to the service on every
    // run.  Figure out how to reference count threads and only
    // bind as necessary in the Timer thread.
    if (!context.bindService(serviceIntent, serviceConnection, 0)) {
      throw new IllegalStateException("Unable to bind to AlarmClock service.");
    }

    try {
      while (clock == null) {
        wait();
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
      throw new IllegalStateException("Unable to bind to AlarmClockService");
    }

    handler.post(work);

    context.unbindService(serviceConnection);
  }

  @Override
  public boolean cancel() {
    handler.removeCallbacks(work);
    return super.cancel();
  }
}
