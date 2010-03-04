package com.angrydoughnuts.android.alarmclock;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

public class DbAccessor {
  private DbHelper db;
  private SQLiteDatabase rDb;
  private SQLiteDatabase rwDb;

  public DbAccessor(Context context) {
    db = new DbHelper(context);
    rwDb = db.getWritableDatabase();
    rDb = db.getReadableDatabase();
  }

  public void closeConnections() {
    rDb.close();
    rwDb.close();
  }

  public Cursor getAlarmList() {
    return rDb.query(
        DbHelper.DB_TABLE_ALARMS,
        new String[] {
          DbHelper.ALARMS_COL__ID,
          DbHelper.ALARMS_COL_TIME,
          DbHelper.ALARMS_COL_ENABLED
        }, null, null, null, null,
        DbHelper.ALARMS_COL_TIME + " DESC");
  }

  public long newAlarm(int minutesAfterMidnight) {
    // TODO(cgallek) make sure this time doesn't exist yet.
    ContentValues values = new ContentValues(2);
    values.put(DbHelper.ALARMS_COL_TIME, minutesAfterMidnight);
    values.put(DbHelper.ALARMS_COL_ENABLED, false);
    long id = rwDb.insert(DbHelper.DB_TABLE_ALARMS, null, values);
    if (id < 0) {
      throw new IllegalStateException("Unable to insert into database");
    }
    return id;
  }

  public boolean deleteAlarm(long alarmId) {
    int count = rDb.delete(DbHelper.DB_TABLE_ALARMS,
        DbHelper.ALARMS_COL__ID + " = " + alarmId, null);
    return count > 0;
  }

  public boolean enableAlarm(long alarmId, boolean enabled) {
    ContentValues values = new ContentValues(1);
    values.put(DbHelper.ALARMS_COL_ENABLED, enabled);
    int count = rwDb.update(DbHelper.DB_TABLE_ALARMS, values,
        DbHelper.ALARMS_COL__ID + " = " + alarmId, null);
    return count != 0;
  }

  public int alarmTime(long alarmId) {
    Cursor cursor = rDb.query(DbHelper.DB_TABLE_ALARMS,
        new String[] { DbHelper.ALARMS_COL_TIME },
        DbHelper.ALARMS_COL__ID + " = " + alarmId, null, null, null, null);
    if (cursor.getCount() != 1) {
      cursor.close();
      return -1;
    }
    cursor.moveToFirst();
    int time = cursor.getInt(0);
    cursor.close();
    return time;
  }

  // TODO(cgallek): use a settings object instead of individual
  // parameters.
  public boolean writeAlarmSettings(long alarmId, AlarmSettings settings) {
    Cursor cursor = rDb.query(DbHelper.DB_TABLE_SETTINGS,
        new String[] { DbHelper.SETTINGS_COL_ID },
        DbHelper.SETTINGS_COL_ID + " = " + alarmId, null, null, null, null);

    boolean success = false;
    if (cursor.getCount() < 1) {
      success = rwDb.insert(DbHelper.DB_TABLE_SETTINGS, null, settings.contentValues(alarmId)) >= 0;
    } else {
      success = rwDb.update(DbHelper.DB_TABLE_SETTINGS, settings.contentValues(alarmId),
          DbHelper.SETTINGS_COL_ID + " = " + alarmId, null) == 1;
    }
    cursor.close();
    return success;
  }

  public AlarmSettings readAlarmSettings(long alarmId) {
    Cursor cursor = rDb.query(DbHelper.DB_TABLE_SETTINGS, 
        new String[] { DbHelper.SETTINGS_COL_ID, DbHelper.SETTINGS_COL_TONE_URL },
        DbHelper.SETTINGS_COL_ID + " = " + alarmId, null, null, null, null);

    if (cursor.getCount() != 1) {
      cursor.close();
      return new AlarmSettings();
    }

    AlarmSettings settings = new AlarmSettings();

    cursor.moveToFirst();
    Uri tone = Uri.parse(cursor.getString(cursor.getColumnIndex(DbHelper.SETTINGS_COL_TONE_URL)));
    settings.setTone(tone);
    cursor.close();
    return settings;
  }

  // TODO(cgallek): remove this function in favor of returning
  // defaults from the previous function when id is not found.
  public AlarmSettings readDefaultAlarmSettings() {
    return readAlarmSettings(AlarmSettings.DEFAULT_SETTINGS_ID);
  }
}
