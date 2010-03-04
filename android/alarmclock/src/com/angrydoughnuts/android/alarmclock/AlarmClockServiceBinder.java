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
      clock = AlarmClockInterface.Stub.asInterface(service);
    }
    @Override
    public void onServiceDisconnected(ComponentName name) {
      // TODO(cgallek): This should only happen if the AlarmClockService
      // crashes.  Consider throwing an exception here.
      Toast.makeText(context, "Service Disconnected " + name, Toast.LENGTH_SHORT).show();
      clock = null;
    }
  };

  public void bind() {
    final Intent serviceIntent = new Intent(context, AlarmClockService.class);
    if (!context.bindService(serviceIntent, serviceConnection, bindFlags)) {
      throw new IllegalStateException("Unable to bind to AlarmClockService.");
    }
  }

  public void unbind() {
    context.unbindService(serviceConnection);
    clock = null;
  }

  // TODO(cgallek): Note not to keep references to this around.
  public AlarmClockInterface clock() {
    return clock;
  }

  private AlarmClockServiceBinder(Context context, int bindFlags) {
    this.context = context;
    this.bindFlags = bindFlags;
  }
}
