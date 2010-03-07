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
      throw new IllegalStateException("Unknown debug mode setting.");
    }
  }
}
