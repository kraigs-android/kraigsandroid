package com.angrydoughnuts.android.alarmclock;

import java.util.Calendar;

import android.os.Parcel;
import android.os.Parcelable;

public class CalendarParcel implements Parcelable {
  private Calendar calendar;

  public CalendarParcel(Calendar calendar) {
    this.calendar = calendar;
  }

  public Calendar calendar() {
    return calendar;
  }

  private CalendarParcel(Parcel source) {
    calendar = (Calendar) source.readSerializable();
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeSerializable(calendar);
  }

  @Override
  public int describeContents() {
    return 0;
  }

  public static final Parcelable.Creator<CalendarParcel> CREATOR =
    new Parcelable.Creator<CalendarParcel>() {
      @Override
      public CalendarParcel createFromParcel(Parcel source) {
        return new CalendarParcel(source);
      }
      @Override
      public CalendarParcel[] newArray(int size) {
        return new CalendarParcel[size];
      }
    };
}
