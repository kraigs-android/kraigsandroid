package com.angrydoughnuts.android.alarmclock;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DbHelper extends SQLiteOpenHelper {
  public static final String DB_NAME = "alarmclock";
  public static final int DB_VERSION = 1;

  public static final String DB_TABLE_ALARMS = "alarms";
  public static final String ALARMS_COL__ID = "_id";
  public static final String ALARMS_COL_TIME = "time";
  public static final String ALARMS_COL_ENABLED = "enabled";

  public static final String DB_TABLE_SETTINGS = "settings";
  public static final String ALARMS_COL_ID = "id";
  public static final String ALARMS_COL_TONE_URL = "tone_url";

  public DbHelper(Context context) {
    super(context, DB_NAME, null, DB_VERSION);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    // Alarm metadata table:
    // |(auto primary) | (0 to 86399) | (boolean) |
    // |     _id       |    time      |  enabled  |
    // time is seconds past midnight.
    // TODO(cgallek): change time from seconds to minutes.
    db.execSQL("CREATE TABLE " + DB_TABLE_ALARMS + " (" 
        + ALARMS_COL__ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
        + ALARMS_COL_TIME + " UNSIGNED INTEGER (0, 86399),"
        + ALARMS_COL_ENABLED + " UNSIGNED INTEGER (0, 1))");
    // |(primary) | (string) |
    // |   id     | tone_url |
    // time is seconds past midnight.
    db.execSQL("CREATE TABLE " + DB_TABLE_SETTINGS + " (" 
        + ALARMS_COL_ID + " INTEGER PRIMARY KEY, "
        + ALARMS_COL_TONE_URL + " TEXT)");

  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
  }

}
