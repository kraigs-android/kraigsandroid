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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.util.Calendar;

public class AlarmOptions extends DialogFragment {
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    super.onCreateDialog(savedInstanceState);

    final long id = getArguments().getLong(
        AlarmNotificationService.ALARM_ID, -1);
    final Uri uri = ContentUris.withAppendedId(
        AlarmClockProvider.ALARMS_URI, id);

    Cursor c = getContext().getContentResolver().query(
        AlarmClockProvider.ALARMS_URI,
        new String[] { AlarmClockProvider.AlarmEntry.TIME,
                       AlarmClockProvider.AlarmEntry.ENABLED,
                       AlarmClockProvider.AlarmEntry.NAME,
                       AlarmClockProvider.AlarmEntry.DAY_OF_WEEK },
        AlarmClockProvider.AlarmEntry._ID + " == " + id,
        null, null);
    c.moveToFirst();
    final int time =
      c.getInt(c.getColumnIndex(AlarmClockProvider.AlarmEntry.TIME));
    final int enabled =
      c.getInt(c.getColumnIndex(AlarmClockProvider.AlarmEntry.ENABLED));
    final String label =
      c.getString(c.getColumnIndex(AlarmClockProvider.AlarmEntry.NAME));
    final int repeat =
      c.getInt(c.getColumnIndex(AlarmClockProvider.AlarmEntry.DAY_OF_WEEK));
    c.close();

    final View v =
      ((LayoutInflater)getContext().getSystemService(
          Context.LAYOUT_INFLATER_SERVICE)).inflate(
              R.layout.alarm_options, null);

    final Button edit_time = (Button)v.findViewById(R.id.edit_time);
    edit_time.setText(
        TimeUtil.formatLong(getContext(), TimeUtil.nextOccurrence(time)));
    final TimePicker.OnTimePickListener tlist = new TimePicker.OnTimePickListener() {
        @Override
        public void onTimePick(int t) {
          ContentValues val = new ContentValues();
          val.put(AlarmClockProvider.AlarmEntry.TIME, t);
          getContext().getContentResolver().update(
              uri, val, null, null);

          final Calendar next = TimeUtil.nextOccurrence(t);
          if (enabled != 0) {
            AlarmNotificationService.removeAlarmTrigger(
                getContext(), id);
            AlarmNotificationService.scheduleAlarmTrigger(
                getContext(), id, next.getTimeInMillis());
          }

          edit_time.setText(TimeUtil.formatLong(getContext(), next));
        }
      };
    if (savedInstanceState != null) {
      TimePicker t = (TimePicker)getFragmentManager()
        .findFragmentByTag("edit_alarm");
      if (t != null)
        t.setListener(tlist);
    }
    edit_time.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            TimePicker time_pick = new TimePicker();
            time_pick.setListener(tlist);

            Cursor c = getContext().getContentResolver().query(
                AlarmClockProvider.ALARMS_URI,
                new String[] { AlarmClockProvider.AlarmEntry.TIME },
                AlarmClockProvider.AlarmEntry._ID + " == " + id,
                null, null);
            c.moveToFirst();
            final int time =
              c.getInt(c.getColumnIndex(AlarmClockProvider.AlarmEntry.TIME));
            c.close();

