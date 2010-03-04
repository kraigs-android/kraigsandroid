package com.angrydoughnuts.android.alarmclock;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
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
import android.widget.AdapterView.OnItemClickListener;

public class ActivitySettings extends Activity {
  static public final String EXTRAS_ALARM_ID = "alarm_id";

  private enum SettingType { TONE, SNOOZE, VIBRATE, VOLUME_FADE; }

  private final int MISSING_EXTRAS = -69;
  // TODO(cgallek): make these an emum?
  private final int TONE_PICK_ID = 1;
  private final int SNOOZE_PICK_ID = 2;
  private final int VOLUME_FADE_PICK_ID = 3;

  private long alarmId;
  private AlarmClockServiceBinder service;
  private DbAccessor db;
  private AlarmSettings settings;
  SettingsAdapter adapter;

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

    settings = db.readAlarmSettings(alarmId);

    Button okButton = (Button) findViewById(R.id.settings_ok);
    okButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
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
    adapter = new SettingsAdapter(getApplicationContext(), settingsObjects);
    settingsList.setAdapter(adapter);
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

    switch (requestCode) {
      case TONE_PICK_ID:
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
        adapter.notifyDataSetChanged();
      default:
        super.onActivityResult(requestCode, resultCode, data);
    }
  }

  @Override
  protected Dialog onCreateDialog(int id) {
    switch (id) {
      case SNOOZE_PICK_ID:
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
            adapter.notifyDataSetChanged();
            dismissDialog(SNOOZE_PICK_ID);
          }
        });
        return snoozeBuilder.create();
      case VOLUME_FADE_PICK_ID:
        AlertDialog.Builder fadeBuilder = new AlertDialog.Builder(this);
        // TODO(cgallek): move this to strings.xml
        fadeBuilder.setTitle("Alarm Volume Fade");
        View view = getLayoutInflater().inflate(R.layout.fade_settings_dialog, null);
        final EditText volumeStart = (EditText) view.findViewById(R.id.volume_start);
        volumeStart.setText("" + settings.getVolumeStartPercent());
        final EditText volumeEnd = (EditText) view.findViewById(R.id.volume_end);
        volumeEnd.setText("" + settings.getVolumeEndPercent());
        final EditText volumeDuration = (EditText) view.findViewById(R.id.volume_duration);
        volumeDuration.setText("" + settings.getVolumeChangeTimeSec());
        fadeBuilder.setView(view);
        fadeBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            settings.setVolumeStartPercent(Integer.parseInt(volumeStart.getText().toString()));
            settings.setVolumeEndPercent(Integer.parseInt(volumeEnd.getText().toString()));
            settings.setVolumeChangeTimeSec(Integer.parseInt(volumeDuration.getText().toString()));
            adapter.notifyDataSetChanged();
            dismissDialog(VOLUME_FADE_PICK_ID);
          }
        });
        fadeBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            dismissDialog(VOLUME_FADE_PICK_ID);
          }
        });
        return fadeBuilder.create();
      default:
        return super.onCreateDialog(id);
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
          startActivityForResult(i, TONE_PICK_ID);
          break;
        case SNOOZE:
          showDialog(SNOOZE_PICK_ID);
          break;
        case VIBRATE:
          settings.setVibrate(!settings.getVibrate());
          adapter.notifyDataSetChanged();
          break;
        case VOLUME_FADE:
          showDialog(VOLUME_FADE_PICK_ID);
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
