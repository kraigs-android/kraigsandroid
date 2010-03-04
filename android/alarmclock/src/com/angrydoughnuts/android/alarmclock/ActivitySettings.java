package com.angrydoughnuts.android.alarmclock;

import java.util.Calendar;

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
  private enum Dialogs { TIME_PICKER, NAME_PICKER, DOW_PICKER, TONE_PICKER, SNOOZE_PICKER, VOLUME_FADE_PICKER, DELETE_CONFIRM }

  private long alarmId;
  private AlarmClockServiceBinder service;
  private DbAccessor db;
  private AlarmInfo originalInfo;
  private AlarmInfo info;
  private AlarmSettings originalSettings;
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

    // TODO(cgallek): Is there a way to make the originals final??
    originalInfo = db.readAlarmInfo(alarmId);
    if (originalInfo != null) {
      info = new AlarmInfo(originalInfo);
    }
    originalSettings = db.readAlarmSettings(alarmId);
    settings = new AlarmSettings(originalSettings);

    Button okButton = (Button) findViewById(R.id.settings_ok);
    okButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        if (originalInfo != null && !originalInfo.equals(info)) {
          db.writeAlarmInfo(alarmId, info);
          if (info.enabled()) {
            service.scheduleAlarm(alarmId);
          }
        }
        if (!originalSettings.equals(settings)) {
          db.writeAlarmSettings(alarmId, settings);
        }
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
        showDialog(Dialogs.DELETE_CONFIRM.ordinal());
      }
    });

    ListView alarmInfoList = (ListView) findViewById(R.id.alarm_info_list);
    if (alarmId != AlarmSettings.DEFAULT_SETTINGS_ID) {
      Setting[] alarmInfoObjects = new Setting[AlarmInfoType.values().length];
      alarmInfoObjects[AlarmInfoType.TIME.ordinal()] = new Setting() {
        @Override
        public String name() { return getString(R.string.time); }
        @Override
        public String value() { return info.getTime().localizedString(getApplicationContext()); }
      };
      alarmInfoObjects[AlarmInfoType.NAME.ordinal()] = new Setting() {
        @Override
        public String name() { return getString(R.string.label); }
        @Override
        public String value() { return info.getName(); }
      };
      alarmInfoObjects[AlarmInfoType.DAYS_OF_WEEK.ordinal()] = new Setting() {
        @Override
        public String name() { return getString(R.string.repeat); }
        @Override
        public String value() { return info.getTime().getDaysOfWeek().toString(getApplicationContext()); }
      };
      alarmInfoAdapter = new SettingsAdapter(getApplicationContext(), alarmInfoObjects);
      alarmInfoList.setAdapter(alarmInfoAdapter);
      alarmInfoList.setOnItemClickListener(new AlarmInfoListClickListener());
    }

    Setting[] settingsObjects = new Setting[SettingType.values().length];
    settingsObjects[SettingType.TONE.ordinal()] = new Setting() {
      @Override
      public String name() { return getString(R.string.tone); }
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
      public String name() { return getString(R.string.snooze_minutes); }
      @Override
      public String value() { return "" + settings.getSnoozeMinutes(); }      
    };
    settingsObjects[SettingType.VIBRATE.ordinal()] = new Setting() {
      @Override
      public String name() { return getString(R.string.vibrate); }
      @Override
      public String value() { return settings.getVibrate() ? getString(R.string.enabled) : getString(R.string.disabled); }
    };
    settingsObjects[SettingType.VOLUME_FADE.ordinal()] = new Setting() {
      @Override
      public String name() { return getString(R.string.alarm_fade); }
      @Override
      public String value() { return getString(R.string.fade_description, settings.getVolumeStartPercent(), settings.getVolumeEndPercent(), settings.getVolumeChangeTimeSec()); }
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
  protected void onResume() {
    super.onResume();
    service.bind();
  }

  @Override
  protected void onPause() {
    super.onPause();
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
        String name = tone != null ? tone.getTitle(c) : getString(R.string.unknown_name);
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
        AlarmTime time = info.getTime();
        int hour = time.calendar().get(Calendar.HOUR_OF_DAY);
        int minute = time.calendar().get(Calendar.MINUTE);
        boolean is24Hour = DateFormat.is24HourFormat(getApplicationContext());
        return new TimePickerDialog(this,
            new TimePickerDialog.OnTimeSetListener() {
              @Override
              public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                info.setTime(new AlarmTime(hourOfDay, minute, 0));
                alarmInfoAdapter.notifyDataSetChanged();
              }
            },
            hour, minute, is24Hour);
      case NAME_PICKER:
        AlertDialog.Builder nameBuilder = new AlertDialog.Builder(this);
        nameBuilder.setTitle(R.string.alarm_label);
        View nameView = getLayoutInflater().inflate(R.layout.name_settings_dialog, null);
        final TextView label = (TextView) nameView.findViewById(R.id.name_label);
        label.setText(info.getName());
        nameBuilder.setView(nameView);
        nameBuilder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            info.setName(label.getEditableText().toString());
            alarmInfoAdapter.notifyDataSetChanged();
            dismissDialog(Dialogs.NAME_PICKER.ordinal());
          }
        });
        nameBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            dismissDialog(Dialogs.NAME_PICKER.ordinal());
          }
        });
        return nameBuilder.create();
      case DOW_PICKER:
        AlertDialog.Builder dowBuilder = new AlertDialog.Builder(this);
        dowBuilder.setTitle(R.string.scheduled_days);
        dowBuilder.setMultiChoiceItems(
            info.getTime().getDaysOfWeek().names(getApplicationContext()),
            info.getTime().getDaysOfWeek().bitmask(),
            new OnMultiChoiceClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                if (isChecked) {
                  info.getTime().getDaysOfWeek().addDay(Week.Day.values()[which]);
                } else {
                  info.getTime().getDaysOfWeek().removeDay(Week.Day.values()[which]);
                }
              }
        });
        dowBuilder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            alarmInfoAdapter.notifyDataSetChanged();
            dismissDialog(Dialogs.DOW_PICKER.ordinal());
          }
        });
        return dowBuilder.create();

      case SNOOZE_PICKER:
        final CharSequence[] items = new CharSequence[60];
        for (int i = 1; i <= 60; ++i) {
          items[i-1] = new Integer(i).toString();
        }
        AlertDialog.Builder snoozeBuilder = new AlertDialog.Builder(this);
        snoozeBuilder.setTitle(R.string.snooze_minutes);
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
        fadeBuilder.setTitle(R.string.alarm_fade);
        View fadeView = getLayoutInflater().inflate(R.layout.fade_settings_dialog, null);
        final EditText volumeStart = (EditText) fadeView.findViewById(R.id.volume_start);
        volumeStart.setText("" + settings.getVolumeStartPercent());
        final EditText volumeEnd = (EditText) fadeView.findViewById(R.id.volume_end);
        volumeEnd.setText("" + settings.getVolumeEndPercent());
        final EditText volumeDuration = (EditText) fadeView.findViewById(R.id.volume_duration);
        volumeDuration.setText("" + settings.getVolumeChangeTimeSec());
        fadeBuilder.setView(fadeView);
        fadeBuilder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            settings.setVolumeStartPercent(Integer.parseInt(volumeStart.getText().toString()));
            settings.setVolumeEndPercent(Integer.parseInt(volumeEnd.getText().toString()));
            settings.setVolumeChangeTimeSec(Integer.parseInt(volumeDuration.getText().toString()));
            settingsAdapter.notifyDataSetChanged();
            dismissDialog(Dialogs.VOLUME_FADE_PICKER.ordinal());
          }
        });
        fadeBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            dismissDialog(Dialogs.VOLUME_FADE_PICKER.ordinal());
          }
        });
        return fadeBuilder.create();
      case DELETE_CONFIRM:
        AlertDialog.Builder deleteConfirmBuilder = new AlertDialog.Builder(this);
        deleteConfirmBuilder.setTitle(R.string.delete);
        deleteConfirmBuilder.setMessage(R.string.confirm_delete);
        deleteConfirmBuilder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            service.deleteAlarm(alarmId);
            dismissDialog(Dialogs.DELETE_CONFIRM.ordinal());
            finish();
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
          i.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getString(R.string.alarm_tone));
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
