package com.angrydoughnuts.android.alarmclock;

import java.util.Calendar;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.EditText;

public final class TimePicker extends AlertDialog {
  private Calendar calendar;
  private PickerView hourPicker;
  private PickerView minutePicker;
  private PickerView secondPicker;

  public TimePicker(Context context) {
    super(context);

    calendar = Calendar.getInstance();

    setButton(AlertDialog.BUTTON_POSITIVE, context.getString(R.string.ok),
        new OnClickListener(){
          public void onClick(DialogInterface dialog, int which) {
          }
    });
    setButton(AlertDialog.BUTTON_NEGATIVE, context.getString(R.string.cancel),
        new OnClickListener(){
          public void onClick(DialogInterface dialog, int which) {
          }
    });

    setTitle("TESTING TITLE");
    //setIcon(android.R.drawable.ic_dialog_time);

    final LayoutInflater inflater =
      (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    final View body_view = inflater.inflate(R.layout.time_picker_dialog, null);
    setView(body_view);

    hourPicker = new PickerView(Calendar.HOUR);
    hourPicker.inflate(body_view, R.id.picker_hour, false, IncrementValue.ONE);
    minutePicker = new PickerView(Calendar.MINUTE);
    minutePicker.inflate(body_view, R.id.picker_minute, true, IncrementValue.THIRTY);
    secondPicker = new PickerView(Calendar.SECOND);
    secondPicker.inflate(body_view, R.id.picker_second, true, IncrementValue.THIRTY);
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

    public PickerView(int calendarField) {
      this.calendarField = calendarField;
    }

    public void inflate(View parentView, int resourceId, boolean showIncrement, IncrementValue defaultIncrement) {
      final ViewStub stub = (ViewStub) parentView.findViewById(resourceId);
      final View view = stub.inflate();
      text = (EditText) view.findViewById(R.id.time_value);
      increment = new Increment(defaultIncrement);
      incrementValueButton = (Button) view.findViewById(R.id.time_increment);

      incrementValueButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          increment.next();
          refresh();
        }
      });

      if (showIncrement) {
        incrementValueButton.setVisibility(View.VISIBLE);
      } else {
        incrementValueButton.setVisibility(View.GONE);
      }

      final Button plus = (Button) view.findViewById(R.id.time_plus);
      plus.setOnClickListener(new TimeIncrementListener());
      final Button minus = (Button) view.findViewById(R.id.time_minus);
      minus.setOnClickListener(new TimeDecrementListener());

      refresh();
    }

    private void refresh() {
      text.setText("" + calendar.get(calendarField));
      incrementValueButton.setText("+/- " + increment.value());
    }

    private final class Increment {
      private IncrementValue value;
      public Increment(IncrementValue value) {
        this.value = value;
      }
      public void next() {
        int new_value = (value.ordinal() + 1) % IncrementValue.values().length;
        value = IncrementValue.values()[new_value];
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
        refresh();
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
  }
}
