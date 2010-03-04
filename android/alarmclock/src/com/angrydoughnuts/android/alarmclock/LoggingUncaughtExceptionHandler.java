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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * An exception handler that writes the stact trace to a file on the
 * device's SD card.  Make sure that the WRITE_EXTERNAL_STORAGE permission
 * is available before using his class.
 * NOTE: Mostly lifted from:
 * http://stackoverflow.com/questions/601503/how-do-i-obtain-crash-data-from-my-android-application
 */ 
public final class LoggingUncaughtExceptionHandler implements UncaughtExceptionHandler {
  private String directory;
  private UncaughtExceptionHandler defaultHandler;

  public LoggingUncaughtExceptionHandler(String directory) {
    this.directory = directory;
    this.defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
  }

  @Override
  public void uncaughtException(Thread thread, Throwable ex) {
    try {
      String timestamp = new SimpleDateFormat("yyyyMMdd_kkmmss.SSSS").format(Calendar.getInstance().getTime());
      String filename = timestamp + "-alarmclock.txt";

      Writer stacktrace = new StringWriter();
      ex.printStackTrace(new PrintWriter(stacktrace));

      BufferedWriter bos = new BufferedWriter(new FileWriter(directory + "/" + filename));
      bos.write(stacktrace.toString());
      bos.flush();
      bos.close();
      stacktrace.close();
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      defaultHandler.uncaughtException(thread, ex);
    }
  }
}
