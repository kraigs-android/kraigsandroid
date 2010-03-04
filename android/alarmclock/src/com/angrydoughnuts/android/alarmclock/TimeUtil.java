package com.angrydoughnuts.android.alarmclock;

import java.util.Calendar;

public class TimeUtil {

  public static int secondsAfterMidnight(Calendar calendar) {
    return calendar.get(Calendar.HOUR_OF_DAY) * 3600 +
      calendar.get(Calendar.MINUTE) * 60 +
      calendar.get(Calendar.SECOND);
  }

  // TODO(cgallek): This method isn't really needed.  It's used to format
  // The db representation as a string.  Fold this directly into the toString
  // method below.
  public static Calendar secondsAfterMidnightToLocalCalendar(int secondsAfterMidnight) {
    int hour = secondsAfterMidnight % 3600;
    int minutes = (secondsAfterMidnight - (hour * 3600)) % 60;
    int seconds = (secondsAfterMidnight- (hour * 3600 + minutes * 60));
    Calendar calendar = Calendar.getInstance();
    calendar.set(Calendar.HOUR_OF_DAY, hour);
    calendar.set(Calendar.MINUTE, minutes);
    calendar.set(Calendar.SECOND, seconds);
    return calendar;
  }

  public static String calendarToClockString(Calendar c) {
    return String.format("%02d", c.get(Calendar.HOUR_OF_DAY)) + ":" 
      + String.format("%02d", c.get(Calendar.MINUTE)) + ":" 
      + String.format("%02d", c.get(Calendar.SECOND));
  }

  // TODO(cgallek): make an object to manage the concept of 'secondsAfterMidnight'
  // and return that from DbAccessor.alarmTime instead.
  public static long nextLocalOccuranceInUTC(int secondsAfterMidnight) {
    Calendar c = secondsAfterMidnightToLocalCalendar(secondsAfterMidnight);
    Calendar now = Calendar.getInstance();
    if (c.before(now)) {
      c.add(Calendar.DATE, 1);
    }
    return c.getTimeInMillis();
  }

  public static long snoozeInUTC(int minutes) {
    Calendar now = Calendar.getInstance();
    now.set(Calendar.SECOND, 0);
    now.add(Calendar.MINUTE, minutes);
    return now.getTimeInMillis();
  }
}
