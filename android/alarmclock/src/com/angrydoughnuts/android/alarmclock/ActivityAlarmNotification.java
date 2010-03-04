package com.angrydoughnuts.android.alarmclock;

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.provider.Settings;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;

// NOTE: This class assumes that it will never be instantiated nor active
// more than once at the same time. (ie, it assumes
// android:launchMode="singleInstance" is set in the manifest file).
// Current reasons for this assumption:
//  - It does not support more than one active alarm at a time.  If a second
//    alarm triggers while this Activity is running, it will silently snooze
//    the first alarm and start the second.
public class ActivityAlarmNotification extends Activity {
  enum AckStates { UNACKED, ACKED, SNOOZED }

  // Per-intent members.
  private long alarmId;
  private AlarmSettings settings;
  private AckStates ackState;

  // Per-instance members.
  private AlarmClockServiceBinder service;
  private DbAccessor db;
  private KeyguardLock screenLock;
  private MediaPlayer mediaPlayer;
  private Ringtone fallbackSound;
  private Vibrator vibrator;
  private Handler handler;
  private VolumeIncreaser volumeIncreaseCallback; 
  private Runnable timeTick;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.notification);

    service = AlarmClockServiceBinder.newBinder(getApplicationContext());
    db = new DbAccessor(getApplicationContext());

    alarmId = AlarmClockService.alarmUriToId(getIntent().getData());
    settings = db.readAlarmSettings(alarmId);
    ackState = AckStates.UNACKED;

    AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
    audio.setStreamVolume(AudioManager.STREAM_ALARM, audio.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0);
    mediaPlayer = new MediaPlayer();
    mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
    fallbackSound = RingtoneManager.getRingtone(getApplicationContext(),
        Settings.System.DEFAULT_ALARM_ALERT_URI); 
    if (fallbackSound == null) {
      Uri superFallback = RingtoneManager.getValidRingtoneUri(getApplicationContext());
      fallbackSound = RingtoneManager.getRingtone(getApplicationContext(), superFallback);
    }
    if (fallbackSound != null) {
      fallbackSound.setStreamType(AudioManager.STREAM_ALARM);
    }
    vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

    handler = new Handler();
    volumeIncreaseCallback = new VolumeIncreaser();

    timeTick = new Runnable() {
      @Override
      public void run() {
        // TODO(cgallek): Make this control a custom clock too.
        // Some sound should always be playing.
        if (!mediaPlayer.isPlaying() &&
            fallbackSound != null && !fallbackSound.isPlaying()) { 
          fallbackSound.play();
        }

        int intervalMillis = 1000;  // every second
        long now = System.currentTimeMillis();
        long next = intervalMillis - now % intervalMillis;
        handler.postDelayed(timeTick, next);
      }
    };

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

    Button decreaseSnoozeButton = (Button) findViewById(R.id.notify_snooze_minus_five);
    decreaseSnoozeButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        int snooze = settings.getSnoozeMinutes() - 5;
        if (snooze < 5) {
          snooze = 5;
        }
        settings.setSnoozeMinutes(snooze);
        redraw();
      }
    });

    Button increaseSnoozeButton = (Button) findViewById(R.id.notify_snooze_plus_five);
    increaseSnoozeButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        int snooze = settings.getSnoozeMinutes() + 5;
        if (snooze > 60) {
          snooze = 60;
        }
        settings.setSnoozeMinutes(snooze);
        redraw();
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

  // If a second alarm fires while this activity is active, a cycle through
  // onPause(), onNewIntent(), to onResume() will be triggered.
  // This call to onNewIntent() swaps out the per-intent member variables
  // for those associated with the new alarm.
  // Each of these cycles should represent a single call to lock on the
  // wake lock in the alarm receiver, and one unlock call via the ack method.
  @Override
  protected void onNewIntent(Intent intent) {
    alarmId = AlarmClockService.alarmUriToId(intent.getData());
    settings = db.readAlarmSettings(alarmId);
    ackState = AckStates.UNACKED;
    volumeIncreaseCallback.reset();
    redraw();
    super.onNewIntent(intent);
  }

  @Override
  protected void onStart() {
    super.onStart();
    service.bind();
  }

  @Override
  protected void onResume() {
    super.onResume();
    screenLock.disableKeyguard();
    if (settings.getVibrate()) {
      vibrator.vibrate(new long[] {500, 500}, 0);
    }

    mediaPlayer.reset();
    mediaPlayer.setLooping(true);
    try {
      mediaPlayer.setDataSource(getApplicationContext(), settings.getTone());
      mediaPlayer.prepare();
      mediaPlayer.start();
    } catch (Exception e) {
      e.printStackTrace();
    }

    handler.post(volumeIncreaseCallback);
    handler.post(timeTick);

    redraw();
  }

  @Override
  protected void onPause() {
    super.onPause();
    // If the user did not explicitly dismiss or snooze this alarm, snooze
    // it as a default.
    ack(AckStates.SNOOZED);
    handler.removeCallbacks(volumeIncreaseCallback);
    handler.removeCallbacks(timeTick);
    vibrator.cancel();
    mediaPlayer.stop();
    if (fallbackSound != null) {
      fallbackSound.stop();
    }
    screenLock.reenableKeyguard();
  }

  @Override
  protected void onStop() {
    super.onStop();
    service.unbind();
    finish();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    db.closeConnections();
    mediaPlayer.release();
    if (ackState == AckStates.UNACKED) {
      throw new IllegalStateException(
          "Alarm notification was destroyed without ever being acknowledged.");
    }
  }

  void redraw() {
    AlarmInfo alarmInfo = db.readAlarmInfo(alarmId);
    String info = alarmInfo.getTime().toString() + "\n" + alarmInfo.getName();
    if (AlarmClockService.debug(getApplicationContext())) {
      info += " [" + alarmId + "]";
      findViewById(R.id.volume).setVisibility(View.VISIBLE);
    } else {
      findViewById(R.id.volume).setVisibility(View.GONE);
    }
    TextView infoText = (TextView) findViewById(R.id.alarm_info);
    infoText.setText(info);
    TextView snoozeInfo = (TextView) findViewById(R.id.notify_snooze_time);
    snoozeInfo.setText(getString(R.string.snooze) + "\n"
        + getString(R.string.minutes, settings.getSnoozeMinutes()));
  }

  private void ack(AckStates ack) {
    if (ackState != AckStates.UNACKED) {
      return;
    } else {
      ackState = ack;
    }

    switch (ack) {
      case SNOOZED:
        service.snoozeAlarmFor(alarmId, settings.getSnoozeMinutes());
        WakeLock.release(alarmId);
        break;
      case ACKED:
        service.acknowledgeAlarm(alarmId);
        WakeLock.release(alarmId);
        break;
      default:
        throw new IllegalStateException("Unknow alarm notification state.");
    }
  }

  private class VolumeIncreaser implements Runnable {
    float start;
    float end;
    float increment;

    public VolumeIncreaser() {
      reset();
    }

    public void reset() {
      start = (float) (settings.getVolumeStartPercent() / 100.0);
      end = (float) (settings.getVolumeEndPercent() / 100.0);
      increment = (end - start) / (float) settings.getVolumeChangeTimeSec();
      mediaPlayer.setVolume(start, start);
    }

    @Override
    public void run() {
      start += increment;
      if (start > end) {
        start = end;
      }
      mediaPlayer.setVolume(start, start);
      TextView volume = (TextView) findViewById(R.id.volume);
      volume.setText("Volume: " + start);

      if (Math.abs(start - end) > (float) 0.0001) {
        handler.postDelayed(volumeIncreaseCallback, 1000);
      }
    }
  }
}
