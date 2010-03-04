package com.angrydoughnuts.android.alarmclock;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

class AlarmViewAdapter extends ResourceCursorAdapter {
  private AlarmClockServiceBinder service;
  private int idIndex;
  private int timeIndex;
  private int enabledIndex;

  public AlarmViewAdapter(
      Context context, int layout, Cursor c, AlarmClockServiceBinder service) {
    super(context, layout, c);
    this.service = service;
    this.idIndex = c.getColumnIndex(DbHelper.ALARMS_COL__ID);
    this.timeIndex = c.getColumnIndex(DbHelper.ALARMS_COL_TIME);
    this.enabledIndex = c.getColumnIndex(DbHelper.ALARMS_COL_ENABLED);
  }

  @Override
  public void bindView(View view, Context context, Cursor cursor) {
    TextView timeView = (TextView) view.findViewById(R.id.alarm_time);
    CheckBox enabledView = (CheckBox) view.findViewById(R.id.alarm_enabled);

    AlarmTime time = new AlarmTime(cursor.getInt(timeIndex));
    String label = time.toString();
    if (AlarmClockService.debug(context)) {
      label += " [" + cursor.getLong(idIndex) + "]";
    }
    timeView.setText(label);
    enabledView.setChecked(cursor.getInt(enabledIndex) != 0);
    final long id = cursor.getLong(idIndex);
    enabledView.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        CheckBox check = (CheckBox) v;
        if (check.isChecked()) {
          service.scheduleAlarm(id);
        } else {
          service.dismissAlarm(id);
        }
      }
    });
  }    
}
