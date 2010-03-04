package com.angrydoughnuts.android.alarmclock;

import java.util.Calendar;

public class TimeUtil {

  public static int minutesAfterMidnight(int hour, int minute, int second) {
    // TODO(cgallek): actually using seconds since midnight right now for testing.
    // Change back to minutes.
    return hour * 3600 + minute * 60 + second;
  }

  public static Calendar minutesAfterMidnightToLocalCalendar(int minutesAfterMidnight) {
    // TODO(cgallek): this is actually interpreted as seconds after midnight right
    // now (for testing).  Switch it to minutes eventually.
    int hour = minutesAfterMidnight % 3600;
    int minutes = (minutesAfterMidnight - (hour * 3600)) % 60;
    int seconds = (minutesAfterMidnight- (hour * 3600 + minutes * 60));
    Calendar calendar = Calendar.getInstance();
    calendar.set(Calendar.HOUR_OF_DAY, hour);
    calendar.set(Calendar.MINUTE, minutes);
    calendar.set(Calendar.SECOND, seconds);
    return calendar;
  }

  public static String minutesAfterMidnightToString(int minutesAfterMidnight) {
    Calendar c = minutesAfterMidnightToLocalCalendar(minutesAfterMidnight);
    return String.format("%02d", c.get(Calendar.HOUR_OF_DAY)) + ":" 
      + String.format("%02d", c.get(Calendar.MINUTE)) + ":" 
      + String.format("%02d", c.get(Calendar.SECOND));
  }

  public static long nextLocalOccuranceInUTC(int minutesAfterMidnight) {
    Calendar c = minutesAfterMidnightToLocalCalendar(minutesAfterMidnight);
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
