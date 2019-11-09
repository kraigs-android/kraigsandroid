/****************************************************************************
 * Copyright 2016 kraigs.android@gmail.com
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
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashSet;

public class AlarmNotificationActivity extends Activity {
  public static final String TIMEOUT = "timeout";

  private static final String TAG =
    AlarmNotificationActivity.class.getSimpleName();

  private int snooze;

  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    setContentView(R.layout.notification);
    // Make sure this window always shows over the lock screen.
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);

    // Pull snooze from saved state.
    if (state != null && state.containsKey("snooze")) {
      snooze = state.getInt("snooze");
    }

    final TextView snooze_text = (TextView)findViewById(R.id.snooze_text);
    findViewById(R.id.snooze_minus_five).setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            snooze -= 5;
            if (snooze <= 0) snooze = 5;
            snooze_text.setText(getString(R.string.minutes, snooze));
          }
        });

    findViewById(R.id.snooze_plus_five).setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            snooze += 5;
            if (snooze >= 60) snooze = 60;
            snooze_text.setText(getString(R.string.minutes, snooze));
          }
        });

    findViewById(R.id.snooze_alarm).setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            AlarmNotificationService.snoozeAllAlarms(
                getApplicationContext(),
                TimeUtil.nextMinute(snooze).getTimeInMillis());
            finish();
          }
        });

    ((Slider)findViewById(R.id.dismiss_alarm)).setListener(
        new Slider.Listener() {
          @Override
          public void onComplete() {
            AlarmNotificationService.dismissAllAlarms(getApplicationContext());
            finish();
          }
        });
  }

  @Override
  protected void onResume() {
    super.onResume();

    ArrayList<String> labels = new ArrayList<String>();
    for (long alarmid : AlarmNotificationService.getActiveAlarms()) {
      if (snooze <= 0) {
        snooze = DbUtil.Settings.get(this, alarmid).snooze;
      }
      String label = DbUtil.Alarm.get(this, alarmid).label;
      if (!label.isEmpty()) {
        labels.add(label);
      }
    }

    ((TextView)findViewById(R.id.snooze_text))
      .setText(getString(R.string.minutes, snooze));

    if (!labels.isEmpty()) {
      ((TextView)findViewById(R.id.alarm_label))
        .setText(TextUtils.join(", ", labels));
    }
  }

  @Override
  protected void onNewIntent(Intent i) {
    // The notification service can get us here for one of two reasons:
    super.onNewIntent(i);

    // A firing alarm has run long enough to trigger a timeout.
    if (i.hasExtra(TIMEOUT)) {
      new TimeoutMessage().show(getFragmentManager(), "timeout");
    }
    // Another alarm triggered before the current one one has been dismissed.
    // This triggers onResume() to redraw.
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putInt("snooze", snooze);
  }

  public static class TimeoutMessage extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
      return new AlertDialog.Builder(getContext())
        .setTitle(R.string.time_out_title)
        .setMessage(R.string.time_out_error)
        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface d, int i) {
              getActivity().finish();
            }
          })
        .create();
    }
  }
}
