package com.angrydoughnuts.android.alarmclock;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Settings;

public class AlarmSettings {
  static public final long DEFAULT_SETTINGS_ID = -1;

  private Uri tone;
  private String toneName;
  private int snoozeMinutes;
  private boolean vibrate;
  private int volumeStartPercent;
  private int volumeEndPercent;
  private int volumeChangeTimeSec;

  public ContentValues contentValues(long alarmId) {
    ContentValues values = new ContentValues();
    values.put(DbHelper.SETTINGS_COL_ID, alarmId);
    values.put(DbHelper.SETTINGS_COL_TONE_URL, tone.toString());
    values.put(DbHelper.SETTINGS_COL_TONE_NAME, toneName);
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
      DbHelper.SETTINGS_COL_TONE_NAME,
      DbHelper.SETTINGS_COL_SNOOZE,
      DbHelper.SETTINGS_COL_VIBRATE,
      DbHelper.SETTINGS_COL_VOLUME_STARTING,
      DbHelper.SETTINGS_COL_VOLUME_ENDING,
      DbHelper.SETTINGS_COL_VOLUME_TIME
    };
  }

  public AlarmSettings() {
    // It would be nice to use DEFAULT_ALARM_ALERT_URI, but it's not available
    // until SDK 5.
    tone = Settings.System.DEFAULT_NOTIFICATION_URI;
    toneName = "Default";
    snoozeMinutes = 10;
    vibrate = false;
    volumeStartPercent = 0;
    volumeEndPercent = 100;
    volumeChangeTimeSec = 20;
  }

  public AlarmSettings(AlarmSettings rhs) {
    tone = rhs.tone;
    toneName = rhs.toneName;
    snoozeMinutes = rhs.snoozeMinutes;
    vibrate = rhs.vibrate;
    volumeStartPercent = rhs.volumeStartPercent;
    volumeEndPercent = rhs.volumeEndPercent;
    volumeChangeTimeSec = rhs.volumeChangeTimeSec;
  }

  public AlarmSettings(Cursor cursor) {
    cursor.moveToFirst();
    tone = Uri.parse(cursor.getString(cursor.getColumnIndex(DbHelper.SETTINGS_COL_TONE_URL)));
    toneName = cursor.getString(cursor.getColumnIndex(DbHelper.SETTINGS_COL_TONE_NAME));
    snoozeMinutes = cursor.getInt(cursor.getColumnIndex(DbHelper.SETTINGS_COL_SNOOZE));
    vibrate = cursor.getInt(cursor.getColumnIndex(DbHelper.SETTINGS_COL_VIBRATE)) == 1;
    volumeStartPercent = cursor.getInt(cursor.getColumnIndex(DbHelper.SETTINGS_COL_VOLUME_STARTING));
    volumeEndPercent = cursor.getInt(cursor.getColumnIndex(DbHelper.SETTINGS_COL_VOLUME_ENDING));
    volumeChangeTimeSec = cursor.getInt(cursor.getColumnIndex(DbHelper.SETTINGS_COL_VOLUME_TIME));
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof AlarmSettings)) {
      return false;
    }
    AlarmSettings rhs = (AlarmSettings) o;
    return tone.equals(rhs.tone)
      && toneName.equals(rhs.toneName)
      && snoozeMinutes == rhs.snoozeMinutes
      && vibrate == rhs.vibrate
      && volumeStartPercent == rhs.volumeStartPercent
      && volumeEndPercent == rhs.volumeEndPercent
      && volumeChangeTimeSec == rhs.volumeChangeTimeSec;
  }

  public Uri getTone() {
    return tone;
  }

  public void setTone(Uri tone, String name) {
    this.tone = tone;
    this.toneName = name;
  }

  public String getToneName() {
    return toneName;
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
