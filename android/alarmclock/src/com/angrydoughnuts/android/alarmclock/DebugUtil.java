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

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;

public final class DebugUtil {
  private static String APP_PREFS = "AlarmClockPreferences";
  private static String DEBUG_PREF = "debug";

  public enum DebugMode { DEFAULT, DEBUG, NO_DEBUG };

  static public void setDebugMode(Context c, DebugMode mode) {
    SharedPreferences prefs = c.getSharedPreferences(APP_PREFS, 0);
    Editor editor = prefs.edit();
    editor.putInt(DEBUG_PREF, mode.ordinal());
    editor.commit();
  }

  static public boolean isDebugMode(Context c) {
    SharedPreferences prefs = c.getSharedPreferences(APP_PREFS, 0);
    int value = prefs.getInt(DEBUG_PREF, DebugMode.DEFAULT.ordinal());
    switch (DebugMode.values()[value]) {
      case DEBUG:
        return true;
      case NO_DEBUG:
        return false;
      default:
        return (c.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) > 0;
    }
  }
}
