package com.angrydoughnuts.android.alarmclock;

import android.net.Uri;

public class AlarmUtil {
  static public Uri alarmIdToUri(long alarmId) {
    return Uri.parse("alarm_id:" + alarmId);
  }

  static public long alarmUriToId(Uri uri) {
    return Long.parseLong(uri.getSchemeSpecificPart());
  }

  enum Interval {
    SECOND(1000), MINUTE(60 * 1000), HOUR(60 * 60 * 1000);
    private long millis;
    public long millis() { return millis; }
    Interval(long millis) {
      this.millis = millis;
    }
  }

  static public long millisTillNextInterval(Interval interval) {
    long now = System.currentTimeMillis();
    return interval.millis() - now % interval.millis();
  }

  static public long nextIntervalInUTC(Interval interval) {
    long now = System.currentTimeMillis();
    return now + interval.millis() - now % interval.millis();
  }
}
