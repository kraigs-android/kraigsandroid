package com.angrydoughnuts.android.alarmclock;

import android.os.Parcel;
import android.os.Parcelable;

public class Week implements Parcelable {
  private boolean[] bitmask;

  public Week(Parcel source) {
    bitmask = source.createBooleanArray();
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeBooleanArray(bitmask);
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
}
