package com.angrydoughnuts.android.alarmclock;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class AlarmClockActivity extends Activity {

  private AlarmClockInterface clock;
  final private ServiceConnection serviceConnection = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
      Toast.makeText(getApplicationContext(), "Service Connected " + name,
          Toast.LENGTH_SHORT).show();
      clock = AlarmClockInterface.Stub.asInterface(service);
    }
    @Override
    public void onServiceDisconnected(ComponentName name) {
      // TODO(cgallek): This should only happen if the AlarmClockService
      // crashes.  Consider throwing an exception here.
      Toast.makeText(getApplicationContext(), "Service Disconnected " + name,
          Toast.LENGTH_SHORT).show();
      clock = null;
    }
  };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);

    Button setBtn = (Button) findViewById(R.id.set_alarm);
    Button clearBtn = (Button) findViewById(R.id.clear_alarm);

    setBtn.setOnClickListener(new View.OnClickListener() {
      public void onClick(View view) {
        try {
          clock.alarmOn();
        } catch (RemoteException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    });
    clearBtn.setOnClickListener(new View.OnClickListener() {
      public void onClick(View view) {
        try {
          clock.alarmOff();
        } catch (RemoteException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    });
  }

  @Override
  protected void onResume() {
    super.onStart();
    final Intent serviceIntent =
      new Intent(getApplicationContext(), AlarmClockService.class);
    if (!bindService(
        serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)) {
      throw new IllegalStateException("Unable to bind to AlarmClock service.");
    }
  }

  @Override
  protected void onPause() {
    super.onStop();
    unbindService(serviceConnection);
  }
}
