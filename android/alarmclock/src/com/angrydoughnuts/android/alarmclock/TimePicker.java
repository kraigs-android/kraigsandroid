package com.angrydoughnuts.android.alarmclock;

import java.util.Calendar;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.text.format.DateFormat;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewStub;
import android.view.View.OnFocusChangeListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public final class TimePicker extends AlertDialog {
  public interface OnTimeSetListener {
    public void onTimeSet(int hourOfDay, int minute, int second);
  }

  private static String PICKER_PREFS = "TimePickerPreferences";
  private static String INCREMENT_PREF = "increment";

  private OnTimeSetListener listener;
  private SharedPreferences prefs;
  private Calendar calendar;
  private TextView timeText;
  private Button amPmButton;
  private PickerView hourPicker;
  private PickerView minutePicker;
  private PickerView secondPicker;

  public TimePicker(Context context, String title,
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
  }

  public TimePicker(Context context, String title, final boolean showSeconds,
      OnTimeSetListener setListener) {
    super(context);
    listener = setListener;
    prefs = context.getSharedPreferences(PICKER_PREFS, Context.MODE_PRIVATE);
    calendar = Calendar.getInstance();

    final int incPref = prefs.getInt(INCREMENT_PREF, IncrementValue.FIVE.ordinal());
    final IncrementValue defaultIncrement = IncrementValue.values()[incPref];

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
    setButton(AlertDialog.BUTTON_NEGATIVE, context.getString(R.string.cancel),
        new OnClickListener(){
          public void onClick(DialogInterface dialog, int which) {
          }
    });

    if (title.length() != 0) {
      setTitle(title);
      //setIcon(android.R.drawable.ic_dialog_time);
    }

    final LayoutInflater inflater =
      (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    final View body_view = inflater.inflate(R.layout.time_picker_dialog, null);
    setView(body_view);

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

    if (DateFormat.is24HourFormat(getContext())) {
      amPmButton.setVisibility(View.GONE);
      hourPicker = new PickerView(Calendar.HOUR_OF_DAY);
    } else {
      amPmButton.setVisibility(View.VISIBLE);
      hourPicker = new PickerView(Calendar.HOUR);
    }
    hourPicker.inflate(body_view, R.id.picker_hour, false, IncrementValue.ONE);
    minutePicker = new PickerView(Calendar.MINUTE);
    minutePicker.inflate(body_view, R.id.picker_minute, true, defaultIncrement);
    if (showSeconds) {
      secondPicker = new PickerView(Calendar.SECOND);
      secondPicker.inflate(body_view, R.id.picker_second, true, defaultIncrement);
    }
  }

  void dialogRefresh() {
    AlarmTime time = new AlarmTime(calendar.get(
        Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        calendar.get(Calendar.SECOND));
    // TODO(cgallek): should this have a timer as well to make it refresh when
    // the minutes tick??
    timeText.setText(time.timeUntilString(getContext()));
    if (calendar.get(Calendar.AM_PM) == Calendar.AM) {
      amPmButton.setText(getContext().getString(R.string.am));
    } else {
      amPmButton.setText(getContext().getString(R.string.pm));
    }
  }

  private enum IncrementValue {
    THIRTY(30), FIVE(5), ONE(1);
    private int value;
    IncrementValue(int value) {
      this.value = value;
    }
    public int value() {
      return value;
    }
  }

  private final class PickerView {
    private int calendarField;
    private EditText text = null;
    private Increment increment = null;
    private Button incrementValueButton = null;
    private Button plus = null;
    private Button minus = null;

    public PickerView(int calendarField) {
      this.calendarField = calendarField;
    }

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
      plus.setOnClickListener(new TimeIncrementListener());
      minus = (Button) view.findViewById(R.id.time_minus);
      minus.setOnClickListener(new TimeDecrementListener());

      pickerRefresh();
    }

    private void pickerRefresh() {
      int fieldValue = calendar.get(calendarField);
      if (calendarField == Calendar.HOUR && fieldValue == 0) {
        fieldValue = 12;
      }
      text.setText("" + fieldValue);
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

    private abstract class TimeAdjustListener implements View.OnClickListener {
      protected abstract int sign();
      @Override
      public void onClick(View v) {
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

    private final class TextChangeListener implements OnFocusChangeListener, OnEditorActionListener {
      private void handleChange() {
        try {
          int newValue = Integer.parseInt(text.getText().toString());
          calendar.set(calendarField, newValue);
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
