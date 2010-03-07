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

import java.text.SimpleDateFormat;
import java.util.Calendar;

import com.angrydoughnuts.android.alarmclock.Week.Day;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.format.DateFormat;

/**
 * A class that encapsulates an alarm time.  It represents a time between 00:00
 * and 23:59.  It also contains a list of days on which this alarm should be
 * rescheduled.  If no days are listed, the alarm is only scheduled once
 * per time it is enabled.  The class is Parcelable so that it can be
 * returned as an object from the AlarmClockService and is Comparable so that
 * an ordered list can be created in PendingAlarmList.
 */
public final class AlarmTime implements Parcelable, Comparable<AlarmTime> {
  private Calendar calendar;
  private Week daysOfWeek;

  /**
   * Copy constructor.
   * @param rhs
   */
  public AlarmTime(AlarmTime rhs) {
    calendar = (Calendar) rhs.calendar.clone();
    daysOfWeek = new Week(rhs.daysOfWeek);
  }

  /**
   * Construct an AlarmTime for the next occurrence of this hour/minute/second.
   * It will not repeat.
   * @param hourOfDay
   * @param minute
   * @param second
   */
  public AlarmTime(int hourOfDay, int minute, int second) {
    this(hourOfDay, minute, second, new Week());
  }

  /**
   * Construct an AlarmTime for the next occurrence of this hour/minute/second
   * which occurs on the specified days of the week.
   * @param hourOfDay
   * @param minute
   * @param second
   * @param daysOfWeek
   */
  public AlarmTime(int hourOfDay, int minute, int second, Week daysOfWeek) {
    this.calendar = Calendar.getInstance();
    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
    calendar.set(Calendar.MINUTE, minute);
    calendar.set(Calendar.SECOND, second);
    this.daysOfWeek = daysOfWeek;
    
    findNextOccurrence();
  }

  private void findNextOccurrence() {
    Calendar now = Calendar.getInstance();

    // If this hour/minute/second has already occurred today, move to tomorrow.
    if (calendar.before(now)) {
      calendar.add(Calendar.DATE, 1);
    }

    if (calendar.before(now)) {
      throw new IllegalStateException("Inconsistent calendar.");
    }

    // If there are no repeats requested, there is nothing left to do.
    if (daysOfWeek.equals(Week.NO_REPEATS)) {
      return;
    }

    // Keep incrementing days until we hit a suitable day of the week.
    for (int i = 0; i < Day.values().length; ++i) {
      Day alarmDay = Week.calendarToDay(calendar.get(Calendar.DAY_OF_WEEK));
      if (daysOfWeek.hasDay(alarmDay)) {
        return;
      }
      calendar.add(Calendar.DATE, 1);
    }

    throw new IllegalStateException("Didn't find a suitable date for alarm.");
  }

  @Override
  public int compareTo(AlarmTime another) {
    return calendar.compareTo(another.calendar);
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof AlarmTime)) {
      return false;
    }
    AlarmTime rhs = (AlarmTime) o;
    if (!calendar.equals(rhs.calendar)) {
      return false;
    }
    return this.daysOfWeek.equals(rhs.daysOfWeek);
  }

  public String toString() {
    SimpleDateFormat formatter = new SimpleDateFormat("HH:mm.ss MMMM dd yyyy");
    return formatter.format(calendar.getTimeInMillis());
  }

  public String localizedString(Context context) {
    boolean is24HourFormat = DateFormat.is24HourFormat(context);
    String format = "";
    String second = "";
    if (AppSettings.isDebugMode(context)) {
      second = ".ss";
    }
    if (is24HourFormat) {
      format = "HH:mm" + second;
    } else {
      format = "h:mm" + second + " aaa";
    }

    SimpleDateFormat formatter = new SimpleDateFormat(format);
    return formatter.format(calendar.getTime());
  }

  public Calendar calendar() {
    return calendar;
  }

  public Week getDaysOfWeek() {
    return daysOfWeek;
  }

  public boolean repeats() {
    return !daysOfWeek.equals(Week.NO_REPEATS);
  }

  public String timeUntilString(Context c) {
    Calendar now = Calendar.getInstance();
    if (calendar.before(now)) {
      return c.getString(R.string.alarm_has_occurred);
    }
    long now_min = now.getTimeInMillis() / 1000 / 60;
    long then_min = calendar.getTimeInMillis() / 1000 / 60;
    long difference_minutes = then_min - now_min;
    long days = difference_minutes / (60 * 24);
    long hours = difference_minutes % (60 * 24);
    long minutes = hours % 60;
    hours = hours / 60;

    String value = "";
    if (days == 1) {
      value += c.getString(R.string.day, days) + " ";
    } else if (days > 1) {
      value += c.getString(R.string.days, days) + " ";
    }
    if (hours == 1) {
      value += c.getString(R.string.hour, hours) + " ";
    } else if (hours > 1) {
      value += c.getString(R.string.hours, hours) + " ";
    }
    if (minutes == 1) {
      value += c.getString(R.string.minute, minutes) + " ";
    } else if (minutes > 1) {
      value += c.getString(R.string.minutes, minutes) + " ";
    }
    return value;
  }

  /**
   * A static method which generates an AlarmTime object @minutes in the future.
   * It first truncates seconds (rounds down to the nearest minute) before
   * adding @minutes
   * @param minutes
   * @return
   */
  public static AlarmTime snoozeInMillisUTC(int minutes) {
    Calendar snooze = Calendar.getInstance();
    snooze.set(Calendar.SECOND, 0);
    snooze.add(Calendar.MINUTE, minutes);
    return new AlarmTime(
        snooze.get(Calendar.HOUR_OF_DAY),
        snooze.get(Calendar.MINUTE),
        snooze.get(Calendar.SECOND));
  }

  private AlarmTime(Parcel source) {
    this.calendar = (Calendar) source.readSerializable();
    this.daysOfWeek = source.readParcelable(null);
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeSerializable(calendar);
    dest.writeParcelable(daysOfWeek, 0);
  }

  public static final Parcelable.Creator<AlarmTime> CREATOR =
    new Parcelable.Creator<AlarmTime>() {
      @Override
      public AlarmTime createFromParcel(Parcel source) {
        return new AlarmTime(source);
      }
      @Override
      public AlarmTime[] newArray(int size) {
        return new AlarmTime[size];
      }
    };

  @Override
  public int describeContents() {
    return 0;
  }
}
