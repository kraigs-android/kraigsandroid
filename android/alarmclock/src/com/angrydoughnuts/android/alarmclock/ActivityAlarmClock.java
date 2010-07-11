/****************************************************************************
 * Copyright 2010 kraigs.android@gmail.com
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

package com.angrydoughnuts.android.alarmclock;

import java.util.Calendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

/**
 * This is the main Activity for the application.  It contains a ListView
 * for displaying all alarms, a simple clock, and a button for adding new
 * alarms.  The context menu allows the user to edit default settings.  Long-
 * clicking on the clock will trigger a dialog for enabling/disabling 'debug
 * mode.'
 */
public final class ActivityAlarmClock extends Activity {
  private enum Dialogs { TIME_PICKER, DELETE_CONFIRM };
  private enum Menus { DELETE_ALL, DEFAULT_ALARM_SETTINGS, APP_SETTINGS };

  private AlarmClockServiceBinder service;
  private NotificationServiceBinder notifyService;
  private DbAccessor db;
  private AlarmViewAdapter adapter;
  private TextView clock;
  private Button testBtn;
  private Button pendingBtn;
  private Handler handler;
  private Runnable tickCallback;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.alarm_list);

    // Access to in-memory and persistent data structures.
    service = new AlarmClockServiceBinder(getApplicationContext());
    db = new DbAccessor(getApplicationContext());
    handler = new Handler();
    notifyService = new NotificationServiceBinder(getApplicationContext());

    // Setup individual UI elements.
    // A simple clock.
    clock = (TextView) findViewById(R.id.clock);

    // Used in debug mode.  Schedules an alarm for 5 seconds in the future
    // when clicked.
    testBtn = (Button) findViewById(R.id.test_alarm);
    testBtn.setOnClickListener(new OnClickListener() {
      public void onClick(View view) {
        final Calendar testTime = Calendar.getInstance();
        testTime.add(Calendar.SECOND, 5);
        service.createAlarm(new AlarmTime(testTime.get(
            Calendar.HOUR_OF_DAY),
            testTime.get(Calendar.MINUTE),
            testTime.get(Calendar.SECOND)));
        adapter.requery();
      }
    });

    // Displays a list of pending alarms (only visible in debug mode).
    pendingBtn = (Button) findViewById(R.id.pending_alarms);
    pendingBtn.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        startActivity(
            new Intent(getApplicationContext(), ActivityPendingAlarms.class));
      }
    });

    // Opens the time picker dialog and allows the user to schedule a new alarm.
    Button addBtn = (Button) findViewById(R.id.add_alarm);
    addBtn.setOnClickListener(new View.OnClickListener() {
      public void onClick(View view) {
        showDialog(Dialogs.TIME_PICKER.ordinal());
      }
    });

    // Setup the alarm list and the underlying adapter.  Clicking an individual
    // item will start the settings activity.
    final ListView alarmList = (ListView) findViewById(R.id.alarm_list);
    adapter = new AlarmViewAdapter(this, db, service);
    alarmList.setAdapter(adapter);
    alarmList.setOnItemClickListener(new OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {
        final AlarmInfo info = (AlarmInfo) adapter.getItemAtPosition(position);
        final Intent i = new Intent(getApplicationContext(), ActivityAlarmSettings.class);
        i.putExtra(ActivityAlarmSettings.EXTRAS_ALARM_ID, info.getAlarmId());
        startActivity(i);
      }
    });

    // This is a self-scheduling callback that is responsible for refreshing
    // the screen.  It is started in onResume() and stopped in onPause().
    tickCallback = new Runnable() {
      @Override
      public void run() {
        // Redraw the screen.
        redraw();

        // Schedule the next update on the next interval boundary.
        AlarmUtil.Interval interval = AlarmUtil.Interval.MINUTE;
        if (AppSettings.isDebugMode(getApplicationContext())) {
          interval = AlarmUtil.Interval.SECOND;
        }
        long next = AlarmUtil.millisTillNextInterval(interval);
        handler.postDelayed(tickCallback, next);
      }
    };
  }

  @Override
  protected void onResume() {
    super.onResume();
    service.bind();
    handler.post(tickCallback);
    adapter.requery();
    notifyService.bind();
    notifyService.call(new NotificationServiceBinder.ServiceCallback() {
      @Override
      public void run(NotificationServiceInterface service) {
        int count;
        try {
          count = service.firingAlarmCount();
        } catch (RemoteException e) {
          return;
        } finally {
          handler.post(new Runnable() {
            @Override
            public void run() {
              notifyService.unbind();
            }
          });
        }
        if (count > 0) {
          Intent notifyActivity = new Intent(getApplicationContext(), ActivityAlarmNotification.class);
          notifyActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
          startActivity(notifyActivity);
        }
      }
    });
  }

  @Override
  protected void onPause() {
    super.onPause();
    handler.removeCallbacks(tickCallback);
    service.unbind();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    db.closeConnections();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuItem delete_all =
      menu.add(0, Menus.DELETE_ALL.ordinal(), 0, R.string.delete_all);
    delete_all.setIcon(android.R.drawable.ic_menu_close_clear_cancel);
    MenuItem alarm_settings =
      menu.add(0, Menus.DEFAULT_ALARM_SETTINGS.ordinal(), 0, R.string.default_settings);
    alarm_settings.setIcon(android.R.drawable.ic_lock_idle_alarm);
    MenuItem app_settings =
      menu.add(0, Menus.APP_SETTINGS.ordinal(), 0, R.string.app_settings);
    app_settings.setIcon(android.R.drawable.ic_menu_preferences);
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (Menus.values()[item.getItemId()]) {
      case DELETE_ALL:
        showDialog(Dialogs.DELETE_CONFIRM.ordinal());
        break;
      case DEFAULT_ALARM_SETTINGS:
        Intent alarm_settings = new Intent(getApplicationContext(), ActivityAlarmSettings.class);
        alarm_settings.putExtra(
            ActivityAlarmSettings.EXTRAS_ALARM_ID, AlarmSettings.DEFAULT_SETTINGS_ID);
        startActivity(alarm_settings);
        break;
      case APP_SETTINGS:
        Intent app_settings = new Intent(getApplicationContext(), ActivityAppSettings.class);
        startActivity(app_settings);
        break;
    }
    return super.onOptionsItemSelected(item);
  }

  private final void redraw() {
    // Show/hide debug buttons.
    if (AppSettings.isDebugMode(getApplicationContext())) {
      testBtn.setVisibility(View.VISIBLE);
      pendingBtn.setVisibility(View.VISIBLE);
    } else {
      testBtn.setVisibility(View.GONE);
      pendingBtn.setVisibility(View.GONE);
    }

    // Recompute expiration times in the list view
    adapter.notifyDataSetChanged();

    // Update clock
    Calendar c = Calendar.getInstance();
    AlarmTime time = new AlarmTime(
        c.get(Calendar.HOUR_OF_DAY),
        c.get(Calendar.MINUTE),
        c.get(Calendar.SECOND));
    clock.setText(time.localizedString(getApplicationContext()));
  }

  @Override
  protected Dialog onCreateDialog(int id) {
    switch (Dialogs.values()[id]) {
      case TIME_PICKER:
        Dialog picker = new TimePickerDialog(
            this, getString(R.string.add_alarm), AppSettings.isDebugMode(this),
            new TimePickerDialog.OnTimeSetListener() {
              @Override
              public void onTimeSet(int hourOfDay, int minute, int second) {
                // When a time is selected, create it via the service and
                // force the list view to re-query the alarm list. 
                service.createAlarm(new AlarmTime(hourOfDay, minute, second));
                adapter.requery();
                // Destroy this dialog so that it does not save its state.
                removeDialog(Dialogs.TIME_PICKER.ordinal());
              }
            });
        picker.setOnCancelListener(new OnCancelListener() {
          @Override
          public void onCancel(DialogInterface dialog) {
            removeDialog(Dialogs.TIME_PICKER.ordinal());
          }
        });
        return picker;
      case DELETE_CONFIRM:
        final AlertDialog.Builder deleteConfirmBuilder = new AlertDialog.Builder(this);
        deleteConfirmBuilder.setTitle(R.string.delete_all);
        deleteConfirmBuilder.setMessage(R.string.confirm_delete);
        deleteConfirmBuilder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            service.deleteAllAlarms();
            adapter.requery();
            dismissDialog(Dialogs.DELETE_CONFIRM.ordinal());
          }
        });
        deleteConfirmBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            dismissDialog(Dialogs.DELETE_CONFIRM.ordinal());
          }
        });
        return deleteConfirmBuilder.create();
      default:
        return super.onCreateDialog(id);
    }
  }
}
