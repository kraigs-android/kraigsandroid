package com.angrydoughnuts.android.alarmclock;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

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
          DbHelper.ALARMS_COL_ID,
          DbHelper.ALARMS_COL_TIME,
          DbHelper.ALARMS_COL_ENABLED
        }, null, null, null, null,
        DbHelper.ALARMS_COL_TIME + " DESC");
  }

  public long newAlarm(int time) {
    // TODO(cgallek) make sure this time doesn't exist yet.
    ContentValues values = new ContentValues(2);
    values.put(DbHelper.ALARMS_COL_TIME, time);
    values.put(DbHelper.ALARMS_COL_ENABLED, true);
    long id = rwDb.insert(DbHelper.DB_TABLE_ALARMS, null, values);
    if (id < 0) {
      throw new IllegalStateException("Unable to insert into database");
    }
    return id;
  }
}
