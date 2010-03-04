package com.angrydoughnuts.android.alarmclock;

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class AlarmNotificationActivity extends Activity {

  private int alarmId;
  private AlarmClockServiceBinder service;
  private KeyguardLock screenLock;

  // TODO(cgallek): This doesn't seem to handle the case when a second alarm
  // fires while the first has not yet been acked.
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.notification);
    Bundle extras = getIntent().getExtras();
    this.alarmId = extras.getInt("task_id");

    service = AlarmClockServiceBinder.newBinder(getApplicationContext());

    KeyguardManager screenLockManager =
      (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
    screenLock = screenLockManager.newKeyguardLock(
        "AlarmNotification screen lock");

    Button okButton = (Button) findViewById(R.id.notify_ok);
    okButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        finish();
      }
    });
  }

  @Override
  protected void onResume() {
    super.onResume();
    service.bind();
    screenLock.disableKeyguard();
  }

  @Override
  protected void onPause() {
    super.onPause();
    screenLock.reenableKeyguard();
    // TODO(cgallek): It's important to acknowledge the alarm in here some way
    // so that the power locks are released by the service when this dialog
    // goes away.
    service.acknowledgeAlarm(alarmId);
    service.unbind();
  }

  // TODO(cgallek): Clicking the power button twice while this activity is
  // in the foreground seems to bypass the keyguard...
}
