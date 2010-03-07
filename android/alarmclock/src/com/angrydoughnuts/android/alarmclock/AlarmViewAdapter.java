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

import java.util.LinkedList;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

/**
 * This adapter is used to query the alarm database and translate each alarm
 * into a view which is displayed in a ListView.
 */
public final class AlarmViewAdapter extends ArrayAdapter<AlarmInfo> {
  private AlarmClockServiceBinder service;
  private LayoutInflater inflater;
  private Cursor cursor;

  public AlarmViewAdapter(Activity activity, DbAccessor db, AlarmClockServiceBinder service) {
    super(activity, 0, new LinkedList<AlarmInfo>());
    this.service = service;
    this.inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    this.cursor = db.readAlarmInfo();
    activity.startManagingCursor(cursor);
    loadData();
  }

  private void loadData() {
    while (cursor.moveToNext()) {
      add(new AlarmInfo(cursor));
    }
  }

  public void requery() {
    clear();
    cursor.requery();
    loadData();
    notifyDataSetChanged();
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    View view = inflater.inflate(R.layout.alarm_list_item, null);
    TextView timeView = (TextView) view.findViewById(R.id.alarm_time);
    TextView nextView = (TextView) view.findViewById(R.id.next_alarm);
    TextView labelView = (TextView) view.findViewById(R.id.alarm_label);
    TextView repeatView = (TextView) view.findViewById(R.id.alarm_repeat);
    CheckBox enabledView = (CheckBox) view.findViewById(R.id.alarm_enabled);

    final AlarmInfo info = getItem(position);

    AlarmTime time = null;
    // See if there is an instance of this alarm scheduled.
    if (service.clock() != null) {
      try {
        time = service.clock().pendingAlarm(info.getAlarmId());
      } catch (RemoteException e) {}
    }
    // If we couldn't find a pending alarm, display the configured time.
    if (time == null) {
      time = info.getTime();
    }
    String timeStr = time.localizedString(getContext());
    String alarmId = "";
    if (AppSettings.isDebugMode(getContext())) {
      alarmId = " [" + info.getAlarmId() + "]";
    }
    timeView.setText(timeStr + alarmId);
    enabledView.setChecked(info.enabled());

    nextView.setText(time.timeUntilString(getContext()));
    labelView.setText(info.getName());
    if (!info.getTime().getDaysOfWeek().equals(Week.NO_REPEATS)) {
      repeatView.setText(info.getTime().getDaysOfWeek().toString(getContext()));
    }
    enabledView.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        CheckBox check = (CheckBox) v;
        if (check.isChecked()) {
          service.scheduleAlarm(info.getAlarmId());
          requery();
        } else {
          service.unscheduleAlarm(info.getAlarmId());
          requery();
        }
      }
    });
    return view;
  }
}
