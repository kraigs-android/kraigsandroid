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
  public static final boolean displayNotificationIcon(Context c) {
    final String NOTIFICATION_SETTING = c.getString(R.string.notification_icon_setting);

    final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
    return prefs.getBoolean(NOTIFICATION_SETTING, true);
  }

  public static final String lockScreenString(Context c, AlarmTime nextTime) {
    if (nextTime == null) {
      return "";
    }
    // TODO(cgallek): These constant values should really map to fixed values.
    // Currently, they map to the keys of the list preference, which could be
    // different in different languages (the keys and values currently map to
    // the same array).  If this internationalizes, the values will need to
    // become fixed.
    final String LOCK_SCREEN_SETTING = c.getString(R.string.lock_screen_setting);
    final String LOCK_SCREEN_COUNTDOWN = c.getString(R.string.lock_screen_countdown);
    final String LOCK_SCREEN_TIME = c.getString(R.string.lock_screen_time);
    final String LOCK_SCREEN_BOTH = c.getString(R.string.lock_screen_both);
    final String LOCK_SCREEN_NOTHING = c.getString(R.string.lock_screen_nothing);

    final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
    final String value = prefs.getString(LOCK_SCREEN_SETTING, LOCK_SCREEN_COUNTDOWN);

    final String time = nextTime.localizedString(c);
    final String countdown = nextTime.timeUntilString(c);

    if (value.equals(LOCK_SCREEN_COUNTDOWN)) {
      return countdown;
    } else if (value.equals(LOCK_SCREEN_TIME)) {
      return time;
    } else if (value.equals(LOCK_SCREEN_BOTH)) {
      return time + " (" + countdown + ")";
    } else if (value.equals(LOCK_SCREEN_NOTHING)) {
      return "";
    } else {
      throw new IllegalStateException("Unknown lockscreen preference: " + value);
    }
  }

  public static final boolean isDebugMode(Context c) {
    final String DEBUG_MODE_SETTING = c.getString(R.string.debug_mode_setting);
    final String DEBUG_ON = c.getString(R.string.debug_on);
    final String DEBUG_OFF = c.getString(R.string.debug_off);
    final String DEBUG_DEFAULT = c.getString(R.string.debug_default);

    final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
    final String value = prefs.getString(DEBUG_MODE_SETTING, DEBUG_DEFAULT);
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
}
