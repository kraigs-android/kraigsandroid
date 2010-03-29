package com.angrydoughnuts.android.alarmclock;

import android.content.Context;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.provider.MediaStore.Audio.AlbumColumns;
import android.provider.MediaStore.Audio.Media;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ViewFlipper;

public class MediaAlbumsView extends MediaListView {
  private final String[] albumsColumns = new String[] {
    AlbumColumns.ALBUM,
    AlbumColumns.ALBUM_KEY
  };

  private final int[] albumsResIDs = new int[] {
    R.id.media_value,
    R.id.media_key
  };

  private MediaSongsView songsView;

  public MediaAlbumsView(Context context) {
    this(context, null);
  }

  public MediaAlbumsView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public MediaAlbumsView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    songsView = new MediaSongsView(context);
    songsView.overrideSortOrder(null);
  }

  @Override
  public void addToFlipper(ViewFlipper flipper) {
    super.addToFlipper(flipper);
    songsView.addToFlipper(flipper);
  }

  public void setMediaPlayer(MediaPlayer mPlayer) {
    songsView.setMediaPlayer(mPlayer);
  }

  public Cursor query(Uri contentUri) {
    return query(contentUri, null);
  }

  public Cursor query(Uri contentUri, String selection) {
    return super.query(contentUri, AlbumColumns.ALBUM_KEY, selection, R.layout.media_picker_row, albumsColumns, albumsResIDs);
  }

  @Override
  public void setMediaPickListener(MediaPickListener listener) {
    songsView.setMediaPickListener(listener);
  }

  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    super.onItemClick(parent, view, position, id);
    // TODO(cgallek) manage this cursor.
    songsView.query(Media.EXTERNAL_CONTENT_URI, AlbumColumns.ALBUM_KEY + " = '" + getLastSelectedName() + "'");
    getFlipper().setInAnimation(getContext(), R.anim.slide_in_left);
    getFlipper().setOutAnimation(getContext(), R.anim.slide_out_left);
    getFlipper().showNext();
  }
}
