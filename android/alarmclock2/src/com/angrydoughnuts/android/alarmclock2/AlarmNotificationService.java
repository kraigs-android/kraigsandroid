/****************************************************************************
 * Copyright 2016 kraigs.android@gmail.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ****************************************************************************/

package com.angrydoughnuts.android.alarmclock2;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.IBinder;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.ArrayMap;
import android.util.Log;

import java.io.IOException;
import java.lang.Math;
import java.util.HashSet;
import java.util.Calendar;
import java.util.TimeZone;

public class AlarmNotificationService extends Service {
  public static final String ALARM_ID = "alarm_id";
  private static final String TIME_UTC = "time_utc";
  public static final long DEFAULTS_ALARM_ID = Long.MAX_VALUE;

  public static long newAlarm(Context c, int secondsPastMidnight) {

    ContentValues v = new ContentValues();
    v.put(AlarmClockProvider.AlarmEntry.TIME, secondsPastMidnight);
    Uri u = c.getContentResolver().insert(AlarmClockProvider.ALARMS_URI, v);
    long alarmid = ContentUris.parseId(u);
    Log.i(TAG, "New alarm: " + alarmid + " (" + u +")");

    // Inserted entry is ENABLED by default with no options.  Schedule the
    // first occurrence.
    Calendar ts = TimeUtil.nextOccurrence(secondsPastMidnight, 0);
    scheduleAlarmTrigger(c, alarmid, ts.getTimeInMillis());

    return alarmid;
  }

  public static void scheduleAlarmTrigger(
      Context c, long alarmid, long tsUTC) {
    c.startService(new Intent(c, AlarmNotificationService.class)
                   .putExtra(COMMAND, SCHEDULE_TRIGGER)
                   .putExtra(ALARM_ID, alarmid)
                   .putExtra(TIME_UTC, tsUTC));
  }

  public static void removeAlarmTrigger(Context c, long alarmid) {
    c.startService(new Intent(c, AlarmNotificationService.class)
                   .putExtra(COMMAND, REMOVE_TRIGGER)
                   .putExtra(ALARM_ID, alarmid));
  }

  public static void dismissAllAlarms(Context c) {
    c.startService(new Intent(c, AlarmNotificationService.class)
                   .putExtra(COMMAND, DISMISS_ALL));
  }

  public static void snoozeAllAlarms(Context c, long snoozeUtc) {
    c.startService(new Intent(c, AlarmNotificationService.class)
                   .putExtra(COMMAND, SNOOZE_ALL)
                   .putExtra(TIME_UTC, snoozeUtc));
  }

  private ActiveAlarms activeAlarms = null;

  @Override
  public int onStartCommand(Intent i, int flags, int startId) {
    long alarmid;
    long ts;

    switch (i.hasExtra(COMMAND) ? i.getExtras().getInt(COMMAND) : -1) {
    case TRIGGER_ALARM_NOTIFICATION:
      handleTriggerAlarm(i);
      return START_NOT_STICKY;
    case DISMISS_ALL:
      dismissAll();
      stopSelf();
      return START_NOT_STICKY;
    case SNOOZE_ALL:
      ts = i.getLongExtra(TIME_UTC, -1);
      snoozeAll(ts);
      stopSelf();
      return START_NOT_STICKY;
    case SCHEDULE_TRIGGER:
      alarmid = i.getLongExtra(ALARM_ID, -1);
      ts = i.getLongExtra(TIME_UTC, -1);
      scheduleTrigger(alarmid, ts);
      break;
    case REMOVE_TRIGGER:
      alarmid = i.getLongExtra(ALARM_ID, -1);
      removeTrigger(alarmid);
      break;
    case REFRESH:
      refreshNotifyBar();
      break;
    }

    if (activeAlarms == null)
      stopSelf(startId);

    return START_NOT_STICKY;
  }

  @Override
  public void onDestroy() {
    if (activeAlarms == null)
      return;

    activeAlarms.release();
    activeAlarms = null;
  }

  @Override
  public IBinder onBind(Intent intent) { return null; }

