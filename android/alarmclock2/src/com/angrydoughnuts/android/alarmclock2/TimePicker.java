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

  private int hour;
  private int minute;
  private OnTimePickListener listener = null;
  public void setListener(OnTimePickListener l) { listener = l; }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    final Calendar now = Calendar.getInstance();
    hour = now.get(Calendar.HOUR_OF_DAY);
    minute = now.get(Calendar.MINUTE);

    if (getArguments() != null) {
      int secondsPastMidnight = getArguments().getInt("time", -1);
      if (secondsPastMidnight >= 0) {
        hour = secondsPastMidnight / 3600;
        minute = secondsPastMidnight / 60 - hour * 60;
      }
    }

    if (savedInstanceState != null) {
      hour = savedInstanceState.getInt("hour");
      minute = savedInstanceState.getInt("minute");
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
              listener.onTimePick(toSeconds());
          }
        })
      .create();
    d.getWindow().setSoftInputMode(
        WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

    final TextView t = (TextView)v.findViewById(R.id.picker_countdown);
    t.setText(TimeUtil.until(next()));

    final Button am_pm = (Button)v.findViewById(R.id.picker_am_pm);
    if (DateFormat.is24HourFormat(getContext())) {
      am_pm.setVisibility(View.GONE);
    } else {
      am_pm.setVisibility(View.VISIBLE);
      am_pm.setText(ampm());
      am_pm.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            if (hour < 12)
              hour += 12;
            else
              hour -= 12;
            am_pm.setText(ampm());
            t.setText(TimeUtil.until(next()));
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
          int new_hour;
          int new_minute;
          if (hhmm.length() < 3)
            return;
          try {
            new_hour = Integer.parseInt(hhmm.substring(0, hhmm.length() - 2));
            new_minute = Integer.parseInt(hhmm.substring(
                hhmm.length() - 2, hhmm.length()));
          } catch (NumberFormatException e) {
            return;
          }

          if (DateFormat.is24HourFormat(getContext())) {
            if (new_hour < 0 || new_hour > 23)
              return;
          } else {
            if (new_hour < 1 || new_hour > 12)
              return;
            if (new_hour == 12)
              new_hour = 0;
            if (hour > 11)
              new_hour += 12;
          }
          if (new_minute < 0 || new_minute > 59)
            return;

          hour = new_hour;
          minute = new_minute;

          e.removeTextChangedListener(this);
          e.setText(time());
          e.setSelection(e.getText().length());
          e.addTextChangedListener(this);
          am_pm.setText(ampm());
          t.setText(TimeUtil.until(next()));
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
            hour = (hour + 1) % 24;
            am_pm.setText(ampm());
            e.setText(time());
            e.setSelection(e.getText().length());
            t.setText(TimeUtil.until(next()));
          }
        });
    ((Button)v.findViewById(R.id.hour_minus_one)).setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            hour = (hour - 1) % 24;
            if (hour < 0)
              hour += 24;
            am_pm.setText(ampm());
            e.setText(time());
            e.setSelection(e.getText().length());
            t.setText(TimeUtil.until(next()));
          }
        });
    ((Button)v.findViewById(R.id.minute_plus_five)).setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            minute = (minute / 5 * 5 + 5) % 60;
            e.setText(time());
            e.setSelection(e.getText().length());
            t.setText(TimeUtil.until(next()));
          }
        });
    ((Button)v.findViewById(R.id.minute_minus_five)).setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            int new_minute = minute / 5 * 5;
            if (new_minute == minute)
              new_minute -= 5;
            if (new_minute < 0)
              new_minute += 60;
            minute = new_minute;
            e.setText(time());
            e.setSelection(e.getText().length());
            t.setText(TimeUtil.until(next()));
          }
        });

    return d;
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putInt("hour", hour);
    outState.putInt("minute", minute);
  }

  private int toSeconds() {
    return hour * 3600 + minute * 60;
  }

  private Calendar next() {
    return TimeUtil.nextOccurrence(toSeconds());
  }

  private String time() {
    return TimeUtil.format(getContext(), next());
  }

  private String ampm() {
    if (hour < 12)
      return "AM";
    else
      return "PM";
  }
}
