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

import java.util.Calendar;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Handler;
import android.text.format.DateFormat;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewStub;
import android.view.View.OnFocusChangeListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

/**
 * This class is a slight improvement over the android time picker dialog.
 * It allows the user to select hour, minute, and second (the android picker
 * does not support seconds).  It also has a configurable increment feature
 * (30, 5, and 1).
 */
public final class TimePickerDialog extends AlertDialog {
  public interface OnTimeSetListener {
    public void onTimeSet(int hourOfDay, int minute, int second);
  }

  private static final String PICKER_PREFS = "TimePickerPreferences";
  private static final String INCREMENT_PREF = "increment";

  private OnTimeSetListener listener;
  private SharedPreferences prefs;
  private Calendar calendar;
  private TextView timeText;
  private Button amPmButton;
  private PickerView hourPicker;
  private PickerView minutePicker;
  private PickerView secondPicker;

  /**
   * Construct a time picker with the supplied hour minute and second.
   * @param context
   * @param title Dialog title.
   * @param hourOfDay 0 to 23.
   * @param minute  0 to 60.
   * @param second 0 to 60.
   * @param showSeconds Show/hide the seconds field.
   * @param setListener Callback for when the user selects 'OK'.
   */
  public TimePickerDialog(Context context, String title,
      int hourOfDay, int minute, int second, final boolean showSeconds,
      OnTimeSetListener setListener) {
    this(context, title, showSeconds, setListener);
    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
    hourPicker.pickerRefresh();
    calendar.set(Calendar.MINUTE, minute);
    minutePicker.pickerRefresh();
    calendar.set(Calendar.SECOND, second);
    if (showSeconds) {
      secondPicker.pickerRefresh();
    }
    dialogRefresh();
  }

