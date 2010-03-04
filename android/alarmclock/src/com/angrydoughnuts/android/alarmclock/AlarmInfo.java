package com.angrydoughnuts.android.alarmclock;

import java.util.Calendar;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

public class AlarmInfo {
  public enum Day {
    SUN(R.string.dow_sun),
    MON(R.string.dow_mon),
    TUE(R.string.dow_tue),
    WED(R.string.dow_wed),
    THU(R.string.dow_thu),
    FRI(R.string.dow_fri),
    SAT(R.string.dow_sat);
    private int stringId;
    Day (int stringId) {
      this.stringId = stringId;
    }
    public int stringId() {
      return stringId;
    }
  }

  public class Week {
    private boolean[] bitmask;

    public CharSequence[] names(Context context) {
      CharSequence[] nameList = new CharSequence[Day.values().length];
      for (Day day : Day.values()) {
        nameList[day.ordinal()] = context.getString(day.stringId());
      }
      return nameList;
    }

    public Week() {
      bitmask = new boolean[Day.values().length];
    }
    public Week(int dow) {
      bitmask = new boolean[Day.values().length];
      for (Day day : Day.values()) {
        if ((dow & 1 << day.ordinal()) > 0) {
          addDay(day);
        }
      }
    }
    @Override
    public boolean equals(Object o) {
      if (!(o instanceof Week)) {
        return false;
      }
      Week rhs = (Week) o;
      if (bitmask.length != rhs.bitmask.length) {
        return false;
      }
      for (Day day : Day.values()) {
        if (bitmask[day.ordinal()] != rhs.bitmask[day.ordinal()]) {
          return false;
        }
      }
      return true;
    }
    public boolean[] bitmask() {
      return bitmask;
    }
    public void addDay(Day day) {
      bitmask[day.ordinal()] = true;
    }
    public void removeDay(Day day) {
      bitmask[day.ordinal()] = false;
    }
    public boolean hasDay(Day day) {
      return bitmask[day.ordinal()];
    }
    public int toInetger() {
      int dow = 0;
      for (Day day: Day.values()) {
        if (bitmask[day.ordinal()]) {
          dow |= 1 << day.ordinal();
        }
      }
      return dow;
    }
    public String toString(Context context) {
      final Week everyday = new Week(127);
      final Week weekdays = new Week(62);
      final Week weekends = new Week(65);

      if (this.equals(everyday)) {
        return context.getString(R.string.everyday);
      }
      if (this.equals(weekdays)) {
        return context.getString(R.string.weekdays);
      }
      if (this.equals(weekends)) {
        return context.getString(R.string.weekends);
      }
      String list = "";
      for (Day day : Day.values()) {
        if (!bitmask[day.ordinal()]) {
          continue;
        }
        switch (day) {
          case SUN:
            list += " " + context.getString(R.string.dow_sun_short);
            break;
          case MON:
            list += " " + context.getString(R.string.dow_mon_short);
            break;
          case TUE:
            list += " " + context.getString(R.string.dow_tue_short);
            break;
          case WED:
            list += " " + context.getString(R.string.dow_wed_short);
            break;
          case THU:
            list += " " + context.getString(R.string.dow_thu_short);
            break;
          case FRI:
            list += " " + context.getString(R.string.dow_fri_short);
            break;
          case SAT:
            list += " " + context.getString(R.string.dow_sat_short);
            break;
        }
      }
      return list;
    }
  }

  private boolean dirty;
  private long alarmId;
  private AlarmTime time;
  private boolean enabled;
  private String name;
  private Week daysOfWeek;

  public AlarmInfo(Cursor cursor) {
    dirty = false;
    alarmId = cursor.getLong(cursor.getColumnIndex(DbHelper.ALARMS_COL__ID));
    time = IntegerToAlarmTime(cursor.getInt(cursor.getColumnIndex(DbHelper.ALARMS_COL_TIME)));
    enabled = cursor.getInt(cursor.getColumnIndex(DbHelper.ALARMS_COL_ENABLED)) == 1;
    name = cursor.getString(cursor.getColumnIndex(DbHelper.ALARMS_COL_NAME));
    daysOfWeek = new Week(cursor.getInt(cursor.getColumnIndex(DbHelper.ALARMS_COL_DAY_OF_WEEK)));
  }

  public AlarmInfo() {
    dirty = false;
    alarmId = -69;  // initially invalid.
    time = new AlarmTime(0, 0, 0);
    enabled = false;
    name = "";
    daysOfWeek = new Week();
  }

  public ContentValues contentValues() {
    ContentValues values = new ContentValues();
    values.put(DbHelper.ALARMS_COL_TIME, AlarmTimeToInteger(time));
    values.put(DbHelper.ALARMS_COL_ENABLED, enabled);
    values.put(DbHelper.ALARMS_COL_NAME, name);
    values.put(DbHelper.ALARMS_COL_DAY_OF_WEEK, daysOfWeek.toInetger());
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

  public boolean dirty() {
    return dirty;
  }

  public long getAlarmId() {
    return alarmId;
  }

  public AlarmTime getTime() {
    return time;
  }

  public void setTime(AlarmTime time) {
    // TODO(cgallek) is this equals() implemented?
    if (!this.time.equals(time)) {
      dirty = true;
    }
    this.time = time;
  }

  public boolean enabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    if (this.enabled != enabled) {
      dirty = true;
    }
    this.enabled = enabled;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    if (!this.name.equals(name)) {
      dirty = true;
    }
    this.name = name;
  }

  public Week getDaysOfWeek() {
    return daysOfWeek;
  }

  public void setDow(Week week) {
    if (!this.daysOfWeek.equals(week)) {
      dirty = true;
    }
    this.daysOfWeek = week;
  }

  public String getDaysOfWeekString(Context context) {
    return daysOfWeek.toString(context);
  }

  private static int AlarmTimeToInteger(AlarmTime time) {
    Calendar c = time.calendar();
    int hourOfDay = c.get(Calendar.HOUR_OF_DAY);
    int minute = c.get(Calendar.MINUTE);
    int second = c.get(Calendar.SECOND);
    return hourOfDay * 3600 + minute * 60 + second;
  }

  private static AlarmTime IntegerToAlarmTime(int secondsAfterMidnight) {
    int hours = secondsAfterMidnight % 3600;
    int minutes = (secondsAfterMidnight - (hours * 3600)) % 60;
    int seconds = (secondsAfterMidnight- (hours * 3600 + minutes * 60));
    return new AlarmTime(hours, minutes, seconds);
  }
}
