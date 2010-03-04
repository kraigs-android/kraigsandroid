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
  private boolean vibrate;
  private int volumeStartPercent;
  private int volumeEndPercent;
  private int volumeChangeTimeSec;

  public ContentValues contentValues(long alarmId) {
    ContentValues values = new ContentValues(3);
    values.put(DbHelper.SETTINGS_COL_ID, alarmId);
    values.put(DbHelper.SETTINGS_COL_TONE_URL, tone.toString());
    values.put(DbHelper.SETTINGS_COL_SNOOZE, snoozeMinutes);
    values.put(DbHelper.SETTINGS_COL_VIBRATE, vibrate);
    values.put(DbHelper.SETTINGS_COL_VOLUME_STARTING, volumeStartPercent);
    values.put(DbHelper.SETTINGS_COL_VOLUME_ENDING, volumeEndPercent);
    values.put(DbHelper.SETTINGS_COL_VOLUME_TIME, volumeChangeTimeSec);
    return values;
  }

  static public String[] contentColumns() {
    return new String[] {
      DbHelper.SETTINGS_COL_ID,
      DbHelper.SETTINGS_COL_TONE_URL,
      DbHelper.SETTINGS_COL_SNOOZE,
      DbHelper.SETTINGS_COL_VIBRATE,
      DbHelper.SETTINGS_COL_VOLUME_STARTING,
      DbHelper.SETTINGS_COL_VOLUME_ENDING,
      DbHelper.SETTINGS_COL_VOLUME_TIME
    };
  }

  // TODO(cgallek): default constructor to initialize defaults.
  public AlarmSettings() {
    tone = Settings.System.DEFAULT_ALARM_ALERT_URI;
    snoozeMinutes = 10;
    vibrate = false;
    volumeStartPercent = 0;
    volumeEndPercent = 100;
    volumeChangeTimeSec = 20;
  }

  public AlarmSettings(Cursor cursor) {
    cursor.moveToFirst();
    tone = Uri.parse(cursor.getString(cursor.getColumnIndex(DbHelper.SETTINGS_COL_TONE_URL)));
    snoozeMinutes = cursor.getInt(cursor.getColumnIndex(DbHelper.SETTINGS_COL_SNOOZE));
    vibrate = cursor.getInt(cursor.getColumnIndex(DbHelper.SETTINGS_COL_VIBRATE)) == 1;
    volumeStartPercent = cursor.getInt(cursor.getColumnIndex(DbHelper.SETTINGS_COL_VOLUME_STARTING));
    volumeEndPercent = cursor.getInt(cursor.getColumnIndex(DbHelper.SETTINGS_COL_VOLUME_ENDING));
    volumeChangeTimeSec = cursor.getInt(cursor.getColumnIndex(DbHelper.SETTINGS_COL_VOLUME_TIME));
  }

  public AlarmSettings(Parcel source) {
    tone = source.readParcelable(getClass().getClassLoader());
    snoozeMinutes = source.readInt();
    vibrate = source.readInt() == 1 ? true : false;
    volumeStartPercent = source.readInt();
    volumeEndPercent = source.readInt();
    volumeChangeTimeSec = source.readInt();
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeParcelable(tone, 0);
    dest.writeInt(snoozeMinutes);
    dest.writeInt(vibrate ? 1 : 0);
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
    // TODO(cgallek): These all need validation checks.  Add validation
    // checks and then update all internal modifiers to use setters.
    this.snoozeMinutes = minutes;
  }

  public boolean getVibrate() {
    return vibrate;
  }

  public void setVibrate(boolean vibrate) {
    this.vibrate = vibrate;
  }

  public int getVolumeStartPercent() {
    return volumeStartPercent;
  }

  public void setVolumeStartPercent(int volumeStartPercent) {
    this.volumeStartPercent = volumeStartPercent;
  }

  public int getVolumeEndPercent() {
    return volumeEndPercent;
  }

  public void setVolumeEndPercent(int volumeEndPercent) {
    this.volumeEndPercent = volumeEndPercent;
  }

  public int getVolumeChangeTimeSec() {
    return volumeChangeTimeSec;
  }

  public void setVolumeChangeTimeSec(int volumeChangeTimeSec) {
    this.volumeChangeTimeSec = volumeChangeTimeSec;
  }
}
