/****************************************************************************
 * Copyright 2010 kraigs.android@gmail.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 ****************************************************************************/

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
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;

/**
 * This is the activity responsible for alerting the user when an alarm goes
 * off.  It is capable of playing a tone and vibrating when started.  It should
 * normally be started from the broadcast receiver that receives messages
 * from the AlarmManager service and assumes that this receiver will have
 * aquired a wake lock before starting this activity.  This activity assumes
 * that the data supplied in the triggering intent will contain the alarm uri
 * associated with the fireing alarm.
 * NOTE: This class assumes that it will never be instantiated nor active
 * more than once at the same time. (ie, it assumes
 * android:launchMode="singleInstance" is set in the manifest file).
 * Current reasons for this assumption:
 *  - It does not support more than one active alarm at a time.  If a second
 *    alarm triggers while this Activity is running, it will silently snooze
 *    the first alarm and start the second.
 */
public final class ActivityAlarmNotification extends Activity {
  enum AckStates { UNACKED, ACKED, SNOOZED }

  // Per-intent members (changed if onNewIntent is called).
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

    // Access to in-memory and persistent data structures.
    service = new AlarmClockServiceBinder(getApplicationContext());
    db = new DbAccessor(getApplicationContext());

    // Information associated with the alarm triggered in the first intent.
    alarmId = AlarmUtil.alarmUriToId(getIntent().getData());
    settings = db.readAlarmSettings(alarmId);
    ackState = AckStates.UNACKED;

    // Setup audio.
    final AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
    // Force the alarm stream to be maximum volume.  This will allow the user
    // to select a volume between 0 and 100 percent via the settings activity.
    audio.setStreamVolume(AudioManager.STREAM_ALARM,
        audio.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0);
    // Setup the media play.
    mediaPlayer = new MediaPlayer();
    // Make it use the previously configured alarm stream.
    mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
    // The media player can fail for lots of reasons.  Try to setup a backup
    // sound for use when the media player fails.
    fallbackSound = RingtoneManager.getRingtone(getApplicationContext(),
        AlarmUtil.getDefaultAlarmUri());
    if (fallbackSound == null) {
      Uri superFallback = RingtoneManager.getValidRingtoneUri(getApplicationContext());
      fallbackSound = RingtoneManager.getRingtone(getApplicationContext(), superFallback);
    }
    // Make the fallback sound use the alarm stream as well.
    if (fallbackSound != null) {
      fallbackSound.setStreamType(AudioManager.STREAM_ALARM);
    }

    // Instantiate a vibrator.  That's fun to say.
    vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

    // Setup a self-scheduling timing loop.  This loop will begin in onResume()
    // and will be terminated in onPause();
    handler = new Handler();
    volumeIncreaseCallback = new VolumeIncreaser();

    timeTick = new Runnable() {
      @Override
      public void run() {
        // Some sound should always be playing.
        if (!mediaPlayer.isPlaying() &&
            fallbackSound != null && !fallbackSound.isPlaying()) { 
          fallbackSound.play();
        }

        long next = AlarmUtil.millisTillNextInterval(AlarmUtil.Interval.SECOND);
        handler.postDelayed(timeTick, next);
      }
    };

    // Setup the screen lock object.  The screen will be unlocked onResume() and
    // re-locked onPause();
    final KeyguardManager screenLockManager =
      (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
    screenLock = screenLockManager.newKeyguardLock(
        "AlarmNotification screen lock");

    // Setup individual UI elements.
    final Button snoozeButton = (Button) findViewById(R.id.notify_snooze);
    snoozeButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        ack(AckStates.SNOOZED);
        finish();
      }
    });

    final Button decreaseSnoozeButton = (Button) findViewById(R.id.notify_snooze_minus_five);
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

    final Button increaseSnoozeButton = (Button) findViewById(R.id.notify_snooze_plus_five);
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
    final SeekBar dismiss = (SeekBar) findViewById(R.id.dismiss_slider);
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
    alarmId = AlarmUtil.alarmUriToId(intent.getData());
    settings = db.readAlarmSettings(alarmId);
    ackState = AckStates.UNACKED;
    volumeIncreaseCallback.reset();
    redraw();
    super.onNewIntent(intent);
  }

  @Override
  protected void onResume() {
    super.onResume();
    WakeLock.assertHeld(alarmId);
    service.bind();

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
    // TODO(cgallek): This notification will continue forever unless the user
    // dismisses it.  Consider adding a timeout that dismisses it automatically
    // after some large amount of time.
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

    service.unbind();
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

  private final void redraw() {
    AlarmInfo alarmInfo = db.readAlarmInfo(alarmId);
    String info = alarmInfo.getTime().toString() + "\n" + alarmInfo.getName();
    if (DebugUtil.isDebugMode(getApplicationContext())) {
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

  private final void ack(AckStates ack) {
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

  /**
   * Helper class for gradually increasing the volume of the alarm audio
   * stream.
   */
  private final class VolumeIncreaser implements Runnable {
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
