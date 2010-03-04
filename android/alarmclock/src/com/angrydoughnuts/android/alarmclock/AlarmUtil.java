package com.angrydoughnuts.android.alarmclock;

import android.net.Uri;

public class AlarmUtil {
  public static Uri alarmIdToUri(long alarmId) {
    return Uri.parse("alarm_id:" + alarmId);
  }

  public static long alarmUriToId(Uri uri) {
    return Long.parseLong(uri.getSchemeSpecificPart());
  }
}
