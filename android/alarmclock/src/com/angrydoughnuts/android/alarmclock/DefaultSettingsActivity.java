package com.angrydoughnuts.android.alarmclock;

import android.app.Activity;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class DefaultSettingsActivity extends Activity {
  private final int TONE_PICK_ID = 1;

  private DbAccessor db;
  private TextView tone;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.default_settings);
    db = new DbAccessor(getApplicationContext());

    tone = (TextView) this.findViewById(R.id.tone);
    tone.setText(db.readDefaultAlarmSettings());
    tone.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        // TODO(cgallek): this is the wrong ringtone.
        Uri current_tone = Uri.parse(db.readDefaultAlarmSettings());
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
          uri = Settings.System.DEFAULT_RINGTONE_URI;
        }
        tone.setText(uri.toString());
        db.writeAlarmSettings(-1, uri.toString());
      default:
        super.onActivityResult(requestCode, resultCode, data);
    }
  }
}
