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

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceChangeListener;
import android.provider.Settings;

/**
 * Simple preferences activity to display/manage the shared preferences
 * that make up the global application settings.
 */
public class ActivityAppSettings extends PreferenceActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    addPreferencesFromResource(R.xml.app_settings);

    OnPreferenceChangeListener refreshListener = new OnPreferenceChangeListener() {
      @Override
      public boolean onPreferenceChange(Preference preference, Object newValue) {
        // Clear the lock screen text if the user disables the feature.
        if (preference.getKey().equals(getString(R.string.lock_screen_setting))) {
          Settings.System.putString(getContentResolver(), Settings.System.NEXT_ALARM_FORMATTED, "");
        }

        final Intent causeRefresh = new Intent(getApplicationContext(), AlarmClockService.class);
        causeRefresh.putExtra(AlarmClockService.COMMAND_EXTRA, AlarmClockService.COMMAND_NOTIFICATION_REFRESH);
        startService(causeRefresh);
        return true;
      }
    };

    // Refresh the notification icon when the user changes these preferences.
    final Preference notification_icon = findPreference(getString(R.string.notification_icon_setting));
    notification_icon.setOnPreferenceChangeListener(refreshListener);
    final Preference lock_screen = findPreference(getString(R.string.lock_screen_setting));
    lock_screen.setOnPreferenceChangeListener(refreshListener);
  }
}