  /**
   * Construct a time picker with 'now' as the starting time.
   * @param context
   * @param title Dialog title.
   * @param showSeconds Show/hid the seconds field.
   * @param setListener Callback for when the user selects 'OK'.
   */
  public TimePickerDialog(Context context, String title, final boolean showSeconds,
      OnTimeSetListener setListener) {
    super(context);
    listener = setListener;
    prefs = context.getSharedPreferences(PICKER_PREFS, Context.MODE_PRIVATE);
    calendar = Calendar.getInstance();

    // The default increment amount is stored in a shared preference.  Look
    // it up.
    final int incPref = prefs.getInt(INCREMENT_PREF, IncrementValue.FIVE.ordinal());
    final IncrementValue defaultIncrement = IncrementValue.values()[incPref];

    // OK button setup.
    setButton(AlertDialog.BUTTON_POSITIVE, context.getString(R.string.ok),
        new OnClickListener(){
          public void onClick(DialogInterface dialog, int which) {
            if (listener == null) {
              return;
            }
            int seconds = showSeconds ? calendar.get(Calendar.SECOND) : 0;
            listener.onTimeSet(
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                seconds);
          }
    });

    // Cancel button setup.
    setButton(AlertDialog.BUTTON_NEGATIVE, context.getString(R.string.cancel),
        new OnClickListener(){
          public void onClick(DialogInterface dialog, int which) {
            cancel();
          }
    });

    // Set title and icon.
    if (title.length() != 0) {
      setTitle(title);
      setIcon(R.drawable.ic_dialog_time);
    }

    // Set the view for the body section of the AlertDialog.
    final LayoutInflater inflater =
      (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    final View body_view = inflater.inflate(R.layout.time_picker_dialog, null);
    setView(body_view);

    // Setup each of the components of the body section.
    timeText = (TextView) body_view.findViewById(R.id.picker_text);

    amPmButton = (Button) body_view.findViewById(R.id.picker_am_pm);
    amPmButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (calendar.get(Calendar.AM_PM) == Calendar.AM) {
          calendar.set(Calendar.AM_PM, Calendar.PM);
        } else {
          calendar.set(Calendar.AM_PM, Calendar.AM);
        }
        dialogRefresh();
      }
    });

    // Setup the three time fields.
    if (DateFormat.is24HourFormat(getContext())) {
      body_view.findViewById(R.id.picker_am_pm_layout).setVisibility(View.GONE);
      hourPicker = new PickerView(Calendar.HOUR_OF_DAY, "%02d");
    } else {
      body_view.findViewById(R.id.picker_am_pm_layout).setVisibility(View.VISIBLE);
      hourPicker = new PickerView(Calendar.HOUR, "%d");
    }
    hourPicker.inflate(body_view, R.id.picker_hour, false, IncrementValue.ONE);
    minutePicker = new PickerView(Calendar.MINUTE, "%02d");
    minutePicker.inflate(body_view, R.id.picker_minute, true, defaultIncrement);
    if (showSeconds) {
      secondPicker = new PickerView(Calendar.SECOND, "%02d");
      secondPicker.inflate(body_view, R.id.picker_second, true, defaultIncrement);
    }

    dialogRefresh();
  }

  private void dialogRefresh() {
    AlarmTime time = new AlarmTime(
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        calendar.get(Calendar.SECOND));
    timeText.setText(time.timeUntilString(getContext()));
    if (calendar.get(Calendar.AM_PM) == Calendar.AM) {
      amPmButton.setText(getContext().getString(R.string.am));
    } else {
      amPmButton.setText(getContext().getString(R.string.pm));
    }
  }

  /**
   * Enum that represents the states of the increment picker button.
   */
  private enum IncrementValue {
    FIVE(5), ONE(1);
    private int value;
    IncrementValue(int value) {
      this.value = value;
    }
    public int value() {
      return value;
    }
  }

  /**
   * Helper class that wraps up the view elements of each number picker
   * (plus/minus button, text field, increment picker).
   */
  private final class PickerView {
    private int calendarField;
    private String formatString;
    private EditText text = null;
    private Increment increment = null;
    private Button incrementValueButton = null;
    private Button plus = null;
    private Button minus = null;

    /**
     * Construct a numeric picker for the supplied calendar field and formats
     * it according to the supplied format string.
     * @param calendarField
     * @param formatString
     */
    public PickerView(int calendarField, String formatString) {
      this.calendarField = calendarField;
      this.formatString = formatString;
    }

    /**
     * Inflates the ViewStub for this numeric picker.
     * @param parentView
     * @param resourceId
     * @param showIncrement
     * @param defaultIncrement
     */
    public void inflate(View parentView, int resourceId, boolean showIncrement, IncrementValue defaultIncrement) {
      final ViewStub stub = (ViewStub) parentView.findViewById(resourceId);
      final View view = stub.inflate();
      text = (EditText) view.findViewById(R.id.time_value);
      text.setOnFocusChangeListener(new TextChangeListener());
      text.setOnEditorActionListener(new TextChangeListener());

      increment = new Increment(defaultIncrement);
      incrementValueButton = (Button) view.findViewById(R.id.time_increment);

      incrementValueButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          increment.cycleToNext();
          Editor editor = prefs.edit();
          editor.putInt(INCREMENT_PREF, increment.value.ordinal());
          editor.commit();
          pickerRefresh();
        }
      });

      if (showIncrement) {
        incrementValueButton.setVisibility(View.VISIBLE);
      } else {
        incrementValueButton.setVisibility(View.GONE);
      }

      plus = (Button) view.findViewById(R.id.time_plus);
      TimeIncrementListener incrementListener = new TimeIncrementListener();
      plus.setOnClickListener(incrementListener);
      plus.setOnTouchListener(incrementListener);
      plus.setOnLongClickListener(incrementListener);
      minus = (Button) view.findViewById(R.id.time_minus);
      TimeDecrementListener decrementListener= new TimeDecrementListener();
      minus.setOnClickListener(decrementListener);
      minus.setOnTouchListener(decrementListener);
      minus.setOnLongClickListener(decrementListener);

      pickerRefresh();
    }

    public void pickerRefresh() {
      int fieldValue = calendar.get(calendarField);
      if (calendarField == Calendar.HOUR && fieldValue == 0) {
        fieldValue = 12;
      }
      text.setText(String.format(formatString, fieldValue));
      incrementValueButton.setText("+/- " + increment.nextValue().value());
      plus.setText("+" + increment.value());
      minus.setText("-" + increment.value());
      dialogRefresh();
    }

    private final class Increment {
      private IncrementValue value;
      public Increment(IncrementValue value) {
        this.value = value;
      }
      public IncrementValue nextValue() {
        int nextIndex = (value.ordinal() + 1) % IncrementValue.values().length;
        return IncrementValue.values()[nextIndex];
      }
      public void cycleToNext() {
        value = nextValue();
      }
      public int value() {
        return value.value();
      }
    }

    /**
     * Listener that figures out what the next value should be when a numeric
     * picker plus/minus button is clicked.  It will round up/down to the next
     * interval increment then increment by the increment amount on subsequent
     * clicks.
     */
    private abstract class TimeAdjustListener implements
      View.OnClickListener, View.OnTouchListener, View.OnLongClickListener {
      protected abstract int sign();

      private void adjust() {
        int currentValue = calendar.get(calendarField);
        int remainder = currentValue % increment.value();
        if (remainder == 0) {
          calendar.roll(calendarField, sign() * increment.value());
        } else {
          int difference;
          if (sign() > 0) {
            difference = increment.value() - remainder;
          } else {
            difference = -1 * remainder;
          }
          calendar.roll(calendarField, difference);
        }
        pickerRefresh();
      }

      private Handler handler = new Handler();
      private Runnable delayedAdjust = new Runnable() {
        @Override
        public void run() {
          adjust();
          handler.postDelayed(delayedAdjust, 150);
        }
      };

      @Override
      public void onClick(View v) {
        adjust();
      }

      @Override
      public boolean onLongClick(View v) {
        delayedAdjust.run();
        return false;
      }

      @Override
      public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
          handler.removeCallbacks(delayedAdjust);
        }
        return false;
      }
    }

    private final class TimeIncrementListener extends TimeAdjustListener {
      @Override
      protected int sign() {
        return 1;
      }
    }
    private final class TimeDecrementListener extends TimeAdjustListener {
      @Override
      protected int sign() { return -1; }
    }

    /**
     * Listener to handle direct user input into the time picker text fields.
     * Updates after the editor confirmation button is picked or when the
     * text field loses focus.
     */
    private final class TextChangeListener implements OnFocusChangeListener, OnEditorActionListener {
      private void handleChange() {
        try {
          int newValue = Integer.parseInt(text.getText().toString());
          if (calendarField == Calendar.HOUR &&
              newValue == 12 &&
              calendar.get(Calendar.AM_PM) == Calendar.AM) {
            calendar.set(Calendar.HOUR_OF_DAY, 0);
          } else if (calendarField == Calendar.HOUR &&
              newValue == 12 &&
              calendar.get(Calendar.AM_PM) == Calendar.PM) {
            calendar.set(Calendar.HOUR_OF_DAY, 12);
          } else {
            calendar.set(calendarField, newValue);
          }
        } catch (NumberFormatException e) {}
        pickerRefresh();
      }
      @Override
      public void onFocusChange(View v, boolean hasFocus) {
        handleChange();
      }
      @Override
      public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        handleChange();
        return false;
      }
    }
  }
}
