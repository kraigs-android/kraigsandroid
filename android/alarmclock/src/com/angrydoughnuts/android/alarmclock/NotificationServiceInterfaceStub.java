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

import com.angrydoughnuts.android.alarmclock.NotificationService.NoAlarmsException;

import android.content.Context;
import android.os.RemoteException;
import android.widget.Toast;

public class NotificationServiceInterfaceStub extends NotificationServiceInterface.Stub {
  private NotificationService service;

  public NotificationServiceInterfaceStub(NotificationService service) {
    this.service = service;
  }

  @Override
  public long currentAlarmId() throws RemoteException {
    try {
      return service.currentAlarmId();
    } catch (NoAlarmsException e) {
      throw new RemoteException();
    }
  }

  public int firingAlarmCount() throws RemoteException {
    return service.firingAlarmCount();
  }

  @Override
  public float volume() throws RemoteException {
    return service.volume();
  }

  @Override
  public void acknowledgeCurrentNotification(int snoozeMinutes) throws RemoteException {
    debugToast("STOP NOTIFICATION");
    try {
      service.acknowledgeCurrentNotification(snoozeMinutes);
    } catch (NoAlarmsException e) {
      throw new RemoteException();
    }
  }

  private void debugToast(String message) {
    Context context = service.getApplicationContext();
    if (AppSettings.isDebugMode(context)) {
      Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
  }
}
