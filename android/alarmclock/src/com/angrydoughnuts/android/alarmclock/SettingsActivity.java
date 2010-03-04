package com.angrydoughnuts.android.alarmclock;

import android.app.Activity;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class SettingsActivity extends Activity {
  static public final String EXTRAS_ALARM_ID = "alarm_id";

  private final int MISSING_EXTRAS = -69;
  private final int TONE_PICK_ID = 1;

  private long alarmId;
  private DbAccessor db;
  private AlarmSettings settings;
  private TextView tone;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.settings);

    alarmId = getIntent().getExtras().getLong(EXTRAS_ALARM_ID, MISSING_EXTRAS);
    if (alarmId == MISSING_EXTRAS) {
      throw new IllegalStateException("EXTRAS_ALARM_ID not supplied in intent.");
    }

    db = new DbAccessor(getApplicationContext());

    settings = db.readAlarmSettings(alarmId);

    tone = (TextView) findViewById(R.id.tone);
    tone.setText(settings.getTone().toString());
    tone.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        // TODO(cgallek): this is the wrong ringtone.
        Uri current_tone = settings.getTone();
        Intent i = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        i.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALL);
        i.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
        // TODO(cgallek): It would be nice to include the default, but the media
        // player doesn't resolve this url...
        i.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, false);
        i.putExtra(RingtoneManager.EXTRA_RINGTONE_INCLUDE_DRM, true);
        i.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, current_tone);
        i.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Default Alarm Tone");
        // TODO(cgallek): Set the initially selected item.
        startActivityForResult(i, TONE_PICK_ID);
      }
    });

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
    if (alarmId == AlarmSettings.DEFAULT_SETTINGS_ID) {
      deleteButton.setVisibility(View.GONE);
    }
    deleteButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        // TODO(cgallek): Add confirmation dialog.
        db.deleteAlarm(alarmId);
        finish();
      }
    });

  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    db.closeConnections();
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (resultCode == RESULT_CANCELED) {
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
          // TODO(cgallek): this url doesn't actually resolve in the media
          // player.  Change it.
          uri = Settings.System.DEFAULT_RINGTONE_URI;
        }
        tone.setText(uri.toString());
        settings.setTone(uri);
      default:
        super.onActivityResult(requestCode, resultCode, data);
    }
  }
}
