/****************************************************************************
 * Copyright 2009 kraigs.android@gmail.com
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

package com.angrydoughnuts.android.brightprof;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DbHelper extends SQLiteOpenHelper {
  public static final String DB_NAME = "brightprof";
  public static final String DB_TABLE = "profiles";
  public static final int DB_VERSION = 1;
  public static final String PROF_ID_COL = "_id";
  public static final String PROF_NAME_COL = "name";
  public static final String PROF_VALUE_COL = "value";

  public DbHelper(Context context) {
    super(context, DB_NAME, null, DB_VERSION);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    db.execSQL("CREATE TABLE " + DB_TABLE + " (" + PROF_ID_COL
        + " INTEGER PRIMARY KEY AUTOINCREMENT, " + PROF_NAME_COL
        + " TEXT NOT NULL," + PROF_VALUE_COL + " UNSIGNED INTEGER (0, 100))");
    db.execSQL("INSERT INTO " + DB_TABLE + "( " + PROF_NAME_COL + ", "
        + PROF_VALUE_COL + ") VALUES ('Low', 5)");
    db.execSQL("INSERT INTO " + DB_TABLE + "( " + PROF_NAME_COL + ", "
        + PROF_VALUE_COL + ") VALUES ('Normal', 25)");
    db.execSQL("INSERT INTO " + DB_TABLE + "( " + PROF_NAME_COL + ", "
        + PROF_VALUE_COL + ") VALUES ('High', 100)");
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    // First version, nothing to do yet.
  }
}
