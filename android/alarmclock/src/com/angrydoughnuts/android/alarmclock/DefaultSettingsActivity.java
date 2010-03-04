package com.angrydoughnuts.android.alarmclock;

import android.app.Activity;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class DefaultSettingsActivity extends Activity {
  private final int TONE_PICK_ID = 1;

  private TextView tone;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.default_settings);

    tone = (TextView) this.findViewById(R.id.tone);
    tone.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        Intent i = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        i.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
        i.putExtra(RingtoneManager.EXTRA_RINGTONE_INCLUDE_DRM, true);
        i.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Default Alarm Tone");
        startActivityForResult(i, TONE_PICK_ID);
      }
    });
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (resultCode == RESULT_CANCELED) {
      return;
    }

    switch (requestCode) {
      case TONE_PICK_ID:
        Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
        if (uri != null) {
          tone.setText(uri.toString());
        } else {
          tone.setText("none");
        }
      default:
        super.onActivityResult(requestCode, resultCode, data);
    }
  }
}
