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
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.LoaderManager;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.Runnable;
import java.util.Calendar;

public class AlarmClockActivity extends Activity {
  private final Handler handler = new Handler();
  private Runnable refresh_tick;
  private final TimePicker.OnTimePickListener new_alarm =
    new TimePicker.OnTimePickListener() {
      @Override
      public void onTimePick(int secondsPastMidnight) {
        AlarmNotificationService.newAlarm(
            getApplicationContext(), secondsPastMidnight);
      }
    };

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.alarm_list);

    final ResourceCursorAdapter adapter = new ResourceCursorAdapter(
        this, R.layout.alarm_list_item, null, 0) {
        @Override
        public void bindView(View v, Context context, Cursor c) {
          final int secondsPastMidnight = c.getInt(
              c.getColumnIndex(AlarmClockProvider.AlarmEntry.TIME));
          final int enabled = c.getInt(
              c.getColumnIndex(AlarmClockProvider.AlarmEntry.ENABLED));
          final Calendar next = TimeUtil.nextOccurrence(secondsPastMidnight);

          ((TextView)v.findViewById(R.id.debug_text))
            .setText(TimeUtil.formatLong(getApplicationContext(), next));
          ((TextView)v.findViewById(R.id.countdown))
            .setText(TimeUtil.until(next));
          ((CheckBox)v.findViewById(R.id.enabled))
            .setChecked(enabled != 0);
        }
      };

    ListView list = (ListView)findViewById(R.id.alarm_list);
    list.setAdapter(adapter);
    list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View v, int x, long id) {
          boolean check = ((CheckBox)v.findViewById(R.id.enabled)).isChecked();

          ContentValues val = new ContentValues();
          val.put(AlarmClockProvider.AlarmEntry.ENABLED, !check);
          getContentResolver().update(
              ContentUris.withAppendedId(AlarmClockProvider.ALARMS_URI, id),
              val, null, null);

          if (check) {
            AlarmNotificationService.removeAlarmNotification(
                getApplicationContext(), id);
          } else {
            Cursor c = getContentResolver().query(
                AlarmClockProvider.ALARMS_URI,
                new String[] { AlarmClockProvider.AlarmEntry.TIME },
                AlarmClockProvider.AlarmEntry._ID + " == " + id,
                null, null);
            c.moveToFirst();
            int secondsPastMidnight =
              c.getInt(c.getColumnIndex(AlarmClockProvider.AlarmEntry.TIME));
            c.close();
            Calendar alarm = TimeUtil.nextOccurrence(secondsPastMidnight);
            AlarmNotificationService.scheduleAlarmNotification(
                getApplicationContext(), id, alarm.getTimeInMillis());
          }
        }
      });
    list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> p, View v, int x, long id) {
          getContentResolver().delete(
              ContentUris.withAppendedId(AlarmClockProvider.ALARMS_URI, id),
              null, null);

          AlarmNotificationService.removeAlarmNotification(
              getApplicationContext(), id);
          return true;
        }
      });

    final Loader<Cursor> loader = getLoaderManager().initLoader(
        0, null, new LoaderManager.LoaderCallbacks<Cursor>() {
            @Override
            public Loader<Cursor> onCreateLoader(int id, Bundle args) {
              return new CursorLoader(
                  getApplicationContext(), AlarmClockProvider.ALARMS_URI,
                  new String[] {
                    AlarmClockProvider.AlarmEntry._ID,
                    AlarmClockProvider.AlarmEntry.TIME,
                    AlarmClockProvider.AlarmEntry.ENABLED },
                  null, null, AlarmClockProvider.AlarmEntry.TIME + " ASC");
            }
            @Override
            public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
              adapter.changeCursor(data);
            }
            @Override
            public void onLoaderReset(Loader<Cursor> loader) {
              adapter.changeCursor(null);
            }
          });
    refresh_tick = new Runnable() {
        @Override
        public void run() {
          loader.forceLoad();
          handler.postDelayed(refresh_tick, TimeUtil.nextMinuteDelay());
        }
      };

    ((Button)findViewById(R.id.test_alarm)).setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            final Calendar c = Calendar.getInstance();
            final int secondsPastMidnight = 5 +
              c.get(Calendar.HOUR_OF_DAY) * 3600 +
              c.get(Calendar.MINUTE) * 60 +
              c.get(Calendar.SECOND);
            AlarmNotificationService.newAlarm(
                getApplicationContext(), secondsPastMidnight);
          }
        });

    // Listener can not be serialized in time picker, so it must be explicitly
    // set each time.
    if (savedInstanceState != null) {
      TimePicker t = (TimePicker)getFragmentManager()
        .findFragmentByTag("new_alarm");
      if (t != null)
        t.setListener(new_alarm);
    }
    ((Button)findViewById(R.id.new_alarm)).setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            TimePicker time_pick = new TimePicker();
            time_pick.setListener(new_alarm);
            // TODO: input values
            //Bundle b = new Bundle();
            //b.putLong("time", System.currentTimeMillis());
            // time_pick.setArguments(b);
            time_pick.show(getFragmentManager(), "new_alarm");
          }
        });
  }

  @Override
  public void onStart() {
    super.onStart();
    handler.postDelayed(refresh_tick, TimeUtil.nextMinuteDelay());
  }

  @Override
  public void onStop() {
    super.onStop();
    handler.removeCallbacks(refresh_tick);
  }
}
