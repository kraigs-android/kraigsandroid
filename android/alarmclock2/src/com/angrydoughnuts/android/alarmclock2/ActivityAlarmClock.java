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

import android.app.Activity;
import android.app.LoaderManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class ActivityAlarmClock extends Activity {
  private ServiceAlarmClock service = null;
  private final ServiceConnection connection = new ServiceConnection() {
      @Override
      public void onServiceConnected(ComponentName name, IBinder binder) {
        service = ((ServiceAlarmClock.IdentityBinder)binder).getService();
      }
      @Override
      public void onServiceDisconnected(ComponentName name) {
        service = null;
      }
    };

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.alarm_list);

    final CursorAdapter adapter = new SimpleCursorAdapter(
        this, R.layout.alarm_list_item, null,
        new String[] { ProviderAlarmClock.AlarmEntry.TIME },
        new int[] { R.id.debug_text });
    ((ListView)findViewById(R.id.alarm_list)).setAdapter(adapter);
    getLoaderManager().initLoader(
        0, null, new LoaderManager.LoaderCallbacks<Cursor>() {
            @Override
            public Loader<Cursor> onCreateLoader(int id, Bundle args) {
              return new CursorLoader(
                  getApplicationContext(), ProviderAlarmClock.ALARMS_URI,
                  new String[] {
                    ProviderAlarmClock.AlarmEntry._ID,
                    ProviderAlarmClock.AlarmEntry.TIME },
                  null, null, ProviderAlarmClock.AlarmEntry.TIME + " ASC");
            }
            @Override
            public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
              adapter.changeCursor(data);
            }
            @Override
            public void onLoaderReset(Loader<Cursor> loader) {
              adapter.changeCursor(null);
            }
          });

    ((Button)findViewById(R.id.test_alarm)).setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            if (service != null) service.createTestAlarm();
          }
        });
  }

  @Override
  public void onStart() {
    super.onStart();
    if (service == null) {
      bindService(new Intent(this, ServiceAlarmClock.class), connection,
                  Context.BIND_AUTO_CREATE);
    }
  }

  @Override
  public void onStop() {
    super.onStop();
    if (service != null) {
      unbindService(connection);
      service = null;
    }
  }
}
