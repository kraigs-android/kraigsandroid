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
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

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
  // TODO change the launch mode of this application so that it's always
  // in front of all other activities.
public final class ActivityAlarmNotification extends Activity {
  private NotificationServiceBinder notifyService;
  private DbAccessor db;
  private KeyguardLock screenLock;
  private Handler handler;
  private Runnable timeTick;

  // Dialog state
  int snoozeMinutes;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.notification);

    db = new DbAccessor(getApplicationContext());

    // Start the notification service and bind to it.
    notifyService = new NotificationServiceBinder(getApplicationContext());
    notifyService.bind();
    // If the activity was launched from the broadcast receiver (ie, it was
    // given an alarm id to fire), trigger the alarm in the service.
    // TODO consider launching the service directly from the receiver and opening.
    // the activity from there.
    if (getIntent().getData() != null) {
      notifyService.startNotification(AlarmUtil.alarmUriToId(getIntent().getData()));
    }

    // Setup a self-scheduling event loops.
    handler = new Handler();

    timeTick = new Runnable() {
      @Override
      public void run() {
        notifyService.call(new NotificationServiceBinder.ServiceCallback() {
          @Override
          public void run(NotificationServiceInterface service) {
            try {
              TextView volume = (TextView) findViewById(R.id.volume);
              volume.setText("Volume: " + service.volume());
            } catch (RemoteException e) {}

            long next = AlarmUtil.millisTillNextInterval(AlarmUtil.Interval.SECOND);
            handler.postDelayed(timeTick, next);
          }
        });
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
        notifyService.acknowledgeCurrentNotification(snoozeMinutes);
        finish();
      }
    });

    final Button decreaseSnoozeButton = (Button) findViewById(R.id.notify_snooze_minus_five);
    decreaseSnoozeButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        int snooze = snoozeMinutes - 5;
        if (snooze < 5) {
          snooze = 5;
        }
        snoozeMinutes = snooze;
        redraw();
      }
    });

    final Button increaseSnoozeButton = (Button) findViewById(R.id.notify_snooze_plus_five);
    increaseSnoozeButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        int snooze = snoozeMinutes + 5;
        if (snooze > 60) {
          snooze = 60;
        }
        snoozeMinutes = snooze;
        redraw();
      }
    });

    final Slider dismiss = (Slider) findViewById(R.id.dismiss_slider);
    dismiss.setHintText(R.string.wake_up);
    dismiss.setOnTriggerListener(new Slider.OnTriggerListener() {
      @Override
      public void onTrigger(View v) {
        notifyService.acknowledgeCurrentNotification(0);
        finish();
      }
    });
  }

  // Handle the case of a second alarm being triggered while another is firing.
  // TODO: this could also be fixed by launching the service from the receiver
  // rather than the activity.
  @Override
  protected void onNewIntent(Intent intent) {
    if (intent.getData() != null) {
      notifyService.startNotification(AlarmUtil.alarmUriToId(intent.getData()));
    }
    super.onNewIntent(intent);
  }

  @Override
  protected void onResume() {
    super.onResume();
    WakeLock.assertAtLeastOneHeld();

    screenLock.disableKeyguard();

    handler.post(timeTick);

    redraw();
    // TODO(cgallek): This notification will continue forever unless the user
    // dismisses it.  Consider adding a timeout that dismisses it automatically
    // after some large amount of time.
  }

  @Override
  protected void onPause() {
    super.onPause();
    handler.removeCallbacks(timeTick);
    screenLock.reenableKeyguard();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    db.closeConnections();
    notifyService.unbind();
  }

  private final void redraw() {
    notifyService.call(new NotificationServiceBinder.ServiceCallback() {
      @Override
      public void run(NotificationServiceInterface service) {
        long alarmId;
        try {
          alarmId = service.currentAlarmId();
        } catch (RemoteException e) {
          return;
        }
        AlarmInfo alarmInfo = db.readAlarmInfo(alarmId);
        if (snoozeMinutes == 0) {
          snoozeMinutes = db.readAlarmSettings(alarmId).getSnoozeMinutes();
        }
        String info = alarmInfo.getTime().toString() + "\n" + alarmInfo.getName();
        if (AppSettings.isDebugMode(getApplicationContext())) {
          info += " [" + alarmId + "]";
          findViewById(R.id.volume).setVisibility(View.VISIBLE);
        } else {
          findViewById(R.id.volume).setVisibility(View.GONE);
        }
        TextView infoText = (TextView) findViewById(R.id.alarm_info);
        infoText.setText(info);
        TextView snoozeInfo = (TextView) findViewById(R.id.notify_snooze_time);
        snoozeInfo.setText(getString(R.string.snooze) + "\n"
            + getString(R.string.minutes, snoozeMinutes));
      }
    });
  }
}
