package com.angrydoughnuts.android.alarmclock;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.Settings;

public class AlarmSettings implements Parcelable {
  static public final long DEFAULT_SETTINGS_ID = -1;

  private Uri tone;
  private int snoozeMinutes;

  public Uri getTone() {
    return tone;
  }

  public void setTone(Uri tone) {
    this.tone = tone;
  }

  public int getSnoozeMinutes() {
    return snoozeMinutes;
  }

  public void setSnoozeMinutes(int minutes) {
    // TODO(cgallek): These all need validation checks.
    this.snoozeMinutes = minutes;
  }

  public ContentValues contentValues(long alarmId) {
    ContentValues values = new ContentValues(3);
    values.put(DbHelper.SETTINGS_COL_ID, alarmId);
    values.put(DbHelper.SETTINGS_COL_TONE_URL, tone.toString());
    values.put(DbHelper.SETTINGS_COL_SNOOZE, snoozeMinutes);
    return values;
  }

  static public String[] contentColumns() {
    return new String[] {
      DbHelper.SETTINGS_COL_ID,
      DbHelper.SETTINGS_COL_TONE_URL,
      DbHelper.SETTINGS_COL_SNOOZE
    };
  }

  // TODO(cgallek): default constructor to initialize defaults.
  public AlarmSettings() {
    tone = Settings.System.DEFAULT_ALARM_ALERT_URI;
    snoozeMinutes = 10;
  }

  public AlarmSettings(Cursor cursor) {
    cursor.moveToFirst();
    Uri tone = Uri.parse(cursor.getString(cursor.getColumnIndex(DbHelper.SETTINGS_COL_TONE_URL)));
    setTone(tone);

    int minutes = cursor.getInt(cursor.getColumnIndex(DbHelper.SETTINGS_COL_SNOOZE));
    this.setSnoozeMinutes(minutes);
  }

  public AlarmSettings(Parcel source) {
    tone = source.readParcelable(getClass().getClassLoader());
    snoozeMinutes = source.readInt();
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeParcelable(tone, 0);
    dest.writeInt(snoozeMinutes);
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
