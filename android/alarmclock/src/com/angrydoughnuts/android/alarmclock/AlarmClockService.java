package com.angrydoughnuts.android.alarmclock;

import java.util.Timer;
import java.util.TreeMap;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;

public class AlarmClockService extends Service {
  private final int NOTIFICATION_ID = 1;

  private Timer timerThread;
  private Handler uiHandler;

  @Override
  public void onStart(Intent intent, int startId) {
    super.onStart(intent, startId);
  }

  @Override
  public void onCreate() {
    super.onCreate();
    timerThread = new Timer();
    // TODO(cgallek): This must be created in the main thread. Is there a way
    // to ensure this?
    uiHandler = new Handler();

    final NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

    // TODO(cgallek): add a better notification icon.
    Notification notification = new Notification(R.drawable.icon, null, 0);
    notification.flags |= Notification.FLAG_ONGOING_EVENT;

    // Make the notification launch the UI Activity when clicked.
    Intent notificationIntent = new Intent(this, AlarmClockActivity.class);
    PendingIntent launch = PendingIntent.getActivity(this, 0,
        notificationIntent, 0);
    // TODO(cgallek): fill in the 'next alarm in/at' text.
    notification.setLatestEventInfo(getApplicationContext(), "Alarm Clock",
        "Next Alarm in ...", launch);

    manager.notify(NOTIFICATION_ID, notification);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    timerThread.cancel(); // Just in case OnUnbind was never called.

    final NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    manager.cancel(NOTIFICATION_ID);
  }

  @Override
  public IBinder onBind(Intent intent) {
    // Explicitly start this service when a client binds. This will cause
    // the service to outlive all of its binders.
    final Intent selfIntent = new Intent(getApplication(),
        AlarmClockService.class);
    startService(selfIntent);

    return new AlarmClockInterfaceStub(getApplicationContext(), this);
  }

  @Override
  public boolean onUnbind(Intent intent) {
    // Decide if we need to explicitly shut down this service. Normally,
    // the service would shutdown after the last un-bind. However, since
    // we explicitly started the service in onBind (to remain persistent),
    // we must explicitly stop here when there are no alarms set.
    if (alarmCount() == 0) {
      timerThread.cancel();
      stopSelf();
      return false;
    } else {
      // Since we want the service to continue running in this case, return
      // true so that onRebind is called instead of onBind.
      return true;

    }
  }

  // TODO(cgallek): replace this with a data provider.
  private static int taskId = 0;

  private static int nextTaskId() {
    return taskId++;
  }

  private TreeMap<Integer, AlarmClockTimerTask> taskList = new TreeMap<Integer, AlarmClockTimerTask>();

  public int alarmCount() {
    return taskList.size();
  }

  public void scheduleAlarmIn(int seconds) {
    int id = nextTaskId();
    AlarmClockTimerTask task = new AlarmClockTimerTask(getApplicationContext(),
        uiHandler, id);
    // TODO(cgallek): make sure ID doesn't exist yet?
    taskList.put(id, task);
    timerThread.schedule(task, seconds * 1000);
  }

  public boolean acknowledgeAlarm(int alarmId) {
    AlarmClockTimerTask task = taskList.remove(alarmId);
    if (task != null) {
      task.cancel();
      return true;
    } else {
      return false;
    }
  }

  public void clearAllAlarms() {
    for (AlarmClockTimerTask task : taskList.values()) {
      task.cancel();
    }
    taskList.clear();
  }

  public void notifyDialog(int alarmId) {
    Intent notifyIntent = new Intent(getApplicationContext(),
        AlarmNotificationActivity.class);
    notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    notifyIntent.putExtra("task_id", alarmId);

    // TODO(cgallek) Currently, both this service and the Notification
    // Activity manage power settings. It might make sense to move all
    // power management into the service. This would require a callback
    // from the Notification application. I'm not sure how to get a
    // response from an activity started from a service...
    // I think there also might be a race condition between the
    // startActivity call and the wake lock release call below (ie if
    // the lock is released before the activity actually starts). Moving
    // all locking to this service would also fix that problem.
    PowerManager manager = (PowerManager) getSystemService(Context.POWER_SERVICE);
    PowerManager.WakeLock wakeLock = manager.newWakeLock(
        PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
        "Alarm Service CPU wake lock");
    wakeLock.acquire();

    startActivity(notifyIntent);

    wakeLock.release();
  }
}
