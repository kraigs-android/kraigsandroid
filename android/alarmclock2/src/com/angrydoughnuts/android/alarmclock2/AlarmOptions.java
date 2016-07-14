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

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Process;
import android.os.Vibrator;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import java.util.Calendar;

public class AlarmOptions extends DialogFragment {
  private final SettingsObserver observer = new SettingsObserver();

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    super.onCreateDialog(savedInstanceState);

    {
      String p = Manifest.permission.READ_EXTERNAL_STORAGE;
      if (getContext().checkPermission(p, Process.myPid(), Process.myUid()) !=
          PackageManager.PERMISSION_GRANTED)
        requestPermissions(new String[] { p }, 0);
    }

    final long id = getArguments().getLong(
        AlarmNotificationService.ALARM_ID, -1);
    final boolean defaults = id == DbUtil.Settings.DEFAULTS_ID;

    final ScrollView v = new ScrollView(getContext());
    final OptionsView o = new OptionsView(
        getContext(), getFragmentManager(), id);
    v.addView(o);

    if (savedInstanceState != null) {
      TimePicker t = (TimePicker)getFragmentManager()
        .findFragmentByTag("edit_alarm");
      RepeatEditor r = (RepeatEditor)getFragmentManager()
        .findFragmentByTag("edit_repeat");
      MediaPicker m = (MediaPicker)getFragmentManager()
        .findFragmentByTag("edit_tone");
      if (t != null) t.setListener(o.time_listener);
      if (r != null) r.setListener(o.repeat_listener);
      if (m != null) m.setListener(o.tone_listener);
    }

    // NOTE: observe alarm entry changes but not settings entry changes.
    getContext().getContentResolver().registerContentObserver(
        ContentUris.withAppendedId(
            AlarmClockProvider.ALARMS_URI, id), false, observer);

