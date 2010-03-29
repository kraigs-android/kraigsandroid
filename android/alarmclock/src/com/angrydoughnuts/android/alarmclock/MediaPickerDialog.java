package com.angrydoughnuts.android.alarmclock;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.MediaStore.MediaColumns;
import android.provider.MediaStore.Audio.AlbumColumns;
import android.provider.MediaStore.Audio.Albums;
import android.provider.MediaStore.Audio.ArtistColumns;
import android.provider.MediaStore.Audio.Artists;
import android.provider.MediaStore.Audio.AudioColumns;
import android.provider.MediaStore.Audio.Media;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

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
    tabs.addTab(tabs.newTabSpec("artist").setContent(R.id.media_picker_artists).setIndicator("Artist"));
    tabs.addTab(tabs.newTabSpec("albums").setContent(R.id.media_picker_albums).setIndicator("Album"));
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

    final String[] artistColumns = new String[] {
        BaseColumns._ID,
        ArtistColumns.ARTIST,
      };
    final int[] artistLayoutIds = new int[] {
        R.id.media_id,
        R.id.media_artist,
      };

    Cursor artists_cursor = context.managedQuery(Artists.EXTERNAL_CONTENT_URI, artistColumns, null, null, null);
    SimpleCursorAdapter artists_adapter = new SimpleCursorAdapter(context, R.layout.media_picker_row, artists_cursor, artistColumns, artistLayoutIds);
    ListView artists_view = (ListView) body_view.findViewById(R.id.media_picker_artists);
    artists_view.setAdapter(artists_adapter);

    final String[] albumsColumns = new String[] {
        BaseColumns._ID,
        AlbumColumns.ARTIST,
        AlbumColumns.ALBUM,
        //AlbumColumns.ALBUM_ART,
      };
    final int[] albumsLayoutIds = new int[] {
        R.id.media_id,
        R.id.media_artist,
        R.id.media_album
      };

    Cursor albums_cursor = context.managedQuery(Albums.EXTERNAL_CONTENT_URI, albumsColumns, null, null, null);
    SimpleCursorAdapter albums_adapter = new SimpleCursorAdapter(context, R.layout.media_picker_row, albums_cursor, albumsColumns, albumsLayoutIds);
    ListView albums_view = (ListView) body_view.findViewById(R.id.media_picker_albums);
    albums_view.setAdapter(albums_adapter);

    final Cursor songs_cursor = context.managedQuery(Media.EXTERNAL_CONTENT_URI, null, null, null, null);
    CursorAdapter songs_adapter = new CursorAdapter(context, songs_cursor, true) {
      @Override
      public void bindView(View view, Context context, Cursor cursor) {
        TextView id = (TextView) view.findViewById(R.id.media_id);
        TextView title = (TextView) view.findViewById(R.id.media_title);
        TextView artist = (TextView) view.findViewById(R.id.media_artist);
        TextView album = (TextView) view.findViewById(R.id.media_album);

        id.setText(cursor.getString(cursor.getColumnIndex(BaseColumns._ID)));
        title.setText(cursor.getString(cursor.getColumnIndex(MediaColumns.TITLE)));
        artist.setText(cursor.getString(cursor.getColumnIndex(AudioColumns.ARTIST)));
        album.setText(cursor.getString(cursor.getColumnIndex(AudioColumns.ALBUM)));
      }
      @Override
      public View newView(Context context, Cursor cursor, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        return inflater.inflate(R.layout.media_picker_row, parent, false);
      }
    };

    ListView songs_view = (ListView) body_view.findViewById(R.id.media_picker_songs);
    songs_view.setAdapter(songs_adapter);
    songs_view.setOnItemClickListener(new OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        songs_cursor.moveToPosition(position);
        String titleStr = songs_cursor.getString(songs_cursor.getColumnIndex(MediaColumns.TITLE));
        Toast.makeText(getContext(),
            "TITLE: " + titleStr +
            "\nURI: " + Uri.withAppendedPath(Media.EXTERNAL_CONTENT_URI, songs_cursor.getString(songs_cursor.getColumnIndex(BaseColumns._ID))).toString(),
            Toast.LENGTH_SHORT).show();
        
      }
    });
  }
}
