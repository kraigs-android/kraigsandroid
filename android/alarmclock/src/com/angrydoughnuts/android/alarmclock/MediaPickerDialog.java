package com.angrydoughnuts.android.alarmclock;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.provider.MediaStore.MediaColumns;
import android.provider.MediaStore.Audio.AudioColumns;
import android.provider.MediaStore.Audio.Media;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TabHost;

// TODO(cgallek): maybe make this an activity instead of a dialog?
public class MediaPickerDialog extends AlertDialog {

  public MediaPickerDialog(Activity context) {
    super(context);
    final LayoutInflater inflater =
      (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    final View body_view = inflater.inflate(R.layout.media_picker_dialog, null);
    setView(body_view);

    TabHost tabs = (TabHost) body_view.findViewById(R.id.media_tabs);
    tabs.setup();

    // TODO(cgallek): move this to strings.xml
    tabs.addTab(tabs.newTabSpec("internal").setContent(R.id.media_picker_internal).setIndicator("Internal"));
    tabs.addTab(tabs.newTabSpec("songs").setContent(R.id.media_picker_songs).setIndicator("Songs"));

    final String[] columns = new String[] {
      BaseColumns._ID,
      MediaColumns.TITLE,
      AudioColumns.ARTIST,
      AudioColumns.ALBUM
    };
    final int[] layoutIds = new int[] {
      R.id.media_id,
      R.id.media_title,
      R.id.media_artist,
      R.id.media_album
    };

    Cursor internal_cursor = context.managedQuery(Media.INTERNAL_CONTENT_URI, columns, null, null, null);
    SimpleCursorAdapter internal_adapter = new SimpleCursorAdapter(context, R.layout.media_picker_row, internal_cursor, columns, layoutIds);
    ListView internal_view = (ListView) body_view.findViewById(R.id.media_picker_internal);
    internal_view.setAdapter(internal_adapter);

    Cursor songs_cursor = context.managedQuery(Media.EXTERNAL_CONTENT_URI, null, null, null, null);
    SimpleCursorAdapter songs_adapter = new SimpleCursorAdapter(context, R.layout.media_picker_row, songs_cursor, columns, layoutIds);
    ListView songs_view = (ListView) body_view.findViewById(R.id.media_picker_songs);
    songs_view.setAdapter(songs_adapter);
  }
}
