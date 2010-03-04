package com.angrydoughnuts.android.alarmclock;

import com.angrydoughnuts.android.alarmclock.ActivityAlarmNotification;

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

    Intent notifyIntent = new Intent(context, ActivityAlarmNotification.class);
    notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    notifyIntent.setData(alarmUri);

    context.startActivity(notifyIntent);
  }
}
