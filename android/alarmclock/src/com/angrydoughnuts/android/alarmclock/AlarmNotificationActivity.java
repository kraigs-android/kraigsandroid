package com.angrydoughnuts.android.alarmclock;

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.view.View;
import android.widget.Button;

public class AlarmNotificationActivity extends Activity {

  private PowerManager.WakeLock wakeLock;
  private KeyguardLock screenLock;

  private AlarmClockInterface clock;
  final private ServiceConnection serviceConnection = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
      clock = AlarmClockInterface.Stub.asInterface(service);
    }
    @Override
    public void onServiceDisconnected(ComponentName name) {
      // TODO(cgallek): This should only happen if the AlarmClockService
      // crashes.  Consider throwing an exception here.
      clock = null;
    }
  };

  // TODO(cgallek): This doesn't seem to handle the case when a second alarm
  // fires while the first has not yet been acked.
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.notification);

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
        try {
          clock.clearAlarm(id);
        } catch (RemoteException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
        finish();
      }
    });
  }

  @Override
  protected void onResume() {
    super.onResume();
    final Intent serviceIntent =
      new Intent(getApplicationContext(), AlarmClockService.class);
    if (!bindService(
        serviceIntent, serviceConnection, 0)) {
      throw new IllegalStateException("Unable to bind to AlarmClock service.");
    }
  }

  @Override
  protected void onPause() {
    super.onPause();
    unbindService(serviceConnection);
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