  private void handleTriggerAlarm(Intent i) {
    final long alarmid = i.getLongExtra(ALARM_ID, -1);
    final AlarmOptions.OptionalSettings settings =
      AlarmOptions.OptionalSettings.get(getApplicationContext(), alarmid);

    PowerManager.WakeLock w = null;
    if (i.hasExtra(AlarmTriggerReceiver.WAKELOCK_ID)) {
      w = AlarmTriggerReceiver.consumeLock(
          i.getExtras().getInt(AlarmTriggerReceiver.WAKELOCK_ID));
    }

    if (w == null)
      Log.e(TAG, "No wake lock present for TRIGGER_ALARM_NOTIFICATION");

    if (activeAlarms == null) {
      activeAlarms = new ActiveAlarms(getApplicationContext(), w, settings);
    } else {
      Log.i(TAG, "Already wake-locked, releasing extra lock");
      w.release();
    }
    activeAlarms.alarmids.add(alarmid);

    String labels = "";
    for (long id : activeAlarms.alarmids) {
      String label = AlarmOptions.AlarmSettings.getLabel(getApplicationContext(), id);
      if (!label.isEmpty()) {
        if (labels.isEmpty())
          labels = label;
        else
          labels += ", " + label;
      }
    }

    Intent notify = new Intent(this, AlarmNotificationActivity.class)
      .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

    final Notification notification =
      new Notification.Builder(this)
      .setContentTitle(labels.isEmpty() ? "Alarm Klock" : labels)
      .setContentText("Second line...")
      .setSmallIcon(R.drawable.ic_alarm_on)
      .setContentIntent(PendingIntent.getActivity(this, 0, notify, 0))
      .setCategory(Notification.CATEGORY_ALARM)
      .setPriority(Notification.PRIORITY_MAX)
      .setVisibility(Notification.VISIBILITY_PUBLIC)
      .setOngoing(true)
      .setLights(Color.WHITE, 1000, 1000)
      .setVibrate(settings.vibrate ? new long[] {1000, 1000} : null)
      .build();
    notification.flags |= Notification.FLAG_INSISTENT;  // Loop sound/vib/blink
    startForeground(FIRING_ALARM_NOTIFICATION_ID, notification);

    refreshNotifyBar();

    Intent notifyAct = (Intent) notify.clone();
    notifyAct.putExtra(ALARM_ID, alarmid);
    startActivity(notifyAct);
  }

  private void dismissAll() {
    if (activeAlarms == null) {
      Log.w(TAG, "No active alarms when dismissed");
      return;
    }

    for (long alarmid : activeAlarms.alarmids) {
      final int repeat = AlarmOptions.AlarmSettings.getRepeat(this, alarmid);
      ContentValues v = new ContentValues();
      v.put(AlarmClockProvider.AlarmEntry.NEXT_SNOOZE, 0);
      if (repeat == 0)
        v.put(AlarmClockProvider.AlarmEntry.ENABLED, false);
      int r = getContentResolver().update(
          ContentUris.withAppendedId(AlarmClockProvider.ALARMS_URI, alarmid),
          v, null, null);
      if (r < 1) {
        Log.e(TAG, "Failed to disable " + alarmid);
      }
      if (repeat != 0) {
        final long nextUTC = TimeUtil.nextOccurrence(
            AlarmOptions.AlarmSettings.getTime(this, alarmid), repeat, 0)
          .getTimeInMillis();
        AlarmNotificationService.scheduleAlarmTrigger(this, alarmid, nextUTC);
      }
    }

    activeAlarms.alarmids.clear();
    refreshNotifyBar();
  }

  private void snoozeAll(long snoozeUtc) {
    if (activeAlarms == null) {
      Log.w(TAG, "No active alarms when snoozed");
      return;
    }

    for (long alarmid : activeAlarms.alarmids) {
      ContentValues v = new ContentValues();
      v.put(AlarmClockProvider.AlarmEntry.NEXT_SNOOZE, snoozeUtc);
      int r = getContentResolver().update(
          ContentUris.withAppendedId(AlarmClockProvider.ALARMS_URI, alarmid),
          v, null, null);
      if (r < 1) {
        Log.e(TAG, "Failed to snooze " + alarmid);
      }
      scheduleTrigger(alarmid, snoozeUtc);
    }

    activeAlarms.alarmids.clear();
    refreshNotifyBar();
  }

