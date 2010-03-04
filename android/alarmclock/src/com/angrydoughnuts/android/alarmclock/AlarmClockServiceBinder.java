package com.angrydoughnuts.android.alarmclock;

import java.util.Calendar;
import java.util.LinkedList;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;

public class AlarmClockServiceBinder {
  private Context context;
  private int bindFlags;
  private AlarmClockInterface clock;
  private LinkedList<ServiceCallback> callbacks;

  //TODO(cgallek): consider removing this method and using the
  // contstructor of AlarmClockServiceBinder directly (removing the
  // flags argument, of course).
  public static AlarmClockServiceBinder newBinder(Context context) {
    return new AlarmClockServiceBinder(context, Context.BIND_AUTO_CREATE);
  }

  private interface ServiceCallback {
    void run() throws RemoteException;
  }

  final private ServiceConnection serviceConnection = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
      clock = AlarmClockInterface.Stub.asInterface(service);
      while (callbacks.size() > 0) {
        ServiceCallback callback = callbacks.remove();
        try {
          callback.run();
        } catch (RemoteException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    }
    @Override
    public void onServiceDisconnected(ComponentName name) {
      // TODO(cgallek): This should only happen if the AlarmClockService
      // crashes.  Consider throwing an exception here.
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
      callbacks.offer(callback);
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

  /*
  public void newAlarm(final int minutesAfterMidnight) {
    runOrDefer(new ServiceCallback() {
      @Override
      public void run() throws RemoteException {
        clock.newAlarm(minutesAfterMidnight);
      }
    });
  }
  */

  public void createAlarm(final Calendar calendar) {
    runOrDefer(new ServiceCallback() {
      @Override
      public void run() throws RemoteException {
        clock.createAlarm(new CalendarParcel(calendar));
      }
    });
  }

  public void deleteAlarm(final long alarmId) {
    runOrDefer(new ServiceCallback() {
      @Override
      public void run() throws RemoteException {
        clock.deleteAlarm(alarmId);
      }
    });
  }

  public void scheduleAlarm(final long alarmId) {
    runOrDefer(new ServiceCallback() {
      @Override
      public void run() throws RemoteException {
        clock.scheduleAlarm(alarmId);
      }
    });
  }

  public void dismissAlarm(final long alarmId) {
    runOrDefer(new ServiceCallback() {
      @Override
      public void run() throws RemoteException {
        clock.dismissAlarm(alarmId);
      }
    });
  }

  public void snoozeAlarm(final long alarmId) {
    runOrDefer(new ServiceCallback() {
      @Override
      public void run() throws RemoteException {
        clock.snoozeAlarm(alarmId);
      }
    });
  }
}
