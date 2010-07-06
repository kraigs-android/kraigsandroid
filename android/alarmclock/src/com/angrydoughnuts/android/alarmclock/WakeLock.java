package com.angrydoughnuts.android.alarmclock;

import java.util.TreeMap;

import android.content.Context;
import android.os.PowerManager;

public class WakeLock {
  public static class WakeLockException extends Exception {
    private static final long serialVersionUID = 1L;
    public WakeLockException(String e) {
      super(e);
    }
  }

  private static final TreeMap<Long, PowerManager.WakeLock> wakeLocks =
    new TreeMap<Long, PowerManager.WakeLock>();

  public static final void acquire(Context context, long alarmId) throws WakeLockException {
    if (wakeLocks.containsKey(alarmId)) {
      throw new WakeLockException("Multiple acquisitions of wake lock for id: " + alarmId);
    }

    PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
    PowerManager.WakeLock wakeLock = powerManager.newWakeLock(
        PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
        "Alarm Notification Wake Lock id " + alarmId);
    wakeLock.setReferenceCounted(false);
    wakeLock.acquire();

    wakeLocks.put(alarmId, wakeLock);
  }

  public static final void assertHeld(long alarmId) throws WakeLockException {
    PowerManager.WakeLock wakeLock = wakeLocks.get(alarmId);
    if (wakeLock == null || !wakeLock.isHeld()) {
      throw new WakeLockException("Wake lock not held for alarm id: " + alarmId);
    }
  }

  public static final void assertAtLeastOneHeld() throws WakeLockException {
    for (PowerManager.WakeLock wakeLock : wakeLocks.values()) {
      if (wakeLock.isHeld()) {
        return;
      }
    }
    throw new WakeLockException("No wake locks are held.");
  }

  public static final void assertNoneHeld() throws WakeLockException {
    for (PowerManager.WakeLock wakeLock : wakeLocks.values()) {
      if (wakeLock.isHeld()) {
        throw new WakeLockException("No wake locks are held.");
      }
    }
  }

  public static final void release(long alarmId) throws WakeLockException {
    assertHeld(alarmId);
    PowerManager.WakeLock wakeLock = wakeLocks.remove(alarmId);
    wakeLock.release();
  }
}
