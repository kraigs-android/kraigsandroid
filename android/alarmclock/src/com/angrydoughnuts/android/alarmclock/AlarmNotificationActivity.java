package com.angrydoughnuts.android.alarmclock;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class AlarmNotificationActivity extends Activity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.notification);

    Button okButton = (Button) findViewById(R.id.notify_ok);

    okButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        finish();
      }
    });
  }

  @Override
  protected void onPause() {
    super.onPause();
    finish();
  }
}
