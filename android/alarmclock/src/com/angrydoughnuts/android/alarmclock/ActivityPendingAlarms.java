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

public class ActivityPendingAlarms extends Activity {
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
    Intent i = new Intent(getApplicationContext(), AlarmClockService.class);
    bindService(i, connection, Service.BIND_AUTO_CREATE);
  }

  @Override
  protected void onPause() {
    super.onPause();
    if (connected) {
      unbindService(connection);
    }
  }

  private ServiceConnection connection = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
      connected = true;
      AlarmClockInterface clock = AlarmClockInterface.Stub.asInterface(service);
      try {
        ArrayAdapter<AlarmTime> adapter = new ArrayAdapter<AlarmTime>(
            getApplicationContext(), R.layout.pending_alarm, clock.pendingAlarmTimes());
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
