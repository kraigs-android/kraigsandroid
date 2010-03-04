package com.angrydoughnuts.android.alarmclock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class DeviceBootReceiver extends BroadcastReceiver {

  @Override
  public void onReceive(Context context, Intent intent) {
    Intent i = new Intent(context, AlarmClockService.class);
    i.putExtra(AlarmClockService.COMMAND_EXTRA, AlarmClockService.COMMAND_DEVICE_BOOT);
    context.startService(i);
  }

}
