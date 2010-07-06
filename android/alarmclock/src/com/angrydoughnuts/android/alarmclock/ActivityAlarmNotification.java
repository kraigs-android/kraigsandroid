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
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.content.Context;
import android.content.DialogInterface;
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
 * off.  It is the activity triggered by the NotificationService.  It assumes
 * that the intent sender has acquired a screen wake lock.
 * NOTE: This class assumes that it will never be instantiated nor active
 * more than once at the same time. (ie, it assumes
 * android:launchMode="singleInstance" is set in the manifest file).
 */
public final class ActivityAlarmNotification extends Activity {
  public final static String TIMEOUT_COMMAND = "timeout";
  private enum Dialogs { TIMEOUT }

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
    dismiss.setOnCompleteListener(new Slider.OnCompleteListener() {
      @Override
      public void complete() {
        notifyService.acknowledgeCurrentNotification(0);
        finish();
      }
    });
  }

  @Override
  protected void onResume() {
    super.onResume();
    screenLock.disableKeyguard();
    handler.post(timeTick);
    redraw();
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

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    Bundle extras = intent.getExtras();
    if (extras == null || extras.getBoolean(TIMEOUT_COMMAND, false) == false) {
      return;
    }
    // The notification service has signaled this activity for a second time.
    // This represents a acknowledgment timeout.  Display the appropriate error.
    // (which also finish()es this activity.
    showDialog(Dialogs.TIMEOUT.ordinal());
  }

  @Override
  protected Dialog onCreateDialog(int id) {
    switch (Dialogs.values()[id]) {
      case TIMEOUT:
        final AlertDialog.Builder timeoutBuilder = new AlertDialog.Builder(this);
        timeoutBuilder.setIcon(android.R.drawable.ic_dialog_alert);
        timeoutBuilder.setTitle(R.string.time_out_title);
        timeoutBuilder.setMessage(R.string.time_out_error);
        timeoutBuilder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener(){
          @Override
          public void onClick(DialogInterface dialog, int which) {}
        });
        AlertDialog dialog = timeoutBuilder.create();
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
          @Override
          public void onDismiss(DialogInterface dialog) {
            finish();
          }});
        return dialog;
      default:
        return super.onCreateDialog(id);
    }
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
