/****************************************************************************
 * Copyright 2010 kraigs.android@gmail.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ****************************************************************************/

package com.angrydoughnuts.android.alarmclock;

import android.app.Activity;
import android.content.Context;
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
  public void setCursorManager(Activity activity) {
    super.setCursorManager(activity);
    albumsView.setCursorManager(activity);
  }

  @Override
  public void addToFlipper(ViewFlipper flipper) {
    super.addToFlipper(flipper);
    albumsView.addToFlipper(flipper);
  }

  public void setMediaPlayer(MediaPlayer mPlayer) {
    albumsView.setMediaPlayer(mPlayer);
  }

  public void query(Uri contentUri) {
    query(contentUri, null);
  }

  public void query(Uri contentUri, String selection) {
    super.query(contentUri, ArtistColumns.ARTIST_KEY, selection, R.layout.media_picker_row, artistsColumns, artistsResIDs);
  }

  @Override
  public void setMediaPickListener(OnItemPickListener listener) {
    albumsView.setMediaPickListener(listener);
  }

  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    super.onItemClick(parent, view, position, id);
    albumsView.query(Albums.EXTERNAL_CONTENT_URI, ArtistColumns.ARTIST_KEY + " = '" + getLastSelectedName() + "'");
    getFlipper().setInAnimation(getContext(), R.anim.slide_in_left);
    getFlipper().setOutAnimation(getContext(), R.anim.slide_out_left);
    getFlipper().showNext();
  }
}
