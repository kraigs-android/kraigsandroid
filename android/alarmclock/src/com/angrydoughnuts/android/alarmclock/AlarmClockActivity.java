package com.angrydoughnuts.android.alarmclock;

import java.util.Calendar;

import android.app.Activity;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.AdapterView.OnItemClickListener;

public class AlarmClockActivity extends Activity {
  private final int TIME_PICKER_DIALOG_ID = 1;
  private final int DEFAULT_SETTINGS_MENU = 2;

  private AlarmClockServiceBinder service;
  private DbAccessor db;
  private Cursor alarmListCursor;
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
    final TextView clock = (TextView) findViewById(R.id.clock);
    tickCallback = new Runnable() {
      @Override
      public void run() {
        // Current time.
        Calendar c = Calendar.getInstance();
        AlarmTime time = new AlarmTime(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), c.get(Calendar.SECOND));

        // Refresh operations
        clock.setText(time.localizedString(getApplicationContext()));
        alarmListCursor.requery();

        // Schedule the next update on the next interval boundary.
        int intervalMillis = 60 * 1000;  // every minute
        if (AlarmClockService.debug(getApplicationContext())) {
          intervalMillis = 5 * 1000;  // every 5 seconds
        }
        long now = c.getTimeInMillis();
        long next = intervalMillis - now % intervalMillis;
        handler.postDelayed(tickCallback, next);
      }
    };

    Button testBtn = (Button) findViewById(R.id.test_alarm);
    Button pendingBtn = (Button) findViewById(R.id.pending_alarms);
    if (AlarmClockService.debug(getApplicationContext())) {
      testBtn.setVisibility(View.VISIBLE);
      pendingBtn.setVisibility(View.VISIBLE);
    }
    testBtn.setOnClickListener(new OnClickListener() {
      public void onClick(View view) {
        Calendar testTime = Calendar.getInstance();
        testTime.add(Calendar.SECOND, 5);
        service.createAlarm(new AlarmTime(testTime.get(Calendar.HOUR_OF_DAY), testTime.get(Calendar.MINUTE), testTime.get(Calendar.SECOND)));
        alarmListCursor.requery();
      }
    });
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
                alarmListCursor.requery();
              }
            },
            hour, minute, is24Hour);
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
    alarmListCursor.requery();
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
}
