package com.angrydoughnuts.android.alarmclock;

import java.util.Calendar;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

public class Week implements Parcelable {
  public static final Week NO_REPEATS = new Week(new boolean[] {false, false, false, false, false, false, false});
  public static final Week EVERYDAY = new Week(new boolean[] {true, true, true, true, true, true, true});
  public static final Week WEEKDAYS = new Week(new boolean[] {false, true, true, true, true, true, false});
  public static final Week WEEKENDS = new Week(new boolean[] {true, false, false, false, false, false, true});

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

  private boolean[] bitmask;

  public Week(Parcel source) {
    bitmask = source.createBooleanArray();
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeBooleanArray(bitmask);
  }

  public Week() {
    bitmask = new boolean[Day.values().length];
  }

  public Week(Week rhs) {
    bitmask = rhs.bitmask.clone();
  }

  public Week(boolean[] bitmask) {
    if (bitmask.length != Day.values().length) {
      throw new IllegalArgumentException("Wrong sized bitmask: " + bitmask.length);
    }
    this.bitmask = bitmask;
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

  public CharSequence[] names(Context context) {
    CharSequence[] nameList = new CharSequence[Day.values().length];
    for (Day day : Day.values()) {
      nameList[day.ordinal()] = context.getString(day.stringId());
    }
    return nameList;
  }

  public String toString(Context context) {
    if (this.equals(NO_REPEATS)) {
      return context.getString(R.string.no_repeats);
    }
    if (this.equals(EVERYDAY)) {
      return context.getString(R.string.everyday);
    }
    if (this.equals(WEEKDAYS)) {
      return context.getString(R.string.weekdays);
    }
    if (this.equals(WEEKENDS)) {
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

  @Override
  public int describeContents() {
    return 0;
  }

  public static final Parcelable.Creator<Week> CREATOR =
    new Parcelable.Creator<Week>() {
      @Override
      public Week createFromParcel(Parcel source) {
        return new Week(source);
      }
      @Override
      public Week[] newArray(int size) {
        return new Week[size];
      }
    };

  public static Day calendarToDay(int dow) {
    int ordinalOffset = dow - Calendar.SUNDAY;
    return Day.values()[ordinalOffset];
  }
}
