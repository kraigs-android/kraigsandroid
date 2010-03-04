package com.angrydoughnuts.android.alarmclock;

import java.util.ArrayList;

import android.content.ContentValues;
import android.database.Cursor;

public class AlarmInfo {
  public enum Day { SUN, MON, TUE, WED, THU, FRI, SAT; }

  private long alarmId;
  // TODO(cgallek): Move the AlarmTime class in here and replace this integer.
  private int time;
  private boolean enabled;
  private String name;
  private int dow;

  AlarmInfo(Cursor cursor) {
    alarmId = cursor.getLong(cursor.getColumnIndex(DbHelper.ALARMS_COL__ID));
    time = cursor.getInt(cursor.getColumnIndex(DbHelper.ALARMS_COL_TIME));
    enabled = cursor.getInt(cursor.getColumnIndex(DbHelper.ALARMS_COL_ENABLED)) == 1;
    name = cursor.getString(cursor.getColumnIndex(DbHelper.ALARMS_COL_NAME));
    dow = cursor.getInt(cursor.getColumnIndex(DbHelper.ALARMS_COL_DAY_OF_WEEK));
  }

  public ContentValues contentValues() {
    ContentValues values = new ContentValues();
    values.put(DbHelper.ALARMS_COL_TIME, time);
    values.put(DbHelper.ALARMS_COL_ENABLED, enabled);
    values.put(DbHelper.ALARMS_COL_NAME, name);
    values.put(DbHelper.ALARMS_COL_DAY_OF_WEEK, dow);
    return values;
  }

  static public String[] contentColumns() {
    return new String[] {
        DbHelper.ALARMS_COL__ID,
        DbHelper.ALARMS_COL_TIME,
        DbHelper.ALARMS_COL_ENABLED,
        DbHelper.ALARMS_COL_NAME,
        DbHelper.ALARMS_COL_DAY_OF_WEEK
    };
  }

  public long getAlarmId() {
    return alarmId;
  }

  public int getTime() {
    return time;
  }

  public void setTime(int time) {
    this.time = time;
  }

  public boolean enabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Day[] getDaysOfWeek() {
    ArrayList<Day> days = new ArrayList<Day>();
    for (int i = 0; i < Day.values().length; ++i) {
      if ((dow & Day.values()[i].ordinal()) > 0) {
        days.add(Day.values()[i]);
      }
    }
    return (Day[]) days.toArray();
  }

  public void setDow(Day[] daysOfWeek) {
    for (Day day: daysOfWeek) {
      dow |= day.ordinal();
    }
  }
}
