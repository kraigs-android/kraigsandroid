package com.angrydoughnuts.android.alarmclock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class RecevierTimeZoneChange extends BroadcastReceiver {

  @Override
  public void onReceive(Context context, Intent intent) {
    Intent i = new Intent(context, AlarmClockService.class);
    i.putExtra(AlarmClockService.COMMAND_EXTRA, AlarmClockService.COMMAND_TIMEZONE_CHANGE);
    context.startService(i);
  }

}
