package com.angrydoughnuts.android.alarmclock;

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.content.Context;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.View;
import android.widget.Button;

public class AlarmNotificationActivity extends Activity {

  private AlarmClockServiceBinder service;
  private PowerManager.WakeLock wakeLock;
  private KeyguardLock screenLock;

  // TODO(cgallek): This doesn't seem to handle the case when a second alarm
  // fires while the first has not yet been acked.
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.notification);

    service = AlarmClockServiceBinder.newBinder(getApplicationContext());

    PowerManager powerManager =
      (PowerManager) getSystemService(Context.POWER_SERVICE);
    wakeLock = powerManager.newWakeLock(
        PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
        "Alarm Notification Wake Lock");
    wakeLock.setReferenceCounted(false);
    wakeLock.acquire();

    KeyguardManager screenLockManager =
      (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
    screenLock = screenLockManager.newKeyguardLock(
        "AlarmNotification screen lock");
    screenLock.disableKeyguard();
    // TODO(cgallek): Probably should move these lock aquirings to OnResume
    // or OnStart or something...

    Button okButton = (Button) findViewById(R.id.notify_ok);

    okButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        Bundle extras = getIntent().getExtras();
        int id = extras.getInt("task_id");
        service.clearAlarm(id);
        finish();
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
  }

  // TODO(cgallek): Clicking the power button twice while this activity is
  // in the foreground seems to bypass the keyguard...

  // TODO(cgallek):  Currently, this activity only releases its locks when
  // finish() is called.  The activity can be hidden using the back or home
  // (and probably other) buttons, though.  Figure out how to handle this.
  @Override
  protected void onDestroy() {
    super.onDestroy();
    screenLock.reenableKeyguard();
    wakeLock.release();
  }

}
