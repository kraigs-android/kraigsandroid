/****************************************************************************
 * Copyright 2009 kraigs.android@gmail.com
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

package com.angrydoughnuts.android.brightprof;

import java.math.BigDecimal;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

public class BrightnessProfiles extends Activity {
  private static final int ACTIVITY_EDIT = 0;

  private static final int MENU_EDIT = 0;
  private static final int MENU_DELETE = 1;

  private int appBrightness;
  private DbAccessor dbAccessor;
  private Cursor listViewCursor;

  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // Initialize the database helper.
    dbAccessor = new DbAccessor(this);
    setContentView(R.layout.main);

    // Button to close the main window.
    Button closeBtn = (Button) findViewById(R.id.close_button);
    closeBtn.setOnClickListener(new View.OnClickListener() {
      public void onClick(View view) {
        finish();
      }
    });
    // Button to open the edit dialog (in add mode).
    Button addBtn = (Button) findViewById(R.id.add_button);
    addBtn.setOnClickListener(new View.OnClickListener() {
      public void onClick(View view) {
        Intent i = new Intent(getApplication(), EditActivity.class);
        startActivityForResult(i, ACTIVITY_EDIT);
      }
    });

    // Setup slider.
    SeekBar slider = (SeekBar) findViewById(R.id.slider);
    slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
      public void onProgressChanged(SeekBar seekBar, int progress,
          boolean fromTouch) {
        if (fromTouch) {
          setBrightness(progress);
          refreshDisplay();
        }
      }

      public void onStartTrackingTouch(SeekBar seekBar) {
      }

      public void onStopTrackingTouch(SeekBar seekBar) {
      }
    });

    // Get a database cursor.
    listViewCursor = dbAccessor.getAll();
    startManagingCursor(listViewCursor);
    // Populate the list view using the Cursor.
    String[] from = new String[] { "name" };
    int[] to = new int[] { R.id.profile_name };
    SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,
        R.layout.profile, listViewCursor, from, to);
    ListView profileList = (ListView) findViewById(R.id.profile_list);
    profileList.setAdapter(adapter);
    // Set the per-item click handler.
    profileList.setOnItemClickListener(new OnItemClickListener() {
      public void onItemClick(AdapterView<?> parent, View view, int position,
          long id) {
        listViewCursor.moveToPosition(position);
        int brightness = listViewCursor.getInt(listViewCursor
            .getColumnIndexOrThrow(DbHelper.PROF_VALUE_COL));
        setBrightness(brightness);
      }
    });
    registerForContextMenu(profileList);
  }

  @Override
  protected void onResume() {
    // Lookup the initial system brightness and set our app's brightness
    // percentage appropriately.
    int systemBrightness = 0;
    try {
      systemBrightness = Settings.System.getInt(getContentResolver(),
          Settings.System.SCREEN_BRIGHTNESS);
    } catch (SettingNotFoundException e) {
      // TODO Log an error message.
    }
    BigDecimal d = new BigDecimal((systemBrightness * 100) / 255);
    d = d.setScale(0, BigDecimal.ROUND_HALF_EVEN);
    appBrightness = d.intValue();
    // Set the value for the brightness text field and slider.
    refreshDisplay();

    super.onResume();
  }

  @Override
  public void onCreateContextMenu(ContextMenu menu, View v,
      ContextMenuInfo menuInfo) {
    menu.add(Menu.NONE, MENU_EDIT, Menu.NONE, R.string.edit);
    menu.add(Menu.NONE, MENU_DELETE, Menu.NONE, R.string.delete);
  }

  @Override
  public boolean onContextItemSelected(MenuItem item) {
    AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
    switch (item.getItemId()) {
      case MENU_EDIT:
        listViewCursor.moveToPosition(info.position);
        Intent i = new Intent(getApplication(), EditActivity.class);
        i.putExtra(DbHelper.PROF_ID_COL, listViewCursor.getInt(listViewCursor
            .getColumnIndexOrThrow(DbHelper.PROF_ID_COL)));
        i.putExtra(DbHelper.PROF_NAME_COL, listViewCursor
            .getString(listViewCursor
                .getColumnIndexOrThrow(DbHelper.PROF_NAME_COL)));
        i.putExtra(DbHelper.PROF_VALUE_COL, listViewCursor
            .getInt(listViewCursor
                .getColumnIndexOrThrow(DbHelper.PROF_VALUE_COL)));
        startActivityForResult(i, ACTIVITY_EDIT);
        return true;
      case MENU_DELETE:
        listViewCursor.moveToPosition(info.position);
        int id = listViewCursor.getInt(listViewCursor
            .getColumnIndexOrThrow(DbHelper.PROF_ID_COL));
        dbAccessor.deletProfile(id);
        listViewCursor.requery();
        return true;
      default:
        return super.onContextItemSelected(item);
    }
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    switch (keyCode) {
      case KeyEvent.KEYCODE_VOLUME_DOWN:
        setBrightness(getBrightness() - 10);
        return true;
      case KeyEvent.KEYCODE_VOLUME_UP:
        setBrightness(getBrightness() + 10);
        return true;
      default:
        return super.onKeyDown(keyCode, event);
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode,
      Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (resultCode == RESULT_CANCELED) {
      return;
    }

    if (requestCode != ACTIVITY_EDIT) {
      return;
    }

    Bundle extras = data.getExtras();
    int id = extras.getInt(DbHelper.PROF_ID_COL);
    switch (resultCode) {
      case Activity.RESULT_OK:
        String name = extras.getString(DbHelper.PROF_NAME_COL);
        int brightness = extras.getInt(DbHelper.PROF_VALUE_COL);

        dbAccessor.updateProfile(id, name, brightness);
        listViewCursor.requery();
        break;
    }
  }

  private void refreshDisplay() {
    TextView brightnessText = (TextView) findViewById(R.id.brightness);
    brightnessText.setText(getString(R.string.brightness) + " "
        + getBrightness() + "%");

    SeekBar slider = (SeekBar) findViewById(R.id.slider);
    slider.setProgress(getBrightness());
  }

  private int getBrightness() {
    return appBrightness;
  }

  private void setBrightness(int brightness) {
    // The screen is pretty much off at values <5.
    if (brightness < 5) {
      appBrightness = 5;
    } else if (brightness > 100) {
      appBrightness = 100;
    } else {
      appBrightness = brightness;
    }
    setSystemBrightness(appBrightness);
    refreshDisplay();
  }

  void setSystemBrightness(int brightnessPercentage) {
    BigDecimal d = new BigDecimal((brightnessPercentage * 255.0) / 100);
    d = d.setScale(0, BigDecimal.ROUND_HALF_EVEN);
    int brightnessUnits = d.intValue();
    if (brightnessUnits < 0) {
      brightnessUnits = 0;
    } else if (brightnessUnits > 255) {
      brightnessUnits = 255;
    }

    // Change the system brightness setting. This doesn't change the
    // screen brightness immediately. (Scale 0 - 255).
    Settings.System.putInt(getContentResolver(),
        Settings.System.SCREEN_BRIGHTNESS, brightnessUnits);

    // Set the brightness of the current window. This takes effect immediately.
    // When the window is closed, the new system brightness is used.
    // (Scale 0.0 - 1.0).
    WindowManager.LayoutParams lp = getWindow().getAttributes();
    lp.screenBrightness = brightnessPercentage / 100.0f;
    getWindow().setAttributes(lp);
  }
}
