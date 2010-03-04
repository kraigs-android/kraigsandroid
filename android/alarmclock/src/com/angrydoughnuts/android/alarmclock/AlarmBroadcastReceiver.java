package com.angrydoughnuts.android.alarmclock;

import com.angrydoughnuts.android.alarmclock.AlarmNotificationActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.PowerManager;


public class AlarmBroadcastReceiver extends BroadcastReceiver {

  // TODO(cgallek):  I don't think this is safe to do.  The Broadcast receiver can
  // be destroyed as soon as onReceive completes.  But I don't know of any other
  // way to pass the lock onto the notification activity...
  static private PowerManager.WakeLock wakeLock;
  static public PowerManager.WakeLock wakeLock() { return wakeLock; }

  @Override
  public void onReceive(Context context, Intent recvIntent) {
    Uri alarmUri = recvIntent.getData();

    if (wakeLock == null) {
      PowerManager powerManager =
        (PowerManager) context.getSystemService(Context.POWER_SERVICE);
      wakeLock = powerManager.newWakeLock(
          PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
          "Alarm Notification Wake Lock");
      wakeLock.setReferenceCounted(true);
    }
    wakeLock.acquire();

    Intent notifyIntent = new Intent(context, AlarmNotificationActivity.class);
    notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    notifyIntent.setData(alarmUri);

    context.startActivity(notifyIntent);
  }

}
