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
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TimePicker;
import android.widget.AdapterView.OnItemClickListener;

public class AlarmClockActivity extends Activity {
  private final int TIME_PICKER_DIALOG_ID = 1;
  private final int DEFAULT_SETTINGS_MENU = 2;

  private AlarmClockServiceBinder service;
  private DbAccessor db;
  private Cursor alarmListCursor;
  private Handler handler;
  private Runnable refreshCallback;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);

    service = AlarmClockServiceBinder.newBinder(getApplicationContext());
    db = new DbAccessor(getApplicationContext());
    alarmListCursor = db.getAlarmList();
    startManagingCursor(alarmListCursor);

    // TODO(cgallek): Replace this refresh loop with something that is triggered by
    // the time tick notification.  That will also be useful when a clock is added
    // to this activity and will align updates with the minute boundaries.
    handler = new Handler();
    refreshCallback = new Runnable() {
      @Override
      public void run() {
        if (alarmListCursor != null) {
          alarmListCursor.requery();
        }
        handler.postDelayed(refreshCallback, 60 *1000);
      }
    };

    Button testBtn = (Button) findViewById(R.id.test_alarm);
    if (!AlarmClockService.debug(getApplicationContext())) {
      testBtn.setVisibility(View.GONE);
    }
    testBtn.setOnClickListener(new View.OnClickListener() {
      public void onClick(View view) {
        Calendar testTime = Calendar.getInstance();
        testTime.add(Calendar.SECOND, 5);
        // TODO(cgallek): this is going to break if seconds are removed
        // from this service method.
        service.createAlarm(new AlarmTime(testTime));
        alarmListCursor.requery();
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
                Calendar c = Calendar.getInstance();
                c.set(Calendar.HOUR_OF_DAY, hourOfDay);
                c.set(Calendar.MINUTE, minute);
                c.set(Calendar.SECOND, 0);
                service.createAlarm(new AlarmTime(c));
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
    handler.postDelayed(refreshCallback, 60 * 1000);
  }

  @Override
  protected void onPause() {
    super.onPause();
    handler.removeCallbacks(refreshCallback);
    service.unbind();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    db.closeConnections();
  }
}
