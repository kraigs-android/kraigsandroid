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
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.lang.NumberFormatException;
import java.util.Calendar;

public class TimePicker extends DialogFragment {
  public static interface OnTimePickListener {
    abstract void onTimePick(int secondsPastMidnight);
  }

  private final Calendar c = Calendar.getInstance();
  private OnTimePickListener listener = null;
  public void setListener(OnTimePickListener l) { listener = l; }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    if (getArguments() != null) {
      int secondsPastMidnight = getArguments().getInt("time", -1);
      if (secondsPastMidnight >= 0) {
        int hour = secondsPastMidnight / 3600;
        int minute = secondsPastMidnight / 60 - hour * 60;
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, minute);
      }
    }

    if (savedInstanceState != null) {
      c.set(Calendar.HOUR_OF_DAY, savedInstanceState.getInt("hour"));
      c.set(Calendar.MINUTE, savedInstanceState.getInt("minute"));
    }

    final View v =
      ((LayoutInflater)getContext().getSystemService(
          Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.time_picker, null);

    final AlertDialog d = new AlertDialog.Builder(getContext())
      // TODO edit alarm
      .setTitle("New Alarm")
      .setView(v)
      .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {}
        })
      .setPositiveButton("OK", new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            if (listener != null)
              listener.onTimePick(
                  c.get(Calendar.HOUR_OF_DAY) * 3600 +
                  c.get(Calendar.MINUTE) * 60);
          }
        })
      .create();
    d.getWindow().setSoftInputMode(
        WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

    final TextView t = (TextView)v.findViewById(R.id.picker_countdown);
    t.setText(until());

    final Button am_pm = (Button)v.findViewById(R.id.picker_am_pm);
    if (DateFormat.is24HourFormat(getContext())) {
      am_pm.setVisibility(View.GONE);
    } else {
      am_pm.setVisibility(View.VISIBLE);
      am_pm.setText(ampm());
      am_pm.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            if (c.get(Calendar.AM_PM) == Calendar.AM)
              c.set(Calendar.AM_PM, Calendar.PM);
            else
              c.set(Calendar.AM_PM, Calendar.AM);
            am_pm.setText(ampm());
            t.setText(until());
          }
        });
    }


    final EditText e = (EditText)v.findViewById(R.id.picker_time_entry);
    e.setText(time());
    e.addTextChangedListener(new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
        @Override
        public void onTextChanged(CharSequence s, int st, int b, int c) {}
        @Override
        public void afterTextChanged(Editable s) {
          String hhmm = s.toString().replaceAll(":", "");
          int hour;
          int minute;
          if (hhmm.length() < 3)
            return;
          try {
            hour = Integer.parseInt(hhmm.substring(0, hhmm.length() - 2));
            minute = Integer.parseInt(hhmm.substring(
                hhmm.length() - 2, hhmm.length()));
          } catch (NumberFormatException e) {
            return;
          }
          if (!DateFormat.is24HourFormat(getContext()) && hour == 12) hour = 0;

          int hour_field = DateFormat.is24HourFormat(getContext()) ?
            Calendar.HOUR_OF_DAY : Calendar.HOUR;
          c.set(hour_field, hour);
          if (minute < 60) {
            c.set(Calendar.MINUTE, minute);

            e.removeTextChangedListener(this);
            e.setText(time());
            e.setSelection(e.getText().length());
            e.addTextChangedListener(this);
            am_pm.setText(ampm());
            t.setText(until());
          }
        }
      });
    e.setOnEditorActionListener(new TextView.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView v, int action, KeyEvent e) {
          if (action == EditorInfo.IME_ACTION_DONE) {
            d.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
            return true;
          }
          return false;
        }
      });

    ((Button)v.findViewById(R.id.hour_plus_one)).setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            c.roll(Calendar.HOUR_OF_DAY, 1);
            am_pm.setText(ampm());
            e.setText(time());
            e.setSelection(e.getText().length());
            t.setText(until());
          }
        });
    ((Button)v.findViewById(R.id.hour_minus_one)).setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            c.roll(Calendar.HOUR_OF_DAY, -1);
            am_pm.setText(ampm());
            e.setText(time());
            e.setSelection(e.getText().length());
            t.setText(until());
          }
        });
    ((Button)v.findViewById(R.id.minute_plus_five)).setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            int minute = (c.get(Calendar.MINUTE) / 5 * 5 + 5) % 60;
            c.set(Calendar.MINUTE, minute);
            e.setText(time());
            e.setSelection(e.getText().length());
            t.setText(until());
          }
        });
    ((Button)v.findViewById(R.id.minute_minus_five)).setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            int minute = c.get(Calendar.MINUTE) / 5 * 5;
            if (minute == c.get(Calendar.MINUTE))
              minute -= 5;
            if (minute < 0)
              minute += 60;
            c.set(Calendar.MINUTE, minute);
            e.setText(time());
            e.setSelection(e.getText().length());
            t.setText(until());
          }
        });

    return d;
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putInt("hour", c.get(Calendar.HOUR_OF_DAY));
    outState.putInt("minute", c.get(Calendar.MINUTE));
  }

  private String time() {
    // NOTE: these use 'magic' three digits to trigger acceptance in the
    // edit box parsing above.  Don't zero pad 24 hour time.
    if (DateFormat.is24HourFormat(getContext()))
      return String.format(
          "%d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
    else {
      int hour = c.get(Calendar.HOUR);
      if (hour == 0) hour = 12;
      return String.format("%d:%02d", hour, c.get(Calendar.MINUTE));
    }
  }

  private String ampm() {
    if (c.get(Calendar.AM_PM) == Calendar.AM)
      return "AM";
    else
      return "PM";
  }

  private String until() {
    final Calendar now = Calendar.getInstance();
    final Calendar then = (Calendar)now.clone();
    then.set(Calendar.HOUR_OF_DAY, c.get(Calendar.HOUR_OF_DAY));
    then.set(Calendar.MINUTE, c.get(Calendar.MINUTE));
    if (then.before(now))
      then.add(Calendar.DATE, 1);

    long minutes = (then.getTimeInMillis() - now.getTimeInMillis()) / 1000 / 60;
    long hours = minutes / 60;
    minutes -= (hours * 60);

    if (hours > 0)
      return String.format(
          "%d %s %d %s",
          hours, (hours > 1) ? "hours" : "hour",
          minutes, (minutes > 1) ? "minutes" : "minute");
    else
      return String.format(
          "%d %s", minutes, (minutes > 1) ? "minutes" : "minute");
  }
}
