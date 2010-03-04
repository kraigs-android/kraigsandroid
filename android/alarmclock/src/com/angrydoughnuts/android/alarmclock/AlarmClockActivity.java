package com.angrydoughnuts.android.alarmclock;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class AlarmClockActivity extends Activity {
  // TODO(cgallek): replace this with a data provider.
  private static boolean alarmOn = false;
  public static boolean getAlarmOn() { return alarmOn; }
  // TODO(cgallek): make these final.
  private Intent serviceIntent;
  private ServiceConnection serviceConnection;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    serviceIntent = new Intent(getApplication(), AlarmClockService.class);

    Button startBtn = (Button) findViewById(R.id.start_service);
    Button stopBtn = (Button) findViewById(R.id.stop_service);
    Button setBtn = (Button) findViewById(R.id.set_alarm);
    Button clearBtn = (Button) findViewById(R.id.clear_alarm);

    startBtn.setOnClickListener(new View.OnClickListener() {
      public void onClick(View view) {
        startService(serviceIntent);    
      }
    });
    stopBtn.setOnClickListener(new View.OnClickListener() {
      public void onClick(View view) {
        stopService(serviceIntent);
      }
    });
    setBtn.setOnClickListener(new View.OnClickListener() {
      public void onClick(View view) {
        alarmOn = true;
      }
    });
    clearBtn.setOnClickListener(new View.OnClickListener() {
      public void onClick(View view) {
        alarmOn = false;
      }
    });
  }

  // TODO(cgallek): should these be onStart/onStop or onResume/onPause??
  @Override
  protected void onStart() {
    super.onStart();
    serviceConnection = new ServiceConnection() {
      @Override
      public void onServiceConnected(ComponentName name, IBinder service) {
        Toast.makeText(getApplicationContext(), "Service Connected " + name,
            Toast.LENGTH_SHORT).show();
      }
      @Override
      public void onServiceDisconnected(ComponentName name) {
        Toast.makeText(getApplicationContext(), "Service Disconnected " + name,
            Toast.LENGTH_SHORT).show();
      }
    };

    boolean bindSuccess = bindService(
        serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    assert(bindSuccess);
  }

  @Override
  protected void onStop() {
    super.onStop();
    // TODO(cgallek): This doesn't seem to call onUnbind() in the service all
    // the time. I'm not sure why...  The services, therefore, doesn't always
    // auto shutdown.
    unbindService(serviceConnection);
  }
}
