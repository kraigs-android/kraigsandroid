package com.angrydoughnuts.android.alarmclock;

import android.app.Activity;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.View;
import android.widget.Button;

public class AlarmClockActivity extends Activity {
  private AlarmClockServiceBinder service;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);

    service = AlarmClockServiceBinder.newBinderAndStart(getApplicationContext());

    Button setBtn = (Button) findViewById(R.id.set_alarm);
    Button clearBtn = (Button) findViewById(R.id.clear_alarm);

    setBtn.setOnClickListener(new View.OnClickListener() {
      public void onClick(View view) {
        try {
          service.clock().alarmOn();
        } catch (RemoteException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    });
    clearBtn.setOnClickListener(new View.OnClickListener() {
      public void onClick(View view) {
        try {
          service.clock().alarmOff();
        } catch (RemoteException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    });
  }

  @Override
  protected void onResume() {
    super.onResume();
    service.bind();
  }

  @Override
  protected void onPause() {
    super.onPause();
    service.unbind();
    finish();
  }
}
