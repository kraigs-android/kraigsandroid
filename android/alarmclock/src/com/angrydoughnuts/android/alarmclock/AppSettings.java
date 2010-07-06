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
import android.content.pm.ApplicationInfo;
import android.preference.PreferenceManager;

/**
 * Utility class for accessing each of the global application settings.
 */
public final class AppSettings {
  // Some of these have an extra " in them because of an old copy/paste bug.
  // They are forever ingrained in the settings :-(
  public static final String DEBUG_MODE = "DEBUG_MODE";
  public static final String NOTIFICATION_ICON = "NOTIFICATION_ICON";
  public static final String LOCK_SCREEN = "LOCK_SCREEN";
  public static final String CUSTOM_LOCK_SCREEN_TEXT = "CUSTOM_LOCK_SCREEN";
  public static final String CUSTOM_LOCK_SCREEN_PERSISTENT = "CUSTOM_LOCK_PERSISTENT";
  public static final String ALARM_TIMEOUT = "ALARM_TIMEOUT";

  public static final boolean displayNotificationIcon(Context c) {
    final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
    return prefs.getBoolean(NOTIFICATION_ICON, true);
  }

  private static final String FORMAT_COUNTDOWN = "%c";
  private static final String FORMAT_TIME = "%t";
  private static final String FORMAT_BOTH = "%c (%t)";
  public static final String lockScreenString(Context c, AlarmTime nextTime) {
    final String[] values = c.getResources().getStringArray(R.array.lock_screen_values);
    final String LOCK_SCREEN_COUNTDOWN = values[0];
    final String LOCK_SCREEN_TIME = values[1];
    final String LOCK_SCREEN_BOTH = values[2];
    final String LOCK_SCREEN_NOTHING = values[3];
    final String LOCK_SCREEN_CUSTOM = values[4];

    final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
    final String value = prefs.getString(LOCK_SCREEN, LOCK_SCREEN_COUNTDOWN);
    final String customFormat = prefs.getString(CUSTOM_LOCK_SCREEN_TEXT, FORMAT_COUNTDOWN);
    // The lock screen message should be persistent iff the persistent setting
    // is set AND a custom lock screen message is set.
    final boolean persistent = prefs.getBoolean(CUSTOM_LOCK_SCREEN_PERSISTENT, false) && value.equals(LOCK_SCREEN_CUSTOM);

    if (value.equals(LOCK_SCREEN_NOTHING)) {
      return null;
    }

    // If no alarm is set and our lock message is not persistent, return
    // a clearing string.
    if (nextTime == null && !persistent) {
      return "";
    }

    String time = "";
    String countdown = "";
    if (nextTime != null) {
      time = nextTime.localizedString(c);
      countdown = nextTime.timeUntilString(c);
    }

    String text;
    if (value.equals(LOCK_SCREEN_COUNTDOWN)) {
      text = FORMAT_COUNTDOWN;
    } else if (value.equals(LOCK_SCREEN_TIME)) {
      text = FORMAT_TIME;
    } else if (value.equals(LOCK_SCREEN_BOTH)) {
      text = FORMAT_BOTH;
    } else if (value.equals(LOCK_SCREEN_CUSTOM)) {
      text = customFormat;
    } else {
      throw new IllegalStateException("Unknown lockscreen preference: " + value);
    }

    text = text.replace("%t", time);
    text = text.replace("%c", countdown);
    return text;
  }

  public static final boolean isDebugMode(Context c) {
    final String[] values = c.getResources().getStringArray(R.array.debug_values);
    final String DEBUG_DEFAULT = values[0];
    final String DEBUG_ON = values[1];
    final String DEBUG_OFF = values[2];

    final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
    final String value = prefs.getString(DEBUG_MODE, DEBUG_DEFAULT);
    if (value.equals(DEBUG_ON)) {
      return true;
    } else if (value.equals(DEBUG_OFF)) {
      return false;
    } else if (value.equals(DEBUG_DEFAULT)) {
      return (c.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) > 0;
    } else {
      throw new IllegalStateException("Unknown debug mode setting: "+ value);
    }
  }

  public static final int alarmTimeOutMins(Context c) {
    final String[] values = c.getResources().getStringArray(R.array.time_out_values);
    final String ONE_MIN = values[0];
    final String FIVE_MIN = values[1];
    final String TEN_MIN = values[2];
    final String THIRTY_MIN = values[3];
    final String SIXTY_MIN = values[4];

    final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
    final String value = prefs.getString(ALARM_TIMEOUT, TEN_MIN);
    if (value.equals(ONE_MIN)) {
      return 1;
    } else if (value.equals(FIVE_MIN)) {
      return 5;
    } else if (value.equals(TEN_MIN)) {
      return 10;
    } else if (value.equals(THIRTY_MIN)) {
      return 30;
    } else if (value.equals(SIXTY_MIN)) {
      return 60;
    } else {
      return 10;
    }
  }
}
