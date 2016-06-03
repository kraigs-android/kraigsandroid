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

package com.angrydoughnuts.android.alarmclock2;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

public class AlarmNotificationActivity extends Activity {
  private static final String TAG =
    AlarmNotificationActivity.class.getSimpleName();

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.notification);
    // Make sure this window always shows over the lock screen.
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);

    final long alarmid =
      getIntent().getLongExtra(AlarmNotificationService.ALARM_ID, -1);
    if (alarmid != -1) {
      Log.i(TAG, "Alarm notification intent " + alarmid);
    }

    ((Button)findViewById(R.id.dismiss_alarm)).setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            AlarmNotificationService.dismissAllAlarms(getApplicationContext());
            finish();
          }
        });
  }

  @Override
  protected void onNewIntent(Intent i) {
    super.onNewIntent(i);

    final long alarmid = i.getLongExtra(AlarmNotificationService.ALARM_ID, -1);
    if (alarmid != -1) {
      Log.i(TAG, "Another alarm notification intent " + alarmid);
    }
  }
}
