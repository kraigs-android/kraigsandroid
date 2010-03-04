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

import android.net.Uri;

public final class AlarmUtil {
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
