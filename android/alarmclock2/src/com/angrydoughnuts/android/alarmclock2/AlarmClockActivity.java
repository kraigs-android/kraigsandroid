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
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;

public class AlarmClockActivity extends Activity {
  private AlarmClockService service = null;
  private final ServiceConnection connection = new ServiceConnection() {
      @Override
      public void onServiceConnected(ComponentName name, IBinder binder) {
        service = ((AlarmClockService.IdentityBinder)binder).getService();
      }
      @Override
      public void onServiceDisconnected(ComponentName name) {
        service = null;
      }
    };
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

    final SimpleCursorAdapter adapter = new SimpleCursorAdapter(
        this, R.layout.alarm_list_item, null,
        new String[] {
          AlarmClockProvider.AlarmEntry.TIME,
          AlarmClockProvider.AlarmEntry.ENABLED },
        new int[] {
          R.id.debug_text,
          R.id.enabled }, 0);
    adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
        @Override
        public boolean setViewValue(View v, Cursor c, int index) {
          switch (c.getColumnName(index)) {
          case AlarmClockProvider.AlarmEntry.TIME:
            // TODO AM/PM
            int secondsPastMidnight = c.getInt(index);
            int hour = secondsPastMidnight / 3600;
            int minute = secondsPastMidnight / 60 - hour * 60;
            ((TextView) v).setText(String.format("%02d:%02d", hour, minute));
            return true;
          case AlarmClockProvider.AlarmEntry.ENABLED:
            ((CheckBox) v).setChecked(c.getInt(index) != 0);
            return true;
          default:
            return false;
          }
        }
      });

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

          // TODO, this doesn't actually schedule the alarm yet.
          AlarmNotificationService.refreshNotifyBar(getApplicationContext());
        }
      });
    list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> p, View v, int x, long id) {
          getContentResolver().delete(
              ContentUris.withAppendedId(AlarmClockProvider.ALARMS_URI, id),
              null, null);

          // TODO, this doesn't actually clear the alarm yet.
          AlarmNotificationService.refreshNotifyBar(getApplicationContext());
          return true;
        }
      });

    getLoaderManager().initLoader(
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

    ((Button)findViewById(R.id.test_alarm)).setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            if (service != null) service.createTestAlarm();
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
    if (service == null) {
      bindService(new Intent(this, AlarmClockService.class), connection,
                  Context.BIND_AUTO_CREATE);
    }
  }

  @Override
  public void onStop() {
    super.onStop();
    if (service != null) {
      unbindService(connection);
      service = null;
    }
  }
}
