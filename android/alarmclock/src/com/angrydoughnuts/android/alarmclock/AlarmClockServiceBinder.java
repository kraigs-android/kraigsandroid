package com.angrydoughnuts.android.alarmclock;

import java.util.LinkedList;
import java.util.List;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.widget.Toast;

public class AlarmClockServiceBinder {
  private Context context;
  private int bindFlags;
  private AlarmClockInterface clock;
  private List<ServiceCallback> callbacks;
 
  public static AlarmClockServiceBinder newBinderAndStart(Context context) {
    return new AlarmClockServiceBinder(context, Context.BIND_AUTO_CREATE);
  }
  public static AlarmClockServiceBinder newBinder(Context context) {
    return new AlarmClockServiceBinder(context, 0);
  }

  private interface ServiceCallback {
    void run() throws RemoteException;
  }

  final private ServiceConnection serviceConnection = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
      Toast.makeText(context, "Service Connected " + name, Toast.LENGTH_SHORT).show();
      clock = AlarmClockInterface.Stub.asInterface(service);
      for (ServiceCallback callback : callbacks) {
        try {
          callback.run();
        } catch (RemoteException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
      callbacks.clear();
    }
    @Override
    public void onServiceDisconnected(ComponentName name) {
      // TODO(cgallek): This should only happen if the AlarmClockService
      // crashes.  Consider throwing an exception here.
      Toast.makeText(context, "Service Disconnected " + name, Toast.LENGTH_SHORT).show();
      clock = null;
    }
  };

  private AlarmClockServiceBinder(Context context, int bindFlags) {
    this.context = context;
    this.bindFlags = bindFlags;
    this.callbacks = new LinkedList<ServiceCallback>();
  }

  private void runOrDefer(ServiceCallback callback) {
    if (clock != null) {
      try {
        callback.run();
      } catch (RemoteException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    } else {
      callbacks.add(callback);
    }
  }

  public void bind() {
    final Intent serviceIntent = new Intent(context, AlarmClockService.class);
    if (!context.bindService(serviceIntent, serviceConnection, bindFlags)) {
      throw new IllegalStateException("Unable to bind to AlarmClockService.");
    }
  }

  public void unbind() {
    runOrDefer(new ServiceCallback() {
      @Override
      public void run() throws RemoteException {
        context.unbindService(serviceConnection);
        clock = null;
      }
    });
  }

  public void fire(final int id) { 
    runOrDefer(new ServiceCallback() {
      @Override
      public void run() throws RemoteException {
        clock.fire(id);
      }
    });
  }

  public void alarmOn() {
    runOrDefer(new ServiceCallback() {
      @Override
      public void run() throws RemoteException {
        clock.alarmOn();
      }
    });
  }

  public void alarmOff() {
    runOrDefer(new ServiceCallback() {
      @Override
      public void run() throws RemoteException {
        clock.alarmOff();
      }
    });
  }

  public void clearAlarm(final int id) {
    runOrDefer(new ServiceCallback() {
      @Override
      public void run() throws RemoteException {
        clock.clearAlarm(id);
      }
    });
  }
}
