package com.angrydoughnuts.android.alarmclock;

import android.content.Context;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.provider.MediaStore.Audio.Albums;
import android.provider.MediaStore.Audio.ArtistColumns;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ViewFlipper;

public class MediaArtistsView extends MediaListView {
  private final String[] artistsColumns = new String[] {
    ArtistColumns.ARTIST,
    ArtistColumns.ARTIST_KEY
  };
  
  private final int[] artistsResIDs = new int[] {
    R.id.media_value,
    R.id.media_key
  };

  private MediaAlbumsView albumsView;

  public MediaArtistsView(Context context) {
    this(context, null);
  }

  public MediaArtistsView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public MediaArtistsView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    albumsView = new MediaAlbumsView(context);
  }

  @Override
  public void addToFlipper(ViewFlipper flipper) {
    super.addToFlipper(flipper);
    albumsView.addToFlipper(flipper);
  }

  public void setMediaPlayer(MediaPlayer mPlayer) {
    albumsView.setMediaPlayer(mPlayer);
  }

  public Cursor query(Uri contentUri) {
    return query(contentUri, null);
  }

  public Cursor query(Uri contentUri, String selection) {
    return super.query(contentUri, ArtistColumns.ARTIST_KEY, selection, R.layout.media_picker_row, artistsColumns, artistsResIDs);
  }

  @Override
  public void setMediaPickListener(MediaPickListener listener) {
    albumsView.setMediaPickListener(listener);
  }

  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    super.onItemClick(parent, view, position, id);
    // TODO(cgallek) manage this cursor.
    albumsView.query(Albums.EXTERNAL_CONTENT_URI, ArtistColumns.ARTIST_KEY + " = '" + getLastSelectedName() + "'");
    getFlipper().setInAnimation(getContext(), R.anim.slide_in_left);
    getFlipper().setOutAnimation(getContext(), R.anim.slide_out_left);
    getFlipper().showNext();
  }
}