  private void scheduleTrigger(long alarmid, long tsUTC) {
    // Intents are considered equal if they have the same action, data, type,
    // class, and categories.  In order to schedule multiple alarms, every
    // pending intent must be different.  This means that we must encode
    // the alarm id in the request code.
    PendingIntent schedule = PendingIntent.getBroadcast(
        this, (int)alarmid, new Intent(this, AlarmTriggerReceiver.class)
        .putExtra(ALARM_ID, alarmid), 0);

    ((AlarmManager)getSystemService(Context.ALARM_SERVICE))
        .setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, tsUTC, schedule);
    refreshNotifyBar();
  }

  private void removeTrigger(long alarmid) {
    PendingIntent schedule = PendingIntent.getBroadcast(
        this, (int)alarmid, new Intent(this, AlarmTriggerReceiver.class)
        .putExtra(ALARM_ID, alarmid), 0);

    ((AlarmManager)getSystemService(Context.ALARM_SERVICE)).cancel(schedule);
    refreshNotifyBar();
  }

  private void refreshNotifyBar() {
    final NotificationManager manager =
      (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    final PendingIntent tick = PendingIntent.getService(
        this, 0, new Intent(this, AlarmNotificationService.class)
        .putExtra(AlarmNotificationService.COMMAND,
                  AlarmNotificationService.REFRESH), 0);

    final Cursor c = getContentResolver().query(
        AlarmClockProvider.ALARMS_URI,
        new String[] { AlarmClockProvider.AlarmEntry.TIME,
                       AlarmClockProvider.AlarmEntry.NAME,
                       AlarmClockProvider.AlarmEntry.DAY_OF_WEEK,
                       AlarmClockProvider.AlarmEntry.NEXT_SNOOZE },
        AlarmClockProvider.AlarmEntry.ENABLED + " == 1",
        null, null);

    if (c.getCount() == 0 ||
        (activeAlarms != null && !activeAlarms.alarmids.isEmpty())) {
      manager.cancel(NEXT_ALARM_NOTIFICATION_ID);
      c.close();
      ((AlarmManager)getSystemService(Context.ALARM_SERVICE)).cancel(tick);
      return;
    }

    final Calendar now = Calendar.getInstance();
    Calendar next = null;
    String next_label = "";
    while (c.moveToNext()) {
      Calendar n = TimeUtil.nextOccurrence(
          now,
          c.getInt(c.getColumnIndex(AlarmClockProvider.AlarmEntry.TIME)),
          c.getLong(c.getColumnIndex(AlarmClockProvider.AlarmEntry.NEXT_SNOOZE)),
          c.getInt(c.getColumnIndex(AlarmClockProvider.AlarmEntry.DAY_OF_WEEK)));
      if (next == null || n.before(next)) {
        next = n;
        next_label = c.getString(c.getColumnIndex(AlarmClockProvider.AlarmEntry.NAME));
      }
    }
    c.close();

    manager.notify(
        NEXT_ALARM_NOTIFICATION_ID,
        new Notification.Builder(this)
        .setContentTitle(next_label.isEmpty() ? "Alarm Klock" : next_label)
        .setContentText(TimeUtil.formatLong(this, next) + " in " + TimeUtil.until(next))
        .setSmallIcon(R.drawable.ic_alarm)
        .setCategory(Notification.CATEGORY_STATUS)
        .setVisibility(Notification.VISIBILITY_PUBLIC)
        .setOngoing(true)
        .setContentIntent(
            PendingIntent.getActivity(
                this, 0, new Intent(this, AlarmClockActivity.class), 0))
        .build());

    final Calendar wake = TimeUtil.nextMinute();
    ((AlarmManager)getSystemService(Context.ALARM_SERVICE)).setExact(
        AlarmManager.RTC, wake.getTimeInMillis(), tick);
  }

  private static final String TAG =
    AlarmNotificationService.class.getSimpleName();
  // Commands
  private static final String COMMAND = "command";
  private static final int TRIGGER_ALARM_NOTIFICATION = 1;
  private static final int SCHEDULE_TRIGGER = 2;
  private static final int REMOVE_TRIGGER = 3;
  private static final int DISMISS_ALL = 4;
  private static final int SNOOZE_ALL = 5;
  private static final int REFRESH = 6;
  // Notification ids
  private static final int FIRING_ALARM_NOTIFICATION_ID = 42;
  private static final int NEXT_ALARM_NOTIFICATION_ID = 69;

  private static class ActiveAlarms {
    public static final int TRIGGER_INC = 1;
    public static final int RESET_VOLUME = 2;

    private PowerManager.WakeLock wakelock = null;
    private HashSet<Long> alarmids = new HashSet<Long>();
    private MediaPlayer player = null;
    private Handler handler = null;
    private Runnable timeout = null;

    public ActiveAlarms(final Context c, PowerManager.WakeLock w,
                        final AlarmOptions.OptionalSettings s) {
      final AudioManager a = (AudioManager)c.getSystemService(
          Context.AUDIO_SERVICE);
      final int init_volume = a.getStreamVolume(AudioManager.STREAM_ALARM);
      a.setStreamVolume(AudioManager.STREAM_ALARM,
                        a.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0);

      wakelock = w;
      player = new MediaPlayer();
      handler = new Handler() {
          @Override
          public void handleMessage(Message m) {
            switch (m.what) {
            case TRIGGER_INC:
              int inc = (s.volume_ending - s.volume_starting) / s.volume_time;
              int next = Math.min(s.volume_ending, m.arg1 + inc);
              float norm = (float)((Math.pow(5, next/100.0)-1)/4);
              Log.i(TAG, "Incrementing volume to " + norm);
              player.setVolume(norm, norm);
              if (next < s.volume_ending) {
                Message m2 = new Message();
                m2.what = TRIGGER_INC;
                m2.arg1 = next;
                sendMessageDelayed(m2, 1000);
              }
              break;
            case RESET_VOLUME:
              a.setStreamVolume(AudioManager.STREAM_ALARM, init_volume, 0);
              break;
            }
          }
        };
      timeout = new Runnable() {
          @Override
          public void run() {
            Log.w(TAG, "Alarm timeout");
            AlarmNotificationService.dismissAllAlarms(c);
            Intent timeout = new Intent(c, AlarmNotificationActivity.class)
              .putExtra(AlarmNotificationActivity.TIMEOUT, true)
              .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(timeout);

          }
        };
      handler.postDelayed(timeout, 10 * 60 * 1000);

      player.setAudioStreamType(AudioManager.STREAM_ALARM);
      player.setLooping(true);
      final float start = s.volume_starting/(float)100.0;
      player.setVolume(start, start);
      Log.i(TAG, "Starting volume: " + start);

      try {
        player.setDataSource(c, s.tone_url);
      } catch (IOException e) {
        Log.e(TAG, "Failed loading tone: " + e.toString());
        try {
          player.setDataSource(c, Settings.System.DEFAULT_NOTIFICATION_URI);
        } catch (IOException e2) {
          Log.e(TAG, "Failed loading backup tone: " + e2.toString());
        }
      }

      try {
        player.prepare();
        player.start();
      } catch (IOException | IllegalStateException e) {
        Log.e(TAG, "prepare failed: " + e.toString());
      }

      Message m = new Message();
      m.what = TRIGGER_INC;
      m.arg1 = s.volume_starting;
      handler.sendMessage(m);
    }

    public void release() {
      if (!alarmids.isEmpty())
        Log.w(TAG, "Releasing wake lock with active alarms! (" +
              alarmids.size() + ")");
      Log.i(TAG, "Releasing wake lock");
      wakelock.release();
      wakelock = null;

      handler.removeCallbacks(timeout);
      handler.sendEmptyMessage(ActiveAlarms.RESET_VOLUME);
      handler.removeMessages(ActiveAlarms.TRIGGER_INC);
      handler = null;

      if (player.isPlaying())
        player.stop();
      player.reset();
      player.release();
      player = null;
    }
  }

  public static class AlarmTriggerReceiver extends BroadcastReceiver {
    public static final String WAKELOCK_ID = "wakelock_id";
    private static final ArrayMap<Integer, PowerManager.WakeLock> locks =
      new ArrayMap<Integer, PowerManager.WakeLock>();
    private static int nextid = 0;

    @Override
    public void onReceive(Context c, Intent i) {
      final long alarmid = i.getLongExtra(ALARM_ID, -1);

      @SuppressWarnings("deprecation")  // SCREEN_DIM_WAKE_LOCK
      PowerManager.WakeLock w =
        ((PowerManager)c.getSystemService(Context.POWER_SERVICE))
        .newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK |
                     PowerManager.ACQUIRE_CAUSES_WAKEUP, "wake id " + nextid);
      w.setReferenceCounted(false);
      w.acquire();
      locks.put(nextid, w);
      Log.i(TAG, "Acquired lock " + nextid + " for alarm " + alarmid);

      c.startService(new Intent(c, AlarmNotificationService.class)
                     .putExtra(ALARM_ID, alarmid)
                     .putExtra(COMMAND, TRIGGER_ALARM_NOTIFICATION)
                     .putExtra(WAKELOCK_ID, nextid++));
    }

    public static PowerManager.WakeLock consumeLock(int id) {
      return locks.remove(id);
    }
  }
}
