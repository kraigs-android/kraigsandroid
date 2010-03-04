package com.angrydoughnuts.android.alarmclock;

import java.util.TimerTask;

import android.content.Context;
import android.widget.Toast;

public class AlarmClockTimerTask extends TimerTask {

  private Context context;

  public AlarmClockTimerTask(Context context) {
    this.context = context;
  }

  @Override
  public void run() {
    Toast.makeText(context, "FIRE ALARM!", Toast.LENGTH_SHORT).show();
  }
}
