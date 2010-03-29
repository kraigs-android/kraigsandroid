package com.angrydoughnuts.android.alarmclock;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.media.MediaPlayer;
import android.net.Uri;
import android.provider.MediaStore.Audio.Albums;
import android.provider.MediaStore.Audio.Artists;
import android.provider.MediaStore.Audio.Media;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TabHost;
import android.widget.ViewFlipper;
import android.widget.TabHost.OnTabChangeListener;

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

    final MediaSongsView internalList = (MediaSongsView) body_view.findViewById(R.id.media_picker_internal);
    internalList.query(Media.INTERNAL_CONTENT_URI);
    internalList.setMediaPlayer(mediaPlayer);

    final MediaSongsView songsList = (MediaSongsView) body_view.findViewById(R.id.media_picker_songs);
    // TODO(cgallek): this returns a cursor.  Who manages it?
    songsList.query(Media.EXTERNAL_CONTENT_URI);
    songsList.setMediaPlayer(mediaPlayer);

    final ViewFlipper artistsFlipper = (ViewFlipper) body_view.findViewById(R.id.media_picker_artists);
    MediaArtistsView artistsList = new MediaArtistsView(context);
    artistsList.addToFlipper(artistsFlipper);
    artistsList.query(Artists.EXTERNAL_CONTENT_URI);
    artistsList.setMediaPlayer(mediaPlayer);

    final ViewFlipper albumsFlipper = (ViewFlipper) body_view.findViewById(R.id.media_picker_albums);
    final MediaAlbumsView albumsList = new MediaAlbumsView(context);
    albumsList.addToFlipper(albumsFlipper);
    albumsList.query(Albums.EXTERNAL_CONTENT_URI);
    albumsList.setMediaPlayer(mediaPlayer);

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

    // TODO(cgallek) make these methods final or something so
    // callers can't accidentally set them.
    setButton(BUTTON_POSITIVE, getContext().getString(R.string.ok),
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

    setButton(BUTTON_NEGATIVE, getContext().getString(R.string.cancel),
        new OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            selectedName = null;
            selectedUri = null;
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
    // TODO(cgallek): not sure where to put this...
    //mediaPlayer.release();
  }
}
