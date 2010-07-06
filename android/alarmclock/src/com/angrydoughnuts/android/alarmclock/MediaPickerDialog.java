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

import com.angrydoughnuts.android.alarmclock.MediaListView.OnItemPickListener;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Message;
import android.provider.MediaStore.Audio.Albums;
import android.provider.MediaStore.Audio.Artists;
import android.provider.MediaStore.Audio.Media;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.ViewFlipper;
import android.widget.TabHost.OnTabChangeListener;

/**
 * A dialog which displays all of the audio media available on the phone.
 * It allows you to access media through 4 tabs:  One that lists media
 * stored internally on the phone, and three that allow you to access
 * the media stored on the SD card.  These three tabs allow you to browse by
 * artist, album, and song. 
 */
public class MediaPickerDialog extends AlertDialog {
  public interface OnMediaPickListener {
    public void onMediaPick(String name, Uri media);
  }

  private final String INTERNAL_TAB = "internal";
  private final String ARTISTS_TAB = "artists";
  private final String ALBUMS_TAB = "albums";
  private final String ALL_SONGS_TAB = "songs";

  private String selectedName;
  private Uri selectedUri;
  private OnMediaPickListener pickListener;
  private MediaPlayer mediaPlayer;

  public MediaPickerDialog(final Activity context) {
    super(context);
    mediaPlayer = new MediaPlayer();

    final LayoutInflater inflater =
      (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    final View body_view = inflater.inflate(R.layout.media_picker_dialog, null);
    setView(body_view);

    TabHost tabs = (TabHost) body_view.findViewById(R.id.media_tabs);
    tabs.setup();

    tabs.addTab(tabs.newTabSpec(INTERNAL_TAB).setContent(R.id.media_picker_internal).setIndicator(context.getString(R.string.internal)));
    tabs.addTab(tabs.newTabSpec(ARTISTS_TAB).setContent(R.id.media_picker_artists).setIndicator(context.getString(R.string.artists)));
    tabs.addTab(tabs.newTabSpec(ALBUMS_TAB).setContent(R.id.media_picker_albums).setIndicator(context.getString(R.string.albums)));
    tabs.addTab(tabs.newTabSpec(ALL_SONGS_TAB).setContent(R.id.media_picker_songs).setIndicator(context.getString(R.string.songs)));

    final TextView lastSelected = (TextView) body_view.findViewById(R.id.media_picker_status);
    final OnItemPickListener listener = new OnItemPickListener() {
      @Override
      public void onItemPick(Uri uri, String name) {
        selectedUri = uri;
        selectedName = name;
        lastSelected.setText(name);
      }
    };

    final MediaSongsView internalList = (MediaSongsView) body_view.findViewById(R.id.media_picker_internal);
    internalList.setCursorManager(context);
    internalList.includeDefault();
    internalList.query(Media.INTERNAL_CONTENT_URI);
    internalList.setMediaPlayer(mediaPlayer);
    internalList.setMediaPickListener(listener);

    final MediaSongsView songsList = (MediaSongsView) body_view.findViewById(R.id.media_picker_songs);
    songsList.setCursorManager(context);
    songsList.query(Media.EXTERNAL_CONTENT_URI);
    songsList.setMediaPlayer(mediaPlayer);
    songsList.setMediaPickListener(listener);

    final ViewFlipper artistsFlipper = (ViewFlipper) body_view.findViewById(R.id.media_picker_artists);
    final MediaArtistsView artistsList = new MediaArtistsView(context);
    artistsList.setCursorManager(context);
    artistsList.addToFlipper(artistsFlipper);
    artistsList.query(Artists.EXTERNAL_CONTENT_URI);
    artistsList.setMediaPlayer(mediaPlayer);
    artistsList.setMediaPickListener(listener);

    final ViewFlipper albumsFlipper = (ViewFlipper) body_view.findViewById(R.id.media_picker_albums);
    final MediaAlbumsView albumsList = new MediaAlbumsView(context);
    albumsList.setCursorManager(context);
    albumsList.addToFlipper(albumsFlipper);
    albumsList.query(Albums.EXTERNAL_CONTENT_URI);
    albumsList.setMediaPlayer(mediaPlayer);
    albumsList.setMediaPickListener(listener);

    tabs.setOnTabChangedListener(new OnTabChangeListener() {
      @Override
      public void onTabChanged(String tabId) {
        if (tabId.equals(ARTISTS_TAB)) {
          artistsFlipper.setDisplayedChild(0);
        } else if (tabId.equals(ALBUMS_TAB)) {
          albumsFlipper.setDisplayedChild(0);
        }
      }
    });

    super.setButton(BUTTON_POSITIVE, getContext().getString(R.string.ok),
      new OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          if (selectedUri == null || pickListener == null) {
            cancel();
            return;
          }
          pickListener.onMediaPick(selectedName, selectedUri);
        }
    });

    super.setButton(BUTTON_NEGATIVE, getContext().getString(R.string.cancel),
        new OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            selectedName = null;
            selectedUri = null;
            lastSelected.setText("");
            cancel();
          }
      });
  }

  public void setPickListener(OnMediaPickListener listener) {
    this.pickListener = listener;
  }

  @Override
  protected void onStop() {
    super.onStop();
    mediaPlayer.stop();
  }

  @Override
  protected void finalize() throws Throwable {
    mediaPlayer.release();
    super.finalize();
  }

  // Make these no-ops and final so the buttons can't be overridden buy the
  // user nor a child.
  @Override
  public void setButton(CharSequence text, Message msg) {  }
  @Override
  public final void setButton(CharSequence text, OnClickListener listener) {}
  @Override
  public final void setButton(int whichButton, CharSequence text, Message msg) {}
  @Override
  public final void setButton(int whichButton, CharSequence text, OnClickListener listener) {}
  @Override
  public final void setButton2(CharSequence text, Message msg) {}
  @Override
  public final void setButton2(CharSequence text, OnClickListener listener) {}
  @Override
  public final void setButton3(CharSequence text, Message msg) {}
  @Override
  public final void setButton3(CharSequence text, OnClickListener listener) {}
}
