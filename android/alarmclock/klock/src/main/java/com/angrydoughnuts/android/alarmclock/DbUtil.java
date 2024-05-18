/****************************************************************************
 * Copyright 2016 kraigs.android@gmail.com
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

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import java.util.Calendar;

public class DbUtil {
  public static final class Alarm {
    public final int time;
    public final boolean enabled;
    public final String label;
    public final int repeat;
    public final long next_snooze;

    public static Alarm get(Context context, long id) {
      Alarm s = null;
      final Cursor c = context.getContentResolver().query(
          ContentUris.withAppendedId(AlarmClockProvider.ALARMS_URI, id),
          new String[] {
            AlarmClockProvider.AlarmEntry.TIME,
            AlarmClockProvider.AlarmEntry.ENABLED,
            AlarmClockProvider.AlarmEntry.NAME,
            AlarmClockProvider.AlarmEntry.DAY_OF_WEEK,
            AlarmClockProvider.AlarmEntry.NEXT_SNOOZE },
          null, null, null);
      if (c.moveToFirst())
        s = new Alarm(c);
      else
        s = new Alarm();
      c.close();
      return s;
    }

    public static Alarm getNextEnabled(Context context) {
      final Cursor c = context.getContentResolver().query(
          AlarmClockProvider.ALARMS_URI,
          new String[] { AlarmClockProvider.AlarmEntry.TIME,
                         AlarmClockProvider.AlarmEntry.ENABLED,
                         AlarmClockProvider.AlarmEntry.NAME,
                         AlarmClockProvider.AlarmEntry.DAY_OF_WEEK,
                         AlarmClockProvider.AlarmEntry.NEXT_SNOOZE },
          AlarmClockProvider.AlarmEntry.ENABLED + " == 1",
          null, null);

      if (c.getCount() == 0) {
        c.close();
        return null;
      }

      final Calendar now = Calendar.getInstance();
      DbUtil.Alarm alarm = null;
      Calendar next = null;
      while (c.moveToNext()) {
        final DbUtil.Alarm a = new DbUtil.Alarm(c);
        final Calendar n =
          TimeUtil.nextOccurrence(now, a.time, a.repeat, a.next_snooze);
        if (next == null || n.before(next)) {
          alarm = a;
          next = n;
        }
      }
      c.close();
      return alarm;
    }

    public Alarm(Cursor c) {
      time = c.getInt(c.getColumnIndex(AlarmClockProvider.AlarmEntry.TIME));
      enabled = c.getInt(c.getColumnIndex(
          AlarmClockProvider.AlarmEntry.ENABLED)) != 0;
      label = c.getString(c.getColumnIndex(AlarmClockProvider.AlarmEntry.NAME));
      repeat = c.getInt(c.getColumnIndex(
          AlarmClockProvider.AlarmEntry.DAY_OF_WEEK));
      next_snooze = c.getLong(c.getColumnIndex(
          AlarmClockProvider.AlarmEntry.NEXT_SNOOZE));
    }

    private Alarm() {
      time = 0;
      enabled = false;
      label = "Not found";
      repeat = 0;
      next_snooze = 0;
    }
  }

  public static final class Settings {
    public static final long DEFAULTS_ID = Long.MAX_VALUE;

    public final Uri tone_url;
    public final String tone_name;
    public final int snooze;
    public final boolean vibrate;
    public final int volume_starting;
    public final int volume_ending;
    public final int volume_time;
    public final boolean dismiss_by_button;

    private static final Uri TONE_URL_DEFAULT =
      android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI;
    private static final int SNOOZE_DEFAULT = 10;
    private static final boolean VIBRATE_DEFAULT = false;
    private static final int VOLUME_STARTING_DEFAULT = 0;
    private static final int VOLUME_ENDING_DEFAULT = 100;
    private static final int VOLUME_TIME_DEFAULT = 20;
    private static final boolean DISMISS_BY_BUTTON_DEFAULT = false;

    public static Settings get(Context context, long id) {
      Settings s = null;

      // Lookup settings for 'id'.
      Cursor c = query(context, id);
      if (c.moveToFirst())
        s = new Settings(c);
      c.close();

      // If not found, lookup default settings.
      if (s == null) {
        c = query(context, DEFAULTS_ID);
        if (c.moveToFirst())
          s = new Settings(c);
        c.close();
      }

      // If still not found, use application defaults.
      if (s != null)
        return s;
      else
        return new Settings(context);
    }

    private static Cursor query(Context context, long id) {
      return context.getContentResolver().query(
          ContentUris.withAppendedId(AlarmClockProvider.SETTINGS_URI, id),
          new String[] {
            AlarmClockProvider.SettingsEntry.TONE_URL,
            AlarmClockProvider.SettingsEntry.TONE_NAME,
            AlarmClockProvider.SettingsEntry.SNOOZE,
            AlarmClockProvider.SettingsEntry.VIBRATE,
            AlarmClockProvider.SettingsEntry.VOLUME_STARTING,
            AlarmClockProvider.SettingsEntry.VOLUME_ENDING,
            AlarmClockProvider.SettingsEntry.VOLUME_TIME,
            AlarmClockProvider.SettingsEntry.DISMISS_BY_BUTTON},
          null, null, null);
    }

    private Settings(Cursor c) {
      tone_url = Uri.parse(c.getString(c.getColumnIndex(
          AlarmClockProvider.SettingsEntry.TONE_URL)));
      tone_name = c.getString(c.getColumnIndex(
          AlarmClockProvider.SettingsEntry.TONE_NAME));
      snooze = c.getInt(c.getColumnIndex(
          AlarmClockProvider.SettingsEntry.SNOOZE));
      vibrate = c.getInt(c.getColumnIndex(
          AlarmClockProvider.SettingsEntry.VIBRATE)) != 0;
      volume_starting = c.getInt(c.getColumnIndex(
          AlarmClockProvider.SettingsEntry.VOLUME_STARTING));
      volume_ending = c.getInt(c.getColumnIndex(
          AlarmClockProvider.SettingsEntry.VOLUME_ENDING));
      volume_time = c.getInt(c.getColumnIndex(
          AlarmClockProvider.SettingsEntry.VOLUME_TIME));
      dismiss_by_button = c.getInt(c.getColumnIndex(
              AlarmClockProvider.SettingsEntry.DISMISS_BY_BUTTON)) != 0;
    }

    private Settings(Context c) {
      tone_url = TONE_URL_DEFAULT;
      tone_name = c.getString(R.string.system_default_alarm);
      snooze = SNOOZE_DEFAULT;
      vibrate = VIBRATE_DEFAULT;
      volume_starting = VOLUME_STARTING_DEFAULT;
      volume_ending = VOLUME_ENDING_DEFAULT;
      volume_time = VOLUME_TIME_DEFAULT;
      dismiss_by_button = DISMISS_BY_BUTTON_DEFAULT;
    }
  }
}
