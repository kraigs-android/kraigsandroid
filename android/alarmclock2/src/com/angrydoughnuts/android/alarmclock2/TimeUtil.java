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
  public static Calendar nextOccurrence(int secondsPastMidnight) {
    return nextOccurrence(Calendar.getInstance(), secondsPastMidnight);
  }

  public static Calendar nextOccurrence(Calendar now, int secondsPastMidnight) {
    Calendar then = (Calendar)now.clone();
    then.set(Calendar.HOUR_OF_DAY, 0);
    then.set(Calendar.MINUTE, 0);
    then.set(Calendar.SECOND, 0);
    then.set(Calendar.MILLISECOND, 0);
    then.add(Calendar.SECOND, secondsPastMidnight);
    if (then.before(now))
      then.add(Calendar.DATE, 1);
    return then;
  }

  public static Calendar nextMinute() {
    return nextMinute(Calendar.getInstance());
  }

  public static Calendar nextMinute(Calendar now) {
    Calendar then = (Calendar)now.clone();
    then.set(Calendar.SECOND, 0);
    then.set(Calendar.MILLISECOND, 0);
    then.add(Calendar.MINUTE, 1);
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
    long hours = minutes / 60;
    minutes -= (hours * 60);

    if (hours > 0)
      return String.format(
          "%d %s %d %s",
          hours, (hours > 1) ? "hours" : "hour",
          minutes, (minutes > 1) ? "minutes" : "minute");
    else
      return String.format(
          "%d %s", minutes, (minutes > 1) ? "minutes" : "minute");
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
