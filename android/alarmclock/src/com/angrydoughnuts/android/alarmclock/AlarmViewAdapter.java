package com.angrydoughnuts.android.alarmclock;

import java.util.LinkedList;

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

class AlarmViewAdapter extends ArrayAdapter<AlarmInfo> {
  private AlarmClockServiceBinder service;
  private LayoutInflater inflater;
  private DbAccessor db;
  private Cursor cursor;

  public AlarmViewAdapter(Context context, LayoutInflater inflater, AlarmClockServiceBinder service) {
    super(context, 0, new LinkedList<AlarmInfo>());
    this.service = service;
    this.inflater = inflater;
    this.db = new DbAccessor(context);
    this.cursor = db.readAlarmInfo();
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
    View view = inflater.inflate(R.layout.alarm_description, null);
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
      time = new AlarmTime(info.getTime());
    }
    String timeStr = time.localizedString(getContext());
    String alarmId = "";
    if (AlarmClockService.debug(getContext())) {
      alarmId = " [" + info.getAlarmId() + "]";
    }
    timeView.setText(timeStr + alarmId);
    enabledView.setChecked(info.enabled());

    nextView.setText(time.timeUntilString());
    labelView.setText(info.getName());
    repeatView.setText(info.getDaysOfWeek().toString(getContext()));
    enabledView.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        CheckBox check = (CheckBox) v;
        if (check.isChecked()) {
          service.scheduleAlarm(info.getAlarmId());
          requery();
        } else {
          service.dismissAlarm(info.getAlarmId());
          requery();
        }
      }
    });
    return view;
  }

  protected void finalize() throws Throwable {
    cursor.close();
    db.closeConnections();
    super.finalize();
  }
}
