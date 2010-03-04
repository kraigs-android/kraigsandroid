package com.angrydoughnuts.android.alarmclock;

import android.content.ContentValues;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

public class AlarmSettings implements Parcelable {
  static public final long DEFAULT_SETTINGS_ID = -1;

  private Uri tone;

  public Uri getTone() {
    return tone;
  }

  public void setTone(Uri tone) {
    this.tone = tone;
  }

  public ContentValues contentValues(long alarmId) {
    ContentValues values = new ContentValues(2);
    values.put(DbHelper.SETTINGS_COL_ID, alarmId);
    values.put(DbHelper.SETTINGS_COL_TONE_URL, tone.toString());
    return values;
  }

  // TODO(cgallek): default constructor to initialize defaults.
  public AlarmSettings() {
    tone = Uri.parse("none");
  }

  public AlarmSettings(Parcel source) {
    tone = source.readParcelable(getClass().getClassLoader());
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeParcelable(tone, 0);
  }

  @Override
  public int describeContents() {
    return 0;
  }

  public static final Parcelable.Creator<AlarmSettings> CREATOR =
    new Parcelable.Creator<AlarmSettings>() {
      @Override
      public AlarmSettings createFromParcel(Parcel source) {
        return new AlarmSettings(source);
      }
      @Override
      public AlarmSettings[] newArray(int size) {
        return new AlarmSettings[size];
      }
    
  };
}
