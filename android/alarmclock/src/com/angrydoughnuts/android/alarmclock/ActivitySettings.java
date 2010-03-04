package com.angrydoughnuts.android.alarmclock;

import java.util.Calendar;

import com.angrydoughnuts.android.alarmclock.AlarmInfo.Day;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.AdapterView.OnItemClickListener;

public class ActivitySettings extends Activity {
  static public final String EXTRAS_ALARM_ID = "alarm_id";

  private enum AlarmInfoType { TIME, NAME, DAYS_OF_WEEK; }
  private enum SettingType { TONE, SNOOZE, VIBRATE, VOLUME_FADE; }

  private final int MISSING_EXTRAS = -69;
  private enum Dialogs { TIME_PICKER, NAME_PICKER, DOW_PICKER, TONE_PICKER, SNOOZE_PICKER, VOLUME_FADE_PICKER }

  private long alarmId;
  private AlarmClockServiceBinder service;
  private DbAccessor db;
  private AlarmInfo info;
  private AlarmSettings settings;
  SettingsAdapter alarmInfoAdapter;
  SettingsAdapter settingsAdapter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.settings);

    alarmId = getIntent().getExtras().getLong(EXTRAS_ALARM_ID, MISSING_EXTRAS);
    if (alarmId == MISSING_EXTRAS) {
      throw new IllegalStateException("EXTRAS_ALARM_ID not supplied in intent.");
    }

    service = AlarmClockServiceBinder.newBinder(getApplicationContext());
    db = new DbAccessor(getApplicationContext());

    info = db.readAlarmInfo(alarmId);
    settings = db.readAlarmSettings(alarmId);

    Button okButton = (Button) findViewById(R.id.settings_ok);
    okButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        // TODO(cgallek): This needs to update the alarm info through the service rather
        // than directly writing to the database (unschedule old alarm, schedule new if enabled).
        // It should be fine to leave the settings write directly to the db.  They are not
        // maintained in memory anywhere.
        db.writeAlarmInfo(alarmId, info);
        db.writeAlarmSettings(alarmId, settings);
        finish();
      }
    });
    Button cancelButton = (Button) findViewById(R.id.settings_cancel);
    cancelButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        finish();
      }
    });
    Button deleteButton = (Button) findViewById(R.id.settings_delete);
    deleteButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        // TODO(cgallek): Add confirmation dialog.
        service.deleteAlarm(alarmId);
        finish();
      }
    });

    ListView alarmInfoList = (ListView) findViewById(R.id.alarm_info_list);
    if (alarmId != AlarmSettings.DEFAULT_SETTINGS_ID) {
      // TODO(cgallek): move all these strings to strings.xml
      Setting[] alarmInfoObjects = new Setting[AlarmInfoType.values().length];
      alarmInfoObjects[AlarmInfoType.TIME.ordinal()] = new Setting() {
        @Override
        public String name() { return "Time"; }
        @Override
        public String value() { return new AlarmTime(info.getTime()).localizedString(getApplicationContext()); }
      };
      alarmInfoObjects[AlarmInfoType.NAME.ordinal()] = new Setting() {
        @Override
        public String name() { return "Label"; }
        @Override
        public String value() { return info.getName(); }
      };
      alarmInfoObjects[AlarmInfoType.DAYS_OF_WEEK.ordinal()] = new Setting() {
        @Override
        public String name() { return "Repeat"; }
        @Override
        public String value() { return info.getDaysOfWeekString(getApplicationContext()); }
      };
      alarmInfoAdapter = new SettingsAdapter(getApplicationContext(), alarmInfoObjects);
      alarmInfoList.setAdapter(alarmInfoAdapter);
      alarmInfoList.setOnItemClickListener(new AlarmInfoListClickListener());
    }

    // TODO(cgallek): move all these strings to strings.xml
    Setting[] settingsObjects = new Setting[SettingType.values().length];
    settingsObjects[SettingType.TONE.ordinal()] = new Setting() {
      @Override
      public String name() { return "Tone"; }
      @Override
      public String value() {
        String value = settings.getToneName();
        if (AlarmClockService.debug(getApplicationContext())) {
          value += " " + settings.getTone().toString();
        }
        return value;
      }
      
    };
    settingsObjects[SettingType.SNOOZE.ordinal()] = new Setting() {
      @Override
      public String name() { return "Snooze (minutes)"; }
      @Override
      public String value() { return "" + settings.getSnoozeMinutes(); }      
    };
    settingsObjects[SettingType.VIBRATE.ordinal()] = new Setting() {
      @Override
      public String name() { return "Vibrate"; }
      @Override
      public String value() { return settings.getVibrate() ? "Enabled" : "Disabled"; }
    };
    settingsObjects[SettingType.VOLUME_FADE.ordinal()] = new Setting() {
      @Override
      public String name() { return "Alarm volume fade"; }
      @Override
      public String value() { return "From " + settings.getVolumeStartPercent() + "% "
      + "to " + settings.getVolumeEndPercent() + "% "
      + "over " + settings.getVolumeChangeTimeSec() + " seconds."; }
    };
    
    ListView settingsList = (ListView) findViewById(R.id.settings_list);
    settingsAdapter = new SettingsAdapter(getApplicationContext(), settingsObjects);
    settingsList.setAdapter(settingsAdapter);
    settingsList.setOnItemClickListener(new SettingsListClickListener());

    // The alarm info section and the delete button should not be shown when
    // editing the default settings.
    if (alarmId == AlarmSettings.DEFAULT_SETTINGS_ID) {
      deleteButton.setVisibility(View.GONE);
      alarmInfoList.setVisibility(View.GONE);
      TextView infoHeader = (TextView) findViewById(R.id.alarm_info_header);
      infoHeader.setVisibility(View.GONE);
    }
  }

  @Override
  protected void onStart() {
    super.onStart();
    service.bind();
  }

  @Override
  protected void onStop() {
    super.onStop();
    service.unbind();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    db.closeConnections();
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (resultCode != RESULT_OK) {
      return;
    }

    switch (Dialogs.values()[requestCode]) {
      case TONE_PICKER:
        // This can be null if 'Silent' was selected, but it was disabled
        // above so that should never happen.
        Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
        if (uri == null) {
          // This should never happen, but fall back to the phone ringer just
          // in case.
          uri = Settings.System.DEFAULT_ALARM_ALERT_URI;
        }
        Context c = getApplicationContext();
        Ringtone tone = RingtoneManager.getRingtone(c, uri);
        String name = tone != null ? tone.getTitle(c) : "Unknown name";
        settings.setTone(uri, name);
        settingsAdapter.notifyDataSetChanged();
      default:
        super.onActivityResult(requestCode, resultCode, data);
    }
  }

  @Override
  protected Dialog onCreateDialog(int id) {
    switch (Dialogs.values()[id]) {
      case TIME_PICKER:
        AlarmTime time = new AlarmTime(info.getTime());
        int hour = time.calendar().get(Calendar.HOUR_OF_DAY);
        int minute = time.calendar().get(Calendar.MINUTE);
        boolean is24Hour = DateFormat.is24HourFormat(getApplicationContext());
        return new TimePickerDialog(this,
            new TimePickerDialog.OnTimeSetListener() {
              @Override
              public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                AlarmTime newTime = new AlarmTime(hourOfDay, minute, 0);
                info.setTime(newTime.secondsAfterMidnight());
                alarmInfoAdapter.notifyDataSetChanged();
              }
            },
            hour, minute, is24Hour);
      case NAME_PICKER:
        AlertDialog.Builder nameBuilder = new AlertDialog.Builder(this);
        // TODO(cgallek): move this to strings.xml
        nameBuilder.setTitle("Alarm Label");
        View nameView = getLayoutInflater().inflate(R.layout.name_settings_dialog, null);
        final TextView label = (TextView) nameView.findViewById(R.id.name_label);
        label.setText(info.getName());
        nameBuilder.setView(nameView);
        nameBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            info.setName(label.getEditableText().toString());
            alarmInfoAdapter.notifyDataSetChanged();
            dismissDialog(Dialogs.NAME_PICKER.ordinal());
          }
        });
        nameBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            dismissDialog(Dialogs.NAME_PICKER.ordinal());
          }
        });
        return nameBuilder.create();
      case DOW_PICKER:
        AlertDialog.Builder dowBuilder = new AlertDialog.Builder(this);
        // TODO(cgallek): move this to strings.xml
        dowBuilder.setTitle("Repeat Days");
        dowBuilder.setMultiChoiceItems(
            info.getDaysOfWeek().names(getApplicationContext()),
            info.getDaysOfWeek().bitmask(),
            new OnMultiChoiceClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                if (isChecked) {
                  info.getDaysOfWeek().addDay(Day.values()[which]);
                } else {
                  info.getDaysOfWeek().removeDay(Day.values()[which]);
                }
              }
        });
        dowBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            alarmInfoAdapter.notifyDataSetChanged();
            dismissDialog(Dialogs.DOW_PICKER.ordinal());
          }
        });
        return dowBuilder.create();

      case SNOOZE_PICKER:
        // TODO(cgallek): this is silly...
        final CharSequence[] items = {
            "1", "2", "3", "4", "5", "6", "7", "8", "9", "10",
            "11", "12", "13", "14", "15", "16", "17", "18", "19", "20",
            "21", "22", "23", "24", "25", "26", "27", "28", "29", "30",
            "31", "32", "33", "34", "35", "36", "37", "38", "39", "40",
            "41", "42", "43", "44", "45", "46", "47", "48", "49", "50",
            "51", "52", "53", "54", "55", "56", "57", "58", "59", "60" };
        AlertDialog.Builder snoozeBuilder = new AlertDialog.Builder(this);
        // TODO(cgallek): move this to strings.xml
        snoozeBuilder.setTitle("Snooze (in minutes)");
        snoozeBuilder.setSingleChoiceItems(items, settings.getSnoozeMinutes() - 1,
            new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int item) {
            settings.setSnoozeMinutes(item + 1);
            settingsAdapter.notifyDataSetChanged();
            dismissDialog(Dialogs.SNOOZE_PICKER.ordinal());
          }
        });
        return snoozeBuilder.create();
      case VOLUME_FADE_PICKER:
        AlertDialog.Builder fadeBuilder = new AlertDialog.Builder(this);
        // TODO(cgallek): move this to strings.xml
        fadeBuilder.setTitle("Alarm Volume Fade");
        View fadeView = getLayoutInflater().inflate(R.layout.fade_settings_dialog, null);
        final EditText volumeStart = (EditText) fadeView.findViewById(R.id.volume_start);
        volumeStart.setText("" + settings.getVolumeStartPercent());
        final EditText volumeEnd = (EditText) fadeView.findViewById(R.id.volume_end);
        volumeEnd.setText("" + settings.getVolumeEndPercent());
        final EditText volumeDuration = (EditText) fadeView.findViewById(R.id.volume_duration);
        volumeDuration.setText("" + settings.getVolumeChangeTimeSec());
        fadeBuilder.setView(fadeView);
        fadeBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            settings.setVolumeStartPercent(Integer.parseInt(volumeStart.getText().toString()));
            settings.setVolumeEndPercent(Integer.parseInt(volumeEnd.getText().toString()));
            settings.setVolumeChangeTimeSec(Integer.parseInt(volumeDuration.getText().toString()));
            settingsAdapter.notifyDataSetChanged();
            dismissDialog(Dialogs.VOLUME_FADE_PICKER.ordinal());
          }
        });
        fadeBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            dismissDialog(Dialogs.VOLUME_FADE_PICKER.ordinal());
          }
        });
        return fadeBuilder.create();
      default:
        return super.onCreateDialog(id);
    }
  }

  class AlarmInfoListClickListener implements OnItemClickListener {
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
      switch (AlarmInfoType.values()[position]) {
        case TIME:
          showDialog(Dialogs.TIME_PICKER.ordinal());
          break;
        case NAME:
          showDialog(Dialogs.NAME_PICKER.ordinal());
          break;
        case DAYS_OF_WEEK:
          showDialog(Dialogs.DOW_PICKER.ordinal());
          break;
      }
    }
  }

  class SettingsListClickListener implements OnItemClickListener {
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
      switch (SettingType.values()[position]) {
        case TONE:
          Uri current_tone = settings.getTone();
          Intent i = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
          i.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALL);
          i.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
          i.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
          i.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, Settings.System.DEFAULT_ALARM_ALERT_URI);
          i.putExtra(RingtoneManager.EXTRA_RINGTONE_INCLUDE_DRM, true);
          i.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Default Alarm Tone");
          i.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, current_tone);
          startActivityForResult(i, Dialogs.TONE_PICKER.ordinal());
          break;
        case SNOOZE:
          showDialog(Dialogs.SNOOZE_PICKER.ordinal());
          break;
        case VIBRATE:
          settings.setVibrate(!settings.getVibrate());
          settingsAdapter.notifyDataSetChanged();
          break;
        case VOLUME_FADE:
          showDialog(Dialogs.VOLUME_FADE_PICKER.ordinal());
          break;
      }
    }
  }

  private abstract class Setting {
    public abstract String name();
    public abstract String value();
  }

  private class SettingsAdapter extends ArrayAdapter<Setting> {
    public SettingsAdapter(Context context, Setting[] settingsObjects) {
      super(context, 0, settingsObjects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      LayoutInflater inflater = getLayoutInflater();
      View row = inflater.inflate(R.layout.settings_item, null); 
      TextView name = (TextView) row.findViewById(R.id.setting_name);
      TextView value = (TextView) row.findViewById(R.id.setting_value);
      Setting setting = getItem(position);
      name.setText(setting.name());
      value.setText(setting.value());
      return row;
    }
  }
}
