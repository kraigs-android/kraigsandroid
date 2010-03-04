package com.angrydoughnuts.android.alarmclock;

import java.util.Calendar;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class AlarmClockActivity extends Activity {
  private AlarmClockServiceBinder service;
  private DbAccessor db;
  private Cursor alarmListCursor;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);

    service = AlarmClockServiceBinder.newBinderAndStart(getApplicationContext());
    db = new DbAccessor(getApplicationContext());
    alarmListCursor = db.getAlarmList();
    startManagingCursor(alarmListCursor);

    Button setBtn = (Button) findViewById(R.id.set_alarm);
    setBtn.setOnClickListener(new View.OnClickListener() {
      public void onClick(View view) {
        Calendar trigger = Calendar.getInstance();
        trigger.add(Calendar.SECOND, 5);
        // TODO(cgallek): actually using seconds since midnight right now for testing.
        // Change back to minutes.
        int minutesAfterMidnight = trigger.get(Calendar.HOUR_OF_DAY) * 3600 + trigger.get(Calendar.MINUTE) * 60 + trigger.get(Calendar.SECOND);
        service.newAlarm(minutesAfterMidnight);
        alarmListCursor.requery();
      }
    });

    String[] from = new String[] { DbHelper.ALARMS_COL_ID, DbHelper.ALARMS_COL_TIME, DbHelper.ALARMS_COL_ENABLED };
    int[] to = new int[] { R.id.alarm_id, R.id.alarm_time, R.id.alarm_enabled };
    SimpleCursorAdapter adapter = new SimpleCursorAdapter(getApplicationContext(), R.layout.alarm_description, alarmListCursor, from, to);
    ListView alarmList = (ListView) findViewById(R.id.alarm_list);
    alarmList.setAdapter(adapter);
  }

  @Override
  protected void onResume() {
    super.onResume();
    service.bind();
  }

  @Override
  protected void onPause() {
    super.onPause();
    service.unbind();
    finish();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    db.closeConnections();
  }
}
