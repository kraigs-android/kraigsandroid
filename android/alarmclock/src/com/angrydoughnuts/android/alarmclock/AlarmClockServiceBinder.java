package com.angrydoughnuts.android.alarmclock;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.widget.Toast;

public class AlarmClockServiceBinder {
  private Context context;
  private int bindFlags;
  private AlarmClockInterface clock;
  private synchronized void setClock(AlarmClockInterface clock) {
    this.clock = clock;
    notify();
  }

  public static AlarmClockServiceBinder newBinderAndStart(Context context) {
    return new AlarmClockServiceBinder(context, Context.BIND_AUTO_CREATE);
  }
  public static AlarmClockServiceBinder newBinder(Context context) {
    return new AlarmClockServiceBinder(context, 0);
  }

  final private ServiceConnection serviceConnection = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
      Toast.makeText(context, "Service Connected " + name, Toast.LENGTH_SHORT).show();
      setClock(AlarmClockInterface.Stub.asInterface(service));
    }
    @Override
    public void onServiceDisconnected(ComponentName name) {
      // TODO(cgallek): This should only happen if the AlarmClockService
      // crashes.  Consider throwing an exception here.
      Toast.makeText(context, "Service Disconnected " + name, Toast.LENGTH_SHORT).show();
      setClock(null);
    }
  };

  public synchronized void bind() {
    final Intent serviceIntent = new Intent(context, AlarmClockService.class);
    if (!context.bindService(serviceIntent, serviceConnection, bindFlags)) {
      throw new IllegalStateException("Unable to bind to AlarmClockService.");
    }
    while (clock == null) {
      try {
        wait();
      } catch (InterruptedException e) {
        throw new IllegalStateException("Unable to bind to AlarmClockService.");
      }
    }
  }

  public synchronized void unbind() {
    context.unbindService(serviceConnection);
    clock = null;
  }

  // TODO(cgallek): Note not to keep references to this around.
  public synchronized AlarmClockInterface clock() {
    return clock;
  }

  private AlarmClockServiceBinder(Context context, int bindFlags) {
    this.context = context;
    this.bindFlags = bindFlags;
  }
}
