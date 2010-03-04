package com.angrydoughnuts.android.alarmclock;

import java.util.TimerTask;

import android.content.Context;
import android.os.Handler;

public class AlarmClockTimerTask extends TimerTask {

  private Handler handler;
  int alarmId;
  private AlarmClockServiceBinder service;

  final private Runnable work = new Runnable() {
    @Override
    public void run() {
      // TODO(cgallek): This currently re-binds to the service on every
      // run.  Figure out how to reference count threads and only
      // bind as necessary in the Timer thread.
      service.bind();

      service.notifyDialog(alarmId);

      service.unbind();
    }
  };

  public AlarmClockTimerTask(Context context, Handler handler, int alarmId) {
    this.handler = handler;
    this.alarmId = alarmId;
    this.service = AlarmClockServiceBinder.newBinder(context);
  }

  @Override
  public void run() {
    handler.post(work);
  }

  @Override
  public boolean cancel() {
    handler.removeCallbacks(work);
    return super.cancel();
  }
}
