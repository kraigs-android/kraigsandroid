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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceChangeListener;
import android.provider.Settings;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

/**
 * Simple preferences activity to display/manage the shared preferences
 * that make up the global application settings.
 */
public class ActivityAppSettings extends PreferenceActivity {
  private enum Dialogs { CUSTOM_LOCK_SCREEN }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    addPreferencesFromResource(R.xml.app_settings);

    OnPreferenceChangeListener refreshListener = new OnPreferenceChangeListener() {
      @Override
      public boolean onPreferenceChange(Preference preference, Object newValue) {
        // Clear the lock screen text if the user disables the feature.
        if (preference.getKey().equals(AppSettings.LOCK_SCREEN)) {
          Settings.System.putString(getContentResolver(), Settings.System.NEXT_ALARM_FORMATTED, "");

          final String custom_lock_screen = getResources().getStringArray(R.array.lock_screen_values)[4];
          if (newValue.equals(custom_lock_screen)) {
            showDialog(Dialogs.CUSTOM_LOCK_SCREEN.ordinal());
          }
        }

        final Intent causeRefresh = new Intent(getApplicationContext(), AlarmClockService.class);
        causeRefresh.putExtra(AlarmClockService.COMMAND_EXTRA, AlarmClockService.COMMAND_NOTIFICATION_REFRESH);
        startService(causeRefresh);
        return true;
      }
    };

    // Refresh the notification icon when the user changes these preferences.
    final Preference notification_icon = findPreference(AppSettings.NOTIFICATION_ICON);
    notification_icon.setOnPreferenceChangeListener(refreshListener);
    final Preference lock_screen = findPreference(AppSettings.LOCK_SCREEN);
    lock_screen.setOnPreferenceChangeListener(refreshListener);
  }

  @Override
  protected Dialog onCreateDialog(int id) {
    switch (Dialogs.values()[id]) {
      case CUSTOM_LOCK_SCREEN:
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        final View lockTextView = getLayoutInflater().inflate(R.layout.custom_lock_screen_dialog, null);
        final EditText editText = (EditText) lockTextView.findViewById(R.id.custom_lock_screen_text);
        editText.setText(prefs.getString(AppSettings.CUSTOM_LOCK_SCREEN_TEXT, ""));
        final CheckBox persistentCheck = (CheckBox) lockTextView.findViewById(R.id.custom_lock_screen_persistent);
        persistentCheck.setChecked(prefs.getBoolean(AppSettings.CUSTOM_LOCK_SCREEN_PERSISTENT, false));
        final AlertDialog.Builder lockTextBuilder = new AlertDialog.Builder(this);
        lockTextBuilder.setTitle(R.string.custom_lock_screen_text);
        lockTextBuilder.setView(lockTextView);
        lockTextBuilder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            Editor editor = prefs.edit();
            editor.putString(AppSettings.CUSTOM_LOCK_SCREEN_TEXT, editText.getText().toString());
            editor.putBoolean(AppSettings.CUSTOM_LOCK_SCREEN_PERSISTENT, persistentCheck.isChecked());
            editor.commit();
            final Intent causeRefresh = new Intent(getApplicationContext(), AlarmClockService.class);
            causeRefresh.putExtra(AlarmClockService.COMMAND_EXTRA, AlarmClockService.COMMAND_NOTIFICATION_REFRESH);
            startService(causeRefresh);
            dismissDialog(Dialogs.CUSTOM_LOCK_SCREEN.ordinal());
          }
        });
        lockTextBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            dismissDialog(Dialogs.CUSTOM_LOCK_SCREEN.ordinal());
          }
        });
        return lockTextBuilder.create();
      default:
        return super.onCreateDialog(id);
    }
  }
}
