package com.angrydoughnuts.android.alarmclock;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;

public class DebugUtil {
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
