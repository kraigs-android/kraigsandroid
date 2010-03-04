package com.angrydoughnuts.android.alarmclock;

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class AlarmNotificationActivity extends Activity {
  enum AckStates { UNACKED, ACKED, SNOOZED }

  private long alarmId;
  private AckStates ackState;
  private AlarmClockServiceBinder service;
  private DbAccessor db;
  private KeyguardLock screenLock;
  private MediaPlayer mediaPlayer;
  private Handler handler;
  private VolumeIncreaser volumeIncreaseCallback; 

  // TODO(cgallek): This doesn't seem to handle the case when a second alarm
  // fires while the first has not yet been acked.
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.notification);

    alarmId = AlarmClockService.alarmUriToId(getIntent().getData());
    ackState = AckStates.UNACKED;

    service = AlarmClockServiceBinder.newBinder(getApplicationContext());
    db = new DbAccessor(getApplicationContext());
    mediaPlayer = new MediaPlayer();

    handler = new Handler();
    volumeIncreaseCallback = new VolumeIncreaser();

    KeyguardManager screenLockManager =
      (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
    screenLock = screenLockManager.newKeyguardLock(
        "AlarmNotification screen lock");

    Button snoozeButton = (Button) findViewById(R.id.notify_snooze);
    snoozeButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        ack(AckStates.SNOOZED);
        finish();
      }
    });

    // TODO(cgallek) replace this with a different object that implements
    // the AbsSeekBar interface?  The current version works even if you simply
    // tap the right side.
    SeekBar dismiss = (SeekBar) findViewById(R.id.dismiss_slider);
    dismiss.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
      @Override
      public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
      }
      @Override
      public void onStartTrackingTouch(SeekBar seekBar) {
      }
      @Override
      public void onStopTrackingTouch(SeekBar seekBar) {
        if (seekBar.getProgress() > 75) {
          ack(AckStates.ACKED);
          finish();
        }
        seekBar.setProgress(0);
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

    handler.post(volumeIncreaseCallback);

    AlarmTime time = db.alarmTime(alarmId);
    String info = time.toString();
    if (AlarmClockService.debug(getApplicationContext())) {
      info += " [" + alarmId + "]";
      findViewById(R.id.volume).setVisibility(View.VISIBLE);
    } else {
      findViewById(R.id.volume).setVisibility(View.GONE);
    }
    TextView alarmInfo = (TextView) findViewById(R.id.alarm_info);
    alarmInfo.setText(info);
  }

  @Override
  protected void onPause() {
    super.onPause();
    // If the user did not explicitly dismiss or snooze this alarm, snooze
    // it as a default.
    ack(AckStates.SNOOZED);
    mediaPlayer.stop();
    service.unbind();
    screenLock.reenableKeyguard();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    db.closeConnections();
    handler.removeCallbacks(volumeIncreaseCallback);
    mediaPlayer.release();
    if (ackState == AckStates.UNACKED) {
      throw new IllegalStateException(
          "Alarm notification was destroyed without ever being acknowledged.");
    }
  }

  // TODO(cgallek): this wake lock must be released once and exactly once
  // for every lock that is acquired in the BroadcastReceiver.  This
  // method should make sure it's released for every instance of this
  // Activity, but I don't think that there is necessarily a one to one mappeing
  // between broadcast events and instances of this activity.  Figure out how
  // to handle this.
  private void ack(AckStates ack) {
    if (ackState != AckStates.UNACKED) {
      return;
    } else {
      ackState = ack;
    }

    switch (ack) {
      case SNOOZED:
        service.snoozeAlarm(alarmId);
        AlarmBroadcastReceiver.wakeLock().release();
        break;
      case ACKED:
        service.dismissAlarm(alarmId);
        AlarmBroadcastReceiver.wakeLock().release();
        break;
      default:
        throw new IllegalStateException("Unknow alarm notification state.");
    }
  }

  // TODO(cgallek): make this configurable.
  private class VolumeIncreaser implements Runnable {
    float value;

    public VolumeIncreaser() {
      // TODO(cgallek): Do we need to make sure that the system media volume
      // is on?? See AudioManager.
      // TODO(cgallek): Do these need to check for error state first?
      mediaPlayer.setVolume((float)0.10, (float)0.10);
      value = (float) 0.10;
    }

    @Override
    public void run() {
      mediaPlayer.setVolume(value, value);
      TextView volume = (TextView) findViewById(R.id.volume);
      volume.setText("Volume: " + value);
      if (value >= 1) {
        return;
      }
      value += 0.05;
      handler.postDelayed(volumeIncreaseCallback, 1000);
    }
  }
}
