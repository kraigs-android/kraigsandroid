package com.angrydoughnuts.android.alarmclock;

import java.util.Calendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.AdapterView.OnItemClickListener;

public class AlarmClockActivity extends Activity {
  private final int TIME_PICKER_DIALOG_ID = 1;
  private final int DEBUG_DIALOG_ID = 2;

  private final int DEFAULT_SETTINGS_MENU = 1;

  private AlarmClockServiceBinder service;
  private DbAccessor db;
  private Cursor alarmListCursor;
  private TextView clock;
  private Button testBtn;
  private Button pendingBtn;
  private Handler handler;
  private Runnable tickCallback;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);

    service = AlarmClockServiceBinder.newBinder(getApplicationContext());
    db = new DbAccessor(getApplicationContext());
    alarmListCursor = db.getAlarmList();
    startManagingCursor(alarmListCursor);
    handler = new Handler();

    clock = (TextView) findViewById(R.id.clock);
    clock.setOnLongClickListener(new OnLongClickListener() {
      @Override
      public boolean onLongClick(View v) {
        showDialog(DEBUG_DIALOG_ID);
        return true;
      }
    });

    testBtn = (Button) findViewById(R.id.test_alarm);
    testBtn.setOnClickListener(new OnClickListener() {
      public void onClick(View view) {
        Calendar testTime = Calendar.getInstance();
        testTime.add(Calendar.SECOND, 5);
        service.createAlarm(new AlarmTime(testTime.get(Calendar.HOUR_OF_DAY), testTime.get(Calendar.MINUTE), testTime.get(Calendar.SECOND)));
        redraw();
      }
    });

    pendingBtn = (Button) findViewById(R.id.pending_alarms);
    pendingBtn.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        Intent i = new Intent(getApplicationContext(), PendingAlarmsActivity.class);
        startActivity(i);
      }
    });

    Button addBtn = (Button) findViewById(R.id.add_alarm);
    addBtn.setOnClickListener(new View.OnClickListener() {
      public void onClick(View view) {
        showDialog(TIME_PICKER_DIALOG_ID);
      }
    });

    AlarmViewAdapter adapter = new AlarmViewAdapter(getApplicationContext(), R.layout.alarm_description, alarmListCursor, service);
    ListView alarmList = (ListView) findViewById(R.id.alarm_list);
    alarmList.setAdapter(adapter);
    alarmList.setOnItemClickListener(new OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> adapter, View view, int position,
          long id) {
        alarmListCursor.moveToPosition(position);
        long alarmId = alarmListCursor.getLong(
            alarmListCursor.getColumnIndex(DbHelper.ALARMS_COL__ID));
        Intent i = new Intent(getApplicationContext(), SettingsActivity.class);
        i.putExtra(SettingsActivity.EXTRAS_ALARM_ID, alarmId);
        startActivity(i);
      }
    });

    tickCallback = new Runnable() {
      @Override
      public void run() {
        // Redraw the screen.
        redraw();

        // Schedule the next update on the next interval boundary.
        int intervalMillis = 60 * 1000;  // every minute
        if (AlarmClockService.debug(getApplicationContext())) {
          intervalMillis = 5 * 1000;  // every 5 seconds
        }
        long now = System.currentTimeMillis();
        long next = intervalMillis - now % intervalMillis;
        handler.postDelayed(tickCallback, next);
      }
    };
  }

  @Override
  protected Dialog onCreateDialog(int id) {
    switch (id) {
      case TIME_PICKER_DIALOG_ID:
        Calendar now = Calendar.getInstance();
        // TODO(cgallek): replace this with default alarm time.
        int hour = now.get(Calendar.HOUR_OF_DAY);
        int minute = now.get(Calendar.MINUTE);
        boolean is24Hour = DateFormat.is24HourFormat(getApplicationContext());
        return new TimePickerDialog(this,
            new TimePickerDialog.OnTimeSetListener() {
              @Override
              public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                service.createAlarm(new AlarmTime(hourOfDay, minute, 0));
                redraw();
              }
            },
            hour, minute, is24Hour);
      case DEBUG_DIALOG_ID:
        ArrayAdapter<AlarmClockService.DebugMode> adapter = new ArrayAdapter<AlarmClockService.DebugMode>(getApplicationContext(), R.layout.empty_text_view, AlarmClockService.DebugMode.values());
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // TODO(cgallek): move this to strings.xml
        builder.setTitle("Debug Mode");
        builder.setSingleChoiceItems(adapter, 0, new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int item) {
            AlarmClockService.mode = AlarmClockService.DebugMode.values()[item];
            redraw();
            dismissDialog(DEBUG_DIALOG_ID);
          }
        });
        return builder.create();

      default:
        return super.onCreateDialog(id);
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuItem defaults = menu.add(0, DEFAULT_SETTINGS_MENU, 0, R.string.default_settings);
    defaults.setIcon(android.R.drawable.ic_lock_idle_alarm);
    // TODO(cgallek): Should this still call the parent??
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case DEFAULT_SETTINGS_MENU:
        Intent i = new Intent(getApplicationContext(), SettingsActivity.class);
        i.putExtra(SettingsActivity.EXTRAS_ALARM_ID, AlarmSettings.DEFAULT_SETTINGS_ID);
        startActivity(i);
    }
    // TODO(cgallek): Should this still call the parent??
    return super.onOptionsItemSelected(item);
  }

  @Override
  protected void onResume() {
    super.onResume();
    service.bind();
    handler.post(tickCallback);
  }

  @Override
  protected void onPause() {
    super.onPause();
    handler.removeCallbacks(tickCallback);
    service.unbind();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    db.closeConnections();
  }

  private void redraw() {
    // Show/hide debug buttons.
    if (AlarmClockService.debug(getApplicationContext())) {
      testBtn.setVisibility(View.VISIBLE);
      pendingBtn.setVisibility(View.VISIBLE);
    } else {
      testBtn.setVisibility(View.GONE);
      pendingBtn.setVisibility(View.GONE);
    }

    // Update clock
    Calendar c = Calendar.getInstance();
    AlarmTime time = new AlarmTime(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), c.get(Calendar.SECOND));
    clock.setText(time.localizedString(getApplicationContext()));

    // Update the alarm list.
    alarmListCursor.requery();
  }
}
