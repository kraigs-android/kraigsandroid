package com.angrydoughnuts.android.alarmclock;


import android.content.Context;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.provider.MediaStore.MediaColumns;
import android.provider.MediaStore.Audio.AudioColumns;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView;
import android.widget.Toast;

public class MediaSongsView extends MediaListView implements OnItemClickListener {
  private final String[] songsColumns = new String[] {
    MediaColumns.TITLE,
    AudioColumns.ARTIST,
    AudioColumns.ALBUM
  };

  final int[] songsResIDs = new int[] {
      R.id.media_title,
      R.id.media_artist,
      R.id.media_album
  };

  public MediaSongsView(Context context) {
    this(context, null);
  }

  public MediaSongsView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public MediaSongsView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

  public Cursor query(Uri contentUri) {
    return query(contentUri, null);
  }

  public Cursor query(Uri contentUri, String selection) {
    return super.query(contentUri, MediaColumns.TITLE, selection, R.layout.media_picker_sound_row, songsColumns, songsResIDs);
  }

  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    super.onItemClick(parent, view, position, id);

    Toast.makeText(getContext(),
        "TITLE: " + getLastSelectedName() +
        "\nURI: " + getLastSelectedUri().toString(),
        Toast.LENGTH_SHORT).show();
    MediaPlayer mPlayer = getMediaPlayer();
    if (mPlayer == null) {
      return;
    }
    mPlayer.reset();
    try {
      mPlayer.setDataSource(getContext(), getLastSelectedUri());
      mPlayer.prepare();
      mPlayer.start();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
