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

public class NotificationServiceBinder {
  private Context context;
  private NotificationServiceInterface notify;
  private LinkedList<ServiceCallback> callbacks;

  NotificationServiceBinder(Context context) {
    this.context = context;
    this.callbacks = new LinkedList<ServiceCallback>();
  }

  public void bind() {
    final Intent serviceIntent = new Intent(context, NotificationService.class);
    if (!context.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)) {
      throw new IllegalStateException("Unable to bind to NotificationService.");
    }
  }

  public void unbind() {
    context.unbindService(serviceConnection);
    notify = null;
  }

  public interface ServiceCallback {
    void run(NotificationServiceInterface service);
  }

  final private ServiceConnection serviceConnection = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
      notify = NotificationServiceInterface.Stub.asInterface(service);
      while (callbacks.size() > 0) {
        ServiceCallback callback = callbacks.remove();
        callback.run(notify);
      }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
      notify = null;
    }
  };

  public void call(ServiceCallback callback) {
    if (notify != null) {
      callback.run(notify);
    } else {
      callbacks.offer(callback);
    }
  }

  public void acknowledgeCurrentNotification(final int snoozeMinutes) {
    call(new ServiceCallback() {
      @Override
      public void run(NotificationServiceInterface service) {
        try {
          service.acknowledgeCurrentNotification(snoozeMinutes);
        } catch (RemoteException e) {
          e.printStackTrace();
        }
      }
    });
  }
}
