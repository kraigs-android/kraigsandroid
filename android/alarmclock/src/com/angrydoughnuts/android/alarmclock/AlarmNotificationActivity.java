package com.angrydoughnuts.android.alarmclock;

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class AlarmNotificationActivity extends Activity {
  private long alarmId;
  private AlarmClockServiceBinder service;
  private DbAccessor db;
  private KeyguardLock screenLock;
  private MediaPlayer mediaPlayer;

  // TODO(cgallek): This doesn't seem to handle the case when a second alarm
  // fires while the first has not yet been acked.
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.notification);

    alarmId = AlarmClockService.alarmUriToId(getIntent().getData());

    service = AlarmClockServiceBinder.newBinder(getApplicationContext());
    db = new DbAccessor(getApplicationContext());
    mediaPlayer = new MediaPlayer();

    KeyguardManager screenLockManager =
      (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
    screenLock = screenLockManager.newKeyguardLock(
        "AlarmNotification screen lock");

    Button okButton = (Button) findViewById(R.id.notify_ok);
    okButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        // TODO(cgallek):  Currently the alarm will only be acknowledged if the ok
        // button is pressed.  However, this dialog can be closed in other ways.
        // figure out how to handle acknowledgements in those cases.  Maybe
        // a notification item?
        service.dismissAlarm(alarmId);
        AlarmBroadcastReceiver.wakeLock().release();
        finish();
      }
    });

    Button snoozeButton = (Button) findViewById(R.id.notify_snooze);
    snoozeButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        // TODO(cgallek):  Currently the alarm will only be acknowledged if the ok
        // button is pressed.  However, this dialog can be closed in other ways.
        // figure out how to handle acknowledgements in those cases.  Maybe
        // a notification item?
        // TODO(cgallek): make snooze time configurable.
        service.snoozeAlarm(alarmId, 10);
        AlarmBroadcastReceiver.wakeLock().release();
        finish();
      }
    });
  }

  @Override
  protected void onResume() {
    super.onResume();
    screenLock.disableKeyguard();
    service.bind();
    // TODO(cgallek): shouldn't be default.
    Uri tone = db.readAlarmSettings(alarmId).getTone();
    mediaPlayer.reset();
    // TODO(cgallek): figure out how to make sure the volume is appropriate.
    mediaPlayer.setLooping(true);
    try {
      mediaPlayer.setDataSource(getApplicationContext(), tone);
      mediaPlayer.prepare();
      mediaPlayer.start();
    } catch (Exception e) {
      // TODO(cgallek): Come up with a better failure mode.
      e.printStackTrace();
    }
  }

  @Override
  protected void onPause() {
    super.onPause();
    mediaPlayer.stop();
    service.unbind();
    screenLock.reenableKeyguard();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    db.closeConnections();
    mediaPlayer.release();
  }
  // TODO(cgallek): Clicking the power button twice while this activity is
  // in the foreground seems to bypass the keyguard...
}
