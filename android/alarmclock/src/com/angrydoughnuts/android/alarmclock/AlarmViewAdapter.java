package com.angrydoughnuts.android.alarmclock;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

class AlarmViewAdapter extends ResourceCursorAdapter {
  private int timeIndex;
  private int enabledIndex;

  public AlarmViewAdapter(Context context, int layout, Cursor c) {
    super(context, layout, c);
    timeIndex = c.getColumnIndex(DbHelper.ALARMS_COL_TIME);
    enabledIndex = c.getColumnIndex(DbHelper.ALARMS_COL_ENABLED);
  }

  @Override
  public void bindView(View view, Context context, Cursor cursor) {
    TextView timeView = (TextView) view.findViewById(R.id.alarm_time);
    CheckBox enabledView = (CheckBox) view.findViewById(R.id.alarm_enabled);

    timeView.setText(cursor.getString(timeIndex));
    enabledView.setChecked(cursor.getInt(enabledIndex) != 0);
  }    
}
