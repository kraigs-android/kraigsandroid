/****************************************************************************
 * Copyright 2010 kraigs.android@gmail.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 ****************************************************************************/

package com.angrydoughnuts.android.alarmclock;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

/**
 * This class contains all of the settings data for a given alarm.  It also
 * provides the mapping from this data to the respective columns in the
 * persistent settings database.
 */
public final class AlarmSettings {
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
    tone = AlarmUtil.getDefaultAlarmUri();
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
    if (minutes < 1) {
      minutes = 1;
    } else if (minutes > 60) {
      minutes = 60;
    }
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
    if (volumeStartPercent < 0) {
      volumeStartPercent = 0;
    } else if (volumeStartPercent > 100) {
      volumeStartPercent = 100;
    }
    this.volumeStartPercent = volumeStartPercent;
  }

  public int getVolumeEndPercent() {
    return volumeEndPercent;
  }

  public void setVolumeEndPercent(int volumeEndPercent) {
    if (volumeEndPercent < 0) {
      volumeEndPercent = 0;
    } else if (volumeEndPercent > 100) {
      volumeEndPercent = 100;
    }
    this.volumeEndPercent = volumeEndPercent;
  }

  public int getVolumeChangeTimeSec() {
    return volumeChangeTimeSec;
  }

  public void setVolumeChangeTimeSec(int volumeChangeTimeSec) {
    if (volumeChangeTimeSec < 1) {
      volumeChangeTimeSec = 1;
    } else if (volumeChangeTimeSec > 600) {
      volumeChangeTimeSec = 600;
    }
    this.volumeChangeTimeSec = volumeChangeTimeSec;
  }
}