            Bundle b = new Bundle();
            b.putInt(TimePicker.TIME, time);
            b.putString(TimePicker.TITLE, "Edit time");
            time_pick.setArguments(b);
            time_pick.show(getFragmentManager(), "edit_alarm");
          }
        });

    final Button edit_repeat = (Button)v.findViewById(R.id.edit_repeat);
    edit_repeat.setText("" + repeat);
    final RepeatEditor.OnPickListener elist =new RepeatEditor.OnPickListener() {
        @Override
        public void onPick(int repeats) {
          ContentValues val = new ContentValues();
          val.put(AlarmClockProvider.AlarmEntry.DAY_OF_WEEK, repeats);
          getContext().getContentResolver().update(
              uri, val, null, null);
          edit_repeat.setText("" + repeats);
          // TODO: updated triggers
        }
      };
    if (savedInstanceState != null) {
      RepeatEditor e = (RepeatEditor)getFragmentManager()
        .findFragmentByTag("edit_repeat");
      if (e != null)
        e.setListener(elist);
    }
    edit_repeat.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            Cursor c = getContext().getContentResolver().query(
                AlarmClockProvider.ALARMS_URI,
                new String[] { AlarmClockProvider.AlarmEntry.DAY_OF_WEEK },
                AlarmClockProvider.AlarmEntry._ID + " == " + id,
                null, null);
            c.moveToFirst();
            final int repeat =
              c.getInt(c.getColumnIndex(AlarmClockProvider.AlarmEntry.DAY_OF_WEEK));
            c.close();

            RepeatEditor edit = new RepeatEditor();
            Bundle b = new Bundle();
            b.putInt(RepeatEditor.BITMASK, repeat);
            edit.setArguments(b);
            edit.setListener(elist);
            edit.show(getFragmentManager(), "edit_repeat");
          }
        });

    final EditText edit_label = (EditText)v.findViewById(R.id.edit_label);
    edit_label.setText(label);
    edit_label.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View view) {
          }
        });

    return new AlertDialog.Builder(getContext())
      .setTitle("Alarm Options")
      .setView(v)
      .setPositiveButton("Done", new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {}
        })
      .setNeutralButton("Delete", new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            new DialogFragment() {
              @Override
              public Dialog onCreateDialog(Bundle savedInstanceState) {
                return new AlertDialog.Builder(getContext())
                  .setTitle("Confirm Delete")
                  .setMessage("Are you sure you want to delete this alarm?")
                  .setNegativeButton(
                      "Cancel", new DialogInterface.OnClickListener() {
                      @Override
                      public void onClick(DialogInterface dialog, int which) {}
                    })
                  .setPositiveButton(
                      "OK", new DialogInterface.OnClickListener() {
                      @Override
                      public void onClick(DialogInterface dialog, int which) {
                        getContext().getContentResolver().delete(
                            ContentUris.withAppendedId(
                                AlarmClockProvider.ALARMS_URI, id), null, null);
                        AlarmNotificationService.removeAlarmTrigger(
                            getContext(), id);
                      }
                    })
                  .create();
              }
            }.show(getFragmentManager(), "confirm_delete");
          }
        })
      .create();
  }

  static public class RepeatEditor extends DialogFragment {
    final public static String BITMASK = "bitmask";
    public static interface OnPickListener {
      abstract void onPick(int repeats);
    }

    private boolean bit(int bit) { return (b & 1 << bit) != 0; }
    private int b = 0;
    private OnPickListener listener = null;
    public void setListener(OnPickListener l) { listener = l; }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
      super.onCreateDialog(savedInstanceState);

      if (getArguments() != null)
        b = getArguments().getInt(BITMASK, 0);

      if (savedInstanceState != null)
        b = savedInstanceState.getInt(BITMASK);

      final boolean checked[] = new boolean[] {
        bit(0), bit(1), bit(2), bit(3), bit(4), bit(5), bit(6), bit(7)
      };
      final CharSequence days[] = new CharSequence[] {
        "Sunday",
        "Monday",
        "Tuesday",
        "Wednesday",
        "Thursday",
        "Friday",
        "Saturday"
      };
      return new AlertDialog.Builder(getContext())
        .setTitle("Repeat")
        .setMultiChoiceItems(
            days, checked,
            new DialogInterface.OnMultiChoiceClickListener() {
              @Override
              public void onClick(
                  DialogInterface d, int which, boolean checked) {
                b ^= 1 << which;
              }
            })
        .setNegativeButton(
            "Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {}
              })
        .setPositiveButton(
            "OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                  if (listener != null)
                    listener.onPick(b);
                }
              })
        .setNeutralButton(
            "All", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
              })
        .create();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
      super.onSaveInstanceState(outState);
      outState.putInt(BITMASK, b);
    }
  }
}
