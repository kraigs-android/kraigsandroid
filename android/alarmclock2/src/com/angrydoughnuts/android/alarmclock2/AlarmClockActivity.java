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
            ((TextView) v).setText("" + c.getLong(index));
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

    ((Button)findViewById(R.id.new_alarm)).setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            // TODO: input values
            Bundle b = new Bundle();
            //b.putLong("time", System.currentTimeMillis());

            DialogFragment f = new DialogFragment() {
                @Override
                public Dialog onCreateDialog(Bundle savedInstanceState) {
                  final Calendar c = Calendar.getInstance();
                  final View v = getLayoutInflater().inflate(R.layout.time_picker, null);
                  // TODO: input values
                  //getArguments().getLong("time");
                  // TODO: HOUR/HOUR_OF_DAY for AM/PM??
                  final EditText e = (EditText)v.findViewById(R.id.time_entry);
                  e.setText(String.format("%02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE)));
                  e.addTextChangedListener(new TextWatcher() {
                      @Override
                      public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
                      @Override
                      public void onTextChanged(CharSequence s, int st, int b, int c) {}
                      @Override
                      public void afterTextChanged(Editable s) {
                        String hhmm = s.toString().replaceAll(":", "");
                        if (hhmm.isEmpty()) {
                          c.set(Calendar.HOUR_OF_DAY, 0);
                          c.set(Calendar.MINUTE, 0);
                        } else if (hhmm.length() <= 2) {
                          c.set(Calendar.HOUR_OF_DAY, 0);
                          c.set(Calendar.MINUTE, Integer.parseInt(hhmm));
                        } else {
                          c.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hhmm.substring(0, hhmm.length() - 2)));
                          c.set(Calendar.MINUTE, Integer.parseInt(hhmm.substring(hhmm.length() - 2, hhmm.length())));
                        }
                        e.removeTextChangedListener(this);
                        e.setText(String.format("%02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE)));
                        e.setSelection(e.getText().length());
                        e.addTextChangedListener(this);
                      }
                    });

                  final AlertDialog d = new AlertDialog.Builder(getContext())
                    .setTitle("New Alarm")
                    //.setIcon()
                    .setView(v)
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {}
                    })
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                          Toast.makeText(
                              getContext(),
                              String.format("%02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE)),
                              Toast.LENGTH_SHORT).show();
                        }
                      })
                    .create();
                  e.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                      @Override
                      public boolean onEditorAction(TextView v, int action, KeyEvent e) {
                        d.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
                        return true;
                      }
                    });

                  return d;
                }
              };

            f.setArguments(b);
            f.show(getFragmentManager(), "new_alarm");
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
