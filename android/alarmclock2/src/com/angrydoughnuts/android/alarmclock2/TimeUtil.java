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

package com.angrydoughnuts.android.alarmclock2;

import android.content.Context;
import android.text.format.DateFormat;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class TimeUtil {
  public static Calendar nextOccurrence(int secondsPastMidnight, int repeat) {
    return nextOccurrence(Calendar.getInstance(), secondsPastMidnight, repeat);
  }

  public static Calendar nextOccurrence(
      int secondsPastMidnight, int repeat, long nextSnooze) {
    return nextOccurrence(
        Calendar.getInstance(), secondsPastMidnight, repeat, nextSnooze);
  }

  public static Calendar nextOccurrence(
      Calendar now, int secondsPastMidnight, int repeat) {
    Calendar then = (Calendar)now.clone();
    then.set(Calendar.HOUR_OF_DAY, 0);
    then.set(Calendar.MINUTE, 0);
    then.set(Calendar.SECOND, 0);
    then.set(Calendar.MILLISECOND, 0);
    then.add(Calendar.SECOND, secondsPastMidnight);
    if (then.before(now))
      then.add(Calendar.DATE, 1);
    while (repeat > 0 && !dayIsRepeat(then.get(Calendar.DAY_OF_WEEK), repeat))
      then.add(Calendar.DATE, 1);
    return then;
  }

  public static Calendar nextOccurrence(
      Calendar now, int secondsPastMidnight, int repeat, long nextSnooze) {
    Calendar next = nextOccurrence(now, secondsPastMidnight, repeat);
    if (nextSnooze > now.getTimeInMillis()) {
      Calendar snooze = Calendar.getInstance();
      snooze.setTimeInMillis(nextSnooze);
      if (snooze.before(next))
        return snooze;
    }
    return next;
  }

  private static boolean dayIsRepeat(int calendarDow, int repeat) {
    switch (calendarDow) {
      case Calendar.SUNDAY:
        return (1 & repeat) != 0;
      case Calendar.MONDAY:
        return (2 & repeat) != 0;
      case Calendar.TUESDAY:
        return (4 & repeat) != 0;
      case Calendar.WEDNESDAY:
        return (8 & repeat) != 0;
      case Calendar.THURSDAY:
        return (16 & repeat) != 0;
      case Calendar.FRIDAY:
        return (32 & repeat) != 0;
      case Calendar.SATURDAY:
        return (64 & repeat) != 0;
    }
    return true;
  }

  private static final int EVERYDAY = 1 | 2 | 4 | 8 | 16 | 32 | 64;
  private static final int WEEKDAYS = 2 | 4 | 8 | 16 | 32;
  private static final int WEEKENDS = 1 | 64;
  public static String repeatString(int repeat) {
    if (repeat <= 0)
      return "";
    else if (repeat == EVERYDAY)
      // TODO: string
      return "Everyday";
    else if (repeat == WEEKDAYS)
      // TODO: string
      return "Weekdays";
    else if (repeat == WEEKENDS)
      // TODO: string
      return "Weekends";

    // TODO: string
    String s = "";
    if ((1 & repeat) != 0)
      s += "Su ";
    if ((2 & repeat) != 0)
      s += "M ";
    if ((4 & repeat) != 0)
      s += "Tu ";
    if ((8 & repeat) != 0)
      s += "W ";
    if ((16 & repeat) != 0)
      s += "Th ";
    if ((32 & repeat) != 0)
      s += "F ";
    if ((64 & repeat) != 0)
      s += "Sa ";
    return s;
  }

  public static Calendar nextMinute() {
    return nextMinute(Calendar.getInstance());
  }

  public static Calendar nextMinute(int minutes) {
    return nextMinute(Calendar.getInstance(), minutes);
  }

  public static Calendar nextMinute(Calendar now) {
    return nextMinute(now, 1);
  }

  public static Calendar nextMinute(Calendar now, int minutes) {
    Calendar then = (Calendar)now.clone();
    then.set(Calendar.SECOND, 0);
    then.set(Calendar.MILLISECOND, 0);
    then.add(Calendar.MINUTE, minutes);
    return then;
  }

  public static long nextMinuteDelay() {
    Calendar now = Calendar.getInstance();
    Calendar then = nextMinute(now);
    return then.getTimeInMillis() - now.getTimeInMillis();
  }

  public static String until(Calendar alarm) {
    Calendar now = Calendar.getInstance();
    now.set(Calendar.SECOND, 0);
    now.set(Calendar.MILLISECOND, 0);
    return until(now, alarm);
  }

  public static String until(Calendar from, Calendar to) {
    long minutes = (to.getTimeInMillis() - from.getTimeInMillis()) / 1000 / 60;
    long days = minutes / 1440;
    minutes -= (days * 1440);
    long hours = minutes / 60;
    minutes -= (hours * 60);

    String s = "";
    if (days > 0)
      // TODO: string
      s += String.format("%d %s ", days, (days > 1) ? "days" : "day");
    if (hours > 0)
      // TODO: string
      s += String.format("%d %s ", hours, (hours > 1) ? "hours" : "hour");
    if (minutes > 0)
      // TODO: string
      s += String.format("%d %s", minutes, (minutes > 1) ? "minutes" : "minute");
    return s;
  }

  public static String format(Context context, Calendar c) {
    SimpleDateFormat f = DateFormat.is24HourFormat(context) ?
      new SimpleDateFormat("HH:mm") :
      new SimpleDateFormat("h:mm");
    return f.format(c.getTime());
  }

  public static String formatLong(Context context, Calendar c) {
    SimpleDateFormat f = DateFormat.is24HourFormat(context) ?
      new SimpleDateFormat("HH:mm") :
      new SimpleDateFormat("h:mm a");
    return f.format(c.getTime());
  }
}
