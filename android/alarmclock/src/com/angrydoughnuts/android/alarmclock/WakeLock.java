package com.angrydoughnuts.android.alarmclock;

import java.util.TreeMap;

import android.content.Context;
import android.os.PowerManager;

public class WakeLock {
  private static final TreeMap<Long, PowerManager.WakeLock> wakeLocks =
    new TreeMap<Long, PowerManager.WakeLock>();

  public static final void acquire(Context context, long alarmId) {
    if (wakeLocks.containsKey(alarmId)) {
      throw new IllegalStateException("Multiple acquisitions of wake lock for id: " + alarmId);
    }

    PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
    PowerManager.WakeLock wakeLock = powerManager.newWakeLock(
        PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
        "Alarm Notification Wake Lock id " + alarmId);
    wakeLock.setReferenceCounted(false);
    wakeLock.acquire();

    wakeLocks.put(alarmId, wakeLock);
  }

  public static final void assertHeld(long alarmId) {
    PowerManager.WakeLock wakeLock = wakeLocks.get(alarmId);
    if (wakeLock == null || !wakeLock.isHeld()) {
      throw new IllegalStateException("Wake lock not held for alarm id: " + alarmId);
    }
  }

  public static final void release(long alarmId) {
    assertHeld(alarmId);
    PowerManager.WakeLock wakeLock = wakeLocks.remove(alarmId);
    wakeLock.release();
  }
}
