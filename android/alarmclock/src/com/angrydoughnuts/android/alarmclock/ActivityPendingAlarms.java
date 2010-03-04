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

import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.widget.ArrayAdapter;
import android.widget.ListView;

/**
 * This is a simple activity which displays all of the scheduled (in memory)
 * alarms that currently exist (For debugging only). 
 */
public final class ActivityPendingAlarms extends Activity {
  boolean connected;
  private ListView listView;
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.pending_alarms);

    connected = false;
    listView = (ListView) findViewById(R.id.pending_alarm_list);
  }

  @Override
  protected void onResume() {
    super.onResume();
    final Intent i = new Intent(getApplicationContext(), AlarmClockService.class);
    if (!bindService(i, connection, Service.BIND_AUTO_CREATE)) {
      throw new IllegalStateException("Unable to bind to AlarmClockService.");
    }
  }

  @Override
  protected void onPause() {
    super.onPause();
    if (connected) {
      unbindService(connection);
    }
  }

  private final ServiceConnection connection = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
      connected = true;
      AlarmClockInterface clock = AlarmClockInterface.Stub.asInterface(service);
      try {
        ArrayAdapter<AlarmTime> adapter = new ArrayAdapter<AlarmTime>(
            getApplicationContext(), R.layout.pending_alarms_item, clock.pendingAlarmTimes());
        listView.setAdapter(adapter);
      } catch (RemoteException e) {
        e.printStackTrace();
      }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
      connected = false;
    }
  };
}
