package com.angrydoughnuts.android.alarmclock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;


public class ReceiverAlarm extends BroadcastReceiver {
  @Override
  public void onReceive(Context context, Intent recvIntent) {
    Uri alarmUri = recvIntent.getData();
    long alarmId = AlarmUtil.alarmUriToId(alarmUri);

    WakeLock.acquire(context, alarmId);

    Intent notifyService = new Intent(context, NotificationService.class);
    notifyService.setData(alarmUri);

    context.startService(notifyService);
  }
}
