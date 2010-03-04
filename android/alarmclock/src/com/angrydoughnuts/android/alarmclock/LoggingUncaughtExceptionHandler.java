package com.angrydoughnuts.android.alarmclock;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;
import java.text.SimpleDateFormat;
import java.util.Calendar;

// NOTE: Mostly lifted from: http://stackoverflow.com/questions/601503/how-do-i-obtain-crash-data-from-my-android-application
public class LoggingUncaughtExceptionHandler implements UncaughtExceptionHandler {
  private String directory;
  private UncaughtExceptionHandler defaultHandler;

  public LoggingUncaughtExceptionHandler(String directory) {
    this.directory = directory;
    this .defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
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
