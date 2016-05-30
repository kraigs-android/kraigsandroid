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
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

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
            if (listener != null)
              listener.onTimePick(
                  c.get(Calendar.HOUR_OF_DAY) * 3600 +
                  c.get(Calendar.MINUTE) * 60);
          }
        })
      .create();

    final EditText e = (EditText)v.findViewById(R.id.time_entry);
    e.setText(time());
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
    e.setOnEditorActionListener(new TextView.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView v, int action, KeyEvent e) {
          d.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
          return false;
        }
      });

    ((Button)v.findViewById(R.id.hour_plus_one)).setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            c.roll(Calendar.HOUR_OF_DAY, 1);
            e.setText(time());
          }
        });
    ((Button)v.findViewById(R.id.hour_minus_one)).setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            c.roll(Calendar.HOUR_OF_DAY, -1);
            e.setText(time());
          }
        });
    ((Button)v.findViewById(R.id.minute_plus_five)).setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            int minute = (c.get(Calendar.MINUTE) / 5 * 5 + 5) % 60;
            c.set(Calendar.MINUTE, minute);
            e.setText(time());
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
    // TODO: HOUR/HOUR_OF_DAY for AM/PM??
    return String.format(
        "%02d:%02d",
        c.get(Calendar.HOUR_OF_DAY),
        c.get(Calendar.MINUTE));
  }
}
