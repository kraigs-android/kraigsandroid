package com.angrydoughnuts.android.alarmclock;

import java.util.LinkedList;
import android.content.Context;
import android.database.Cursor;
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
  }

  public void requery() {
    clear();
    cursor.requery();
    while (cursor.moveToNext()) {
      add(new AlarmInfo(cursor));
    }
    notifyDataSetChanged();
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    View view = inflater.inflate(R.layout.alarm_description, null);
    TextView timeView = (TextView) view.findViewById(R.id.alarm_time);
    TextView nextView = (TextView) view.findViewById(R.id.next_alarm);
    CheckBox enabledView = (CheckBox) view.findViewById(R.id.alarm_enabled);

    final AlarmInfo info = getItem(position);
    AlarmTime time = new AlarmTime(info.getTime());
    String timeStr = time.localizedString(getContext());
    String alarmId = "";
    if (AlarmClockService.debug(getContext())) {
      alarmId = " [" + info.getAlarmId() + "]";
    }
    timeView.setText(timeStr + alarmId);
    enabledView.setChecked(info.enabled());
    // TODO(cgallek): This doesn't account for snoozed alarms :-\
    // Figure out how to get this information back from the service based
    // on actual scheduled times.
    nextView.setText(time.timeUntilString());
    enabledView.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        CheckBox check = (CheckBox) v;
        if (check.isChecked()) {
          service.scheduleAlarm(info.getAlarmId());
        } else {
          service.dismissAlarm(info.getAlarmId());
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
