package com.angrydoughnuts.android.alarmclock;

import com.angrydoughnuts.android.alarmclock.MediaListView.OnItemClickListener;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.provider.MediaStore.MediaColumns;
import android.provider.MediaStore.Audio.AudioColumns;
import android.provider.MediaStore.Audio.Media;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TabHost;
import android.widget.Toast;

// TODO(cgallek): maybe make this an activity instead of a dialog?
public class MediaPickerDialog extends AlertDialog {
  private MediaPlayer mediaPlayer;

  public MediaPickerDialog(Activity context) {
    super(context);
    mediaPlayer = new MediaPlayer();

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

    final OnItemClickListener songPickListener = new OnItemClickListener() {
      @Override
      public void onItemClick(String name, Uri media) {
        Toast.makeText(getContext(),
            "TITLE: " + name +
            "\nURI: " + media.toString(),
            Toast.LENGTH_SHORT).show();
        mediaPlayer.reset();
        try {
          mediaPlayer.setDataSource(getContext(), media);
          mediaPlayer.prepare();
          mediaPlayer.start();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    };

    final String[] internalSoundColumns = new String[] {
      MediaColumns.TITLE,
    };
    final int[] internalSoundResIDs = new int[] {
      R.id.media_title,
    };
    MediaListView internalList = (MediaListView) body_view.findViewById(R.id.media_picker_internal);
    context.startManagingCursor(
      internalList.query(Media.INTERNAL_CONTENT_URI, R.layout.media_picker_internal_sound_row,
      internalSoundColumns, internalSoundResIDs));
    internalList.setOnItemClickListener(MediaColumns.TITLE, songPickListener);

    final String[] songsColumns = new String[] {
      MediaColumns.TITLE,
      AudioColumns.ARTIST,
      AudioColumns.ALBUM
    };
    final int[] songsResIDs = new int[] {
      R.id.media_title,
      R.id.media_artist,
      R.id.media_album
    };
    MediaListView songList = (MediaListView) body_view.findViewById(R.id.media_picker_songs);
    context.startManagingCursor(
      songList.query(Media.EXTERNAL_CONTENT_URI, R.layout.media_picker_external_sound_row,
      songsColumns, songsResIDs));
    songList.setOnItemClickListener(MediaColumns.TITLE, songPickListener);

    /*
    final String[] artistColumns = new String[] {
        BaseColumns._ID,
        ArtistColumns.ARTIST,
      };
    final int[] artistLayoutIds = new int[] {
        R.id.media_artist,
      };

    Cursor artists_cursor = context.managedQuery(Artists.EXTERNAL_CONTENT_URI, artistColumns, null, null, null);
    SimpleCursorAdapter artists_adapter = new SimpleCursorAdapter(context, R.layout.media_picker_song_row, artists_cursor, artistColumns, artistLayoutIds);
    ListView artists_view = (ListView) body_view.findViewById(R.id.media_picker_artists);
    artists_view.setAdapter(artists_adapter);

    final String[] albumsColumns = new String[] {
        AlbumColumns.ARTIST,
        AlbumColumns.ALBUM,
        //AlbumColumns.ALBUM_ART,
      };
    final int[] albumsLayoutIds = new int[] {
        R.id.media_artist,
        R.id.media_album
      };

    Cursor albums_cursor = context.managedQuery(Albums.EXTERNAL_CONTENT_URI, albumsColumns, null, null, null);
    SimpleCursorAdapter albums_adapter = new SimpleCursorAdapter(context, R.layout.media_picker_song_row, albums_cursor, albumsColumns, albumsLayoutIds);
    ListView albums_view = (ListView) body_view.findViewById(R.id.media_picker_albums);
    albums_view.setAdapter(albums_adapter);
    */
  }

  @Override
  protected void onStop() {
    super.onStop();
    mediaPlayer.stop();
    // TODO(cgallek): not sure where to put this...
    //mediaPlayer.release();
  }
}
