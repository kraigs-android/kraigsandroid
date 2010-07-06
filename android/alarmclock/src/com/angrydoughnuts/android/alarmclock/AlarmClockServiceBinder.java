/****************************************************************************
 * Copyright 2010 kraigs.android@gmail.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 ****************************************************************************/

package com.angrydoughnuts.android.alarmclock;

import java.util.LinkedList;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;

/**
 * This class is a wrapper for the process of binding to the AlarmClockService.
 * It provides a seemingly synchronous semantic for the asynchronous binding
 * process.  If the service is not properly bound, a callback is created and
 * registered to be run as soon as binding successfully completes.  Call
 * bind() and unbind() to trigger these processes.
 */
public class AlarmClockServiceBinder {
  private Context context;
  private AlarmClockInterface clock;
  private LinkedList<ServiceCallback> callbacks;

  public AlarmClockServiceBinder(Context context) {
    this.context = context;
    this.callbacks = new LinkedList<ServiceCallback>();
  }

  public AlarmClockInterface clock() {
    return clock;
  }

  public void bind() {
    final Intent serviceIntent = new Intent(context, AlarmClockService.class);
    if (!context.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)) {
      throw new IllegalStateException("Unable to bind to AlarmClockService.");
    }
  }

  public void unbind() {
    context.unbindService(serviceConnection);
    clock = null;
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
          e.printStackTrace();
        }
      }
    }
    @Override
    public void onServiceDisconnected(ComponentName name) {
      clock = null;
    }
  };

  private void runOrDefer(ServiceCallback callback) {
    if (clock != null) {
      try {
        callback.run();
      } catch (RemoteException e) {
        e.printStackTrace();
      }
    } else {
      callbacks.offer(callback);
    }
  }

  public void createAlarm(final AlarmTime time) {
    runOrDefer(new ServiceCallback() {
      @Override
      public void run() throws RemoteException {
        clock.createAlarm(time);
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

  public void deleteAllAlarms() {
    runOrDefer(new ServiceCallback() {
      @Override
      public void run() throws RemoteException {
        clock.deleteAllAlarms();
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

  public void unscheduleAlarm(final long alarmId) {
    runOrDefer(new ServiceCallback() {
      @Override
      public void run() throws RemoteException {
        clock.unscheduleAlarm(alarmId);
      }
    });
  }

  public void acknowledgeAlarm(final long alarmId) {
    runOrDefer(new ServiceCallback() {
      @Override
      public void run() throws RemoteException {
        clock.acknowledgeAlarm(alarmId);
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

  public void snoozeAlarmFor(final long alarmId, final int minutes) {
    runOrDefer(new ServiceCallback() {
      @Override
      public void run() throws RemoteException {
        clock.snoozeAlarmFor(alarmId, minutes);
      }
    });
  }
}