    AlertDialog d = new AlertDialog.Builder(getContext())
      // TODO: string
      .setTitle(defaults ? "Default Alarm Options" : "Alarm Options")
      .setView(v)
      // TODO: string
      .setPositiveButton("Done",
        new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            DbUtil.Alarm a = DbUtil.Alarm.get(getContext(), id);
            if (a.enabled || !observer.state_changed)
              return;
            // Enable the alarm if the alarm entry changed.
            ContentValues val = new ContentValues();
            val.put(AlarmClockProvider.AlarmEntry.ENABLED, true);
            getContext().getContentResolver().update(
                ContentUris.withAppendedId(AlarmClockProvider.ALARMS_URI, id),
                val, null, null);
            long utc = TimeUtil.nextOccurrence(a.time, a.repeat)
              .getTimeInMillis();
            AlarmNotificationService.scheduleAlarmTrigger(
                getContext(), id, utc);
          }
        })
      // TODO: string
      .setNeutralButton(!defaults ? "Delete" : null,
        new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            new DialogFragment() {
              @Override
              public Dialog onCreateDialog(Bundle savedInstanceState) {
                return new AlertDialog.Builder(getContext())
                  // TODO: string
                  .setTitle("Confirm Delete")
                  // TODO: string
                  .setMessage("Are you sure you want to delete this alarm?")
                  // TODO: string
                  .setNegativeButton("Cancel", null)
                  // TODO: string
                  .setPositiveButton(
                      "OK", new DialogInterface.OnClickListener() {
                      @Override
                      public void onClick(DialogInterface dialog, int which) {
                        getContext().getContentResolver().delete(
                            ContentUris.withAppendedId(
                                AlarmClockProvider.ALARMS_URI, id), null, null);
                        AlarmNotificationService.removeAlarmTrigger(
                            getContext(), id);
                      }
                  }).create();
              }
            }.show(getFragmentManager(), "confirm_delete");
          }
        }).create();

    // Don't display the keyboard until necessary
    d.getWindow().setSoftInputMode(
        WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

    return d;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    getContext().getContentResolver().unregisterContentObserver(observer);
  }

  private static class SettingsObserver extends ContentObserver {
    public boolean state_changed = false;
    public SettingsObserver() { super(null); }
    @Override
    public void onChange(boolean selfChange, Uri uri) {
      state_changed = true;
    }
  };

  private static class RepeatEditor extends DialogFragment {
    final public static String BITMASK = "bitmask";
    public static interface OnPickListener {
      abstract void onPick(int repeats);
    }

    private OnPickListener listener = null;
    public void setListener(OnPickListener l) { listener = l; }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
      super.onCreateDialog(savedInstanceState);

      final boolean checked[] = new boolean[] {
        false, false,false,false,false,false,false
      };
      if (getArguments() != null && savedInstanceState == null) {
        int b = getArguments().getInt(BITMASK, 0);
        for (int i = 0; i < 7; ++i)
          checked[i] = (b & (1 << i)) != 0;
      }
      // TODO: string
      final CharSequence days[] = new CharSequence[] {
        "Sunday",
        "Monday",
        "Tuesday",
        "Wednesday",
        "Thursday",
        "Friday",
        "Saturday"
      };
      return new AlertDialog.Builder(getContext())
        // TODO: string
        .setTitle("Repeat")
        .setMultiChoiceItems(days, checked, null)
        // TODO: string
        .setNegativeButton("Cancel", null)
        // TODO: string
        .setPositiveButton(
            "OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                  if (listener == null)
                    return;
                  int b = 0;
                  ListView list = ((AlertDialog)dialog).getListView();
                  for (int i = 0; i < list.getCount(); ++i)
                    if (list.isItemChecked(i))
                      b |= 1 << i;
                  listener.onPick(b);
                }
              })
        .create();
    }
  }

  private static class OptionsView extends LinearLayout {
    public final TimePicker.OnTimePickListener time_listener;
    public final RepeatEditor.OnPickListener repeat_listener;
    public final MediaPicker.Listener tone_listener;

    public OptionsView(
        final Context c, final FragmentManager fm, final long id) {
      super(c);
      setOrientation(LinearLayout.VERTICAL);

      final Uri uri = ContentUris.withAppendedId(
          AlarmClockProvider.ALARMS_URI, id);
      final Uri settings = ContentUris.withAppendedId(
          AlarmClockProvider.SETTINGS_URI, id);
      final boolean defaults = id == DbUtil.Settings.DEFAULTS_ID;


      final DbUtil.Alarm alarm = DbUtil.Alarm.get(c, id);
      final DbUtil.Settings s = DbUtil.Settings.get(c, id);

      // EDIT TIME
      final ViewGroup edit_time = newItem(c);
      if (!defaults) addView(edit_time);
      time_listener = new TimePicker.OnTimePickListener() {
          @Override
          public void onTimePick(int t) {
            ContentValues val = new ContentValues();
            val.put(AlarmClockProvider.AlarmEntry.TIME, t);
            c.getContentResolver().update(uri, val, null, null);

            final DbUtil.Alarm a = DbUtil.Alarm.get(c, id);
            final Calendar next =
              TimeUtil.nextOccurrence(t, a.repeat, a.next_snooze);
            if (alarm.enabled) {
              AlarmNotificationService.removeAlarmTrigger(c, id);
              AlarmNotificationService.scheduleAlarmTrigger(
                  c, id, next.getTimeInMillis());
            }
            setText(edit_time, TimeUtil.formatLong(c, next));
          }
        };
      setImage(edit_time, R.drawable.ic_alarm);
      setText(edit_time, TimeUtil.formatLong(
          c, TimeUtil.nextOccurrence(alarm.time, alarm.repeat)));
      edit_time.setOnClickListener(
          new View.OnClickListener() {
            @Override
            public void onClick(View view) {
              DbUtil.Alarm a = DbUtil.Alarm.get(c, id);
              TimePicker time_pick = new TimePicker();
              time_pick.setListener(time_listener);
              Bundle b = new Bundle();
              b.putInt(TimePicker.TIME, a.time);
              // TODO: string
              b.putString(TimePicker.TITLE, "Edit time");
              b.putInt(TimePicker.REPEAT, a.repeat);
              time_pick.setArguments(b);
              time_pick.show(fm, "edit_alarm");
            }
          });

      // EDIT REPEAT
      final ViewGroup edit_repeat = newItem(c);
      if (!defaults) addView(edit_repeat);
      repeat_listener = new RepeatEditor.OnPickListener() {
          @Override
          public void onPick(int repeat) {
            ContentValues val = new ContentValues();
            val.put(AlarmClockProvider.AlarmEntry.DAY_OF_WEEK, repeat);
            c.getContentResolver().update(uri, val, null, null);
            // TODO: string
            setText(edit_repeat, repeat == 0 ? "No repeats" :
                    TimeUtil.repeatString(repeat));
            final DbUtil.Alarm a = DbUtil.Alarm.get(c, id);
            final Calendar next =
              TimeUtil.nextOccurrence(a.time, repeat, a.next_snooze);
            if (alarm.enabled) {
              AlarmNotificationService.removeAlarmTrigger(c, id);
              AlarmNotificationService.scheduleAlarmTrigger(
                  c, id, next.getTimeInMillis());
            }
          }
        };
      setImage(edit_repeat, R.drawable.ic_today);
      // TODO: string
      setText(edit_repeat, alarm.repeat == 0 ? "No repeats" :
              TimeUtil.repeatString(alarm.repeat));
      edit_repeat.setOnClickListener(
          new View.OnClickListener() {
            @Override
            public void onClick(View view) {
              RepeatEditor edit = new RepeatEditor();
              Bundle b = new Bundle();
              b.putInt(RepeatEditor.BITMASK, DbUtil.Alarm.get(c, id).repeat);
              edit.setArguments(b);
              edit.setListener(repeat_listener);
              edit.show(fm, "edit_repeat");
            }
          });

      // EDIT LABEL
      final ViewGroup edit_label = newItem(c);
      if (!defaults) addView(edit_label);
      final TextWatcher label_change = new TextWatcher() {
          @Override
          public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
          @Override
          public void onTextChanged(CharSequence s, int st, int b, int c) {}
          @Override
          public void afterTextChanged(Editable s) {
            final String name = s.toString();
            ContentValues val = new ContentValues();
            val.put(AlarmClockProvider.AlarmEntry.NAME, name);
            c.getContentResolver().update(uri, val, null, null);
          }
        };
      setImage(edit_label, R.drawable.ic_label_outline);
      // TODO: string
      setEdit(edit_label, alarm.label, "Label", label_change);

      // EDIT TONE
      final ViewGroup edit_tone = newItem(c);
      addView(edit_tone);
      tone_listener = new MediaPicker.Listener() {
          public void onMediaPick(Uri uri, String title) {
            ContentValues val = new ContentValues();
            val.put(AlarmClockProvider.SettingsEntry.TONE_URL, uri.toString());
            val.put(AlarmClockProvider.SettingsEntry.TONE_NAME, title);
            c.getContentResolver().update(settings, val, null, null);
            setText(edit_tone, title);
          }
        };
      setImage(edit_tone, R.drawable.ic_music_note);
      setText(edit_tone, s.tone_name);
      edit_tone.setOnClickListener(
          new View.OnClickListener() {
            @Override
            public void onClick(View view) {
              MediaPicker media_pick = new MediaPicker();
              media_pick.setListener(tone_listener);
              media_pick.show(fm, "edit_tone");
            }
          });

      // EDIT VIBRATE
      final ViewGroup edit_vibrate = newItem(c);
      addView(edit_vibrate);
      setImage(edit_vibrate, R.drawable.ic_vibration);
      Switch vibrate_switch = new Switch(c);
      setView(edit_vibrate, vibrate_switch, 0.0f);
      vibrate_switch.setChecked(s.vibrate);
      vibrate_switch.setOnCheckedChangeListener(
          new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton b, boolean checked) {
              if (checked)
                ((Vibrator)c.getSystemService(Context.VIBRATOR_SERVICE))
                  .vibrate(100);
              ContentValues val = new ContentValues();
              val.put(AlarmClockProvider.SettingsEntry.VIBRATE, checked);
              c.getContentResolver().update(settings, val, null, null);
            }
          });

      // EDIT SNOOZE
      final ViewGroup edit_snooze = newItem(c);
      addView(edit_snooze);
      setImage(edit_snooze, R.drawable.ic_snooze);
      // TODO: string
      setText(edit_snooze, s.snooze + "m");
      final SeekBar snooze_bar = new SeekBar(c);
      setView(edit_snooze, snooze_bar, 1.0f);
      // Range 5 - 60 increments of 5.
      snooze_bar.setMax(11);
      snooze_bar.setProgress((s.snooze - 5) / 5);
      snooze_bar.setOnSeekBarChangeListener(
          new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int prog, boolean user) {
              final int snooze = prog * 5 + 5;
              // TODO: string
              setText(edit_snooze, snooze + "m");
            }
            @Override
            public void onStartTrackingTouch(SeekBar s) {}
            @Override
            public void onStopTrackingTouch(SeekBar s) {
              final int snooze = s.getProgress() * 5 + 5;
              ContentValues val = new ContentValues();
              val.put(AlarmClockProvider.SettingsEntry.SNOOZE, snooze);
              c.getContentResolver().update(settings, val, null, null);
            }
          });

      // VOLUME FADE
      final TextView volume_status = new TextView(c);
      // TODO: string
      volume_status.setText("volume " + s.volume_starting + " to " + s.volume_ending + " over " + s.volume_time);

      // Range 0 - 100 increments of 5.
      final RangeBar volume_range = new RangeBar(c);
      volume_range.setRange(20);
      volume_range.setPosition(
          s.volume_starting / 5,
          s.volume_ending / 5);

      // Range 0 - 60 increments of 5.
      final SeekBar volume_time_slide = new SeekBar(c);
      volume_time_slide.setMax(12);
      volume_time_slide.setProgress(s.volume_time / 5);

      volume_range.setListener(
          new RangeBar.Listener() {
            @Override
            public void onChange(int min, int max) {
              final int volume_starting = min * 5;
              final int volume_ending = max * 5;
              final int volume_time = volume_time_slide.getProgress() * 5;
              // TODO: string
              volume_status.setText("volume " + volume_starting + " to " + volume_ending + " over " + volume_time);
            }
            public void onDone(int min, int max) {
              ContentValues val = new ContentValues();
              val.put(AlarmClockProvider.SettingsEntry.VOLUME_STARTING, min*5);
              val.put(AlarmClockProvider.SettingsEntry.VOLUME_ENDING, max*5);
              c.getContentResolver().update(settings, val, null, null);
            }
          });

      volume_time_slide.setOnSeekBarChangeListener(
          new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int progress, boolean user) {
              final int volume_starting = volume_range.getPositionMin() * 5;
              final int volume_ending = volume_range.getPositionMax() * 5;
              final int volume_time = volume_time_slide.getProgress() * 5;
              // TODO: string
              volume_status.setText("volume " + volume_starting + " to " + volume_ending + " over " + volume_time);
            }
            @Override
            public void onStartTrackingTouch(SeekBar s) {}
            @Override
            public void onStopTrackingTouch(SeekBar s) {
              ContentValues val = new ContentValues();
              val.put(AlarmClockProvider.SettingsEntry.VOLUME_TIME,
                      s.getProgress() * 5);
              c.getContentResolver().update(settings, val, null, null);
            }
          });

      final ViewGroup edit_volume = newItem(c);
      addView(edit_volume);
      setImage(edit_volume, R.drawable.ic_volume_up);
      setView(edit_volume, volume_range, 1.0f);

      final ViewGroup edit_volume_time = newItem(c);
      addView(edit_volume_time);
      setImage(edit_volume_time, R.drawable.ic_access_time);
      setView(edit_volume_time, volume_time_slide, 1.0f);

      setView(this, volume_status, 1.0f);
    }

    private ViewGroup newItem(Context c) {
      return (ViewGroup)
        ((LayoutInflater)c.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
        .inflate(R.layout.settings_item, null);
    }

    private void setImage(ViewGroup v, int id) {
      ((ImageView)v.findViewById(R.id.setting_icon)).setImageResource(id);
    }

    private void setText(ViewGroup v, String s) {
      TextView t = (TextView)v.findViewById(R.id.setting_text);
      t.setVisibility(View.VISIBLE);
      t.setText(s);
    }

    private void setEdit(View v, String s, String hint, TextWatcher w) {
      EditText t = (EditText)v.findViewById(R.id.setting_edit);
      t.setVisibility(View.VISIBLE);
      t.setText(s);
      t.setSelection(s.length());
      t.setHint(hint);
      t.addTextChangedListener(w);
    }

    private void setView(ViewGroup g, View v, float gravity) {
      LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
          LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, gravity);
      p.gravity = Gravity.CENTER;
      g.addView(v, -1, p);
    }

  }
}
