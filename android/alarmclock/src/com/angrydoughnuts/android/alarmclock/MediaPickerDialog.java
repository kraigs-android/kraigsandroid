package com.angrydoughnuts.android.alarmclock;

import com.angrydoughnuts.android.alarmclock.MediaListView.OnItemClickListener;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.media.MediaPlayer;
import android.net.Uri;
import android.provider.MediaStore.MediaColumns;
import android.provider.MediaStore.Audio.AlbumColumns;
import android.provider.MediaStore.Audio.Albums;
import android.provider.MediaStore.Audio.ArtistColumns;
import android.provider.MediaStore.Audio.Artists;
import android.provider.MediaStore.Audio.AudioColumns;
import android.provider.MediaStore.Audio.Media;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TabHost;
import android.widget.Toast;
import android.widget.ViewFlipper;
import android.widget.TabHost.OnTabChangeListener;

// TODO(cgallek): maybe make this an activity instead of a dialog?
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

    // TODO(cgallek): move this to strings.xml
    tabs.addTab(tabs.newTabSpec(INTERNAL_TAB).setContent(R.id.media_picker_internal).setIndicator("Internal"));
    tabs.addTab(tabs.newTabSpec(ARTISTS_TAB).setContent(R.id.media_picker_artists).setIndicator("Artist"));
    tabs.addTab(tabs.newTabSpec(ALBUMS_TAB).setContent(R.id.media_picker_albums).setIndicator("Album"));
    tabs.addTab(tabs.newTabSpec(ALL_SONGS_TAB).setContent(R.id.media_picker_songs).setIndicator("Songs"));

    final OnItemClickListener songPickListener = new OnItemClickListener() {
      @Override
      public void onItemClick(String name, Uri media) {
        selectedName = name;
        selectedUri = media;
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
      R.id.media_value,
    };
    MediaListView internalList = (MediaListView) body_view.findViewById(R.id.media_picker_internal);
    context.startManagingCursor(
      internalList.query(Media.INTERNAL_CONTENT_URI, R.layout.media_picker_row,
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
      songList.query(Media.EXTERNAL_CONTENT_URI, R.layout.media_picker_sound_row,
      songsColumns, songsResIDs));
    songList.setOnItemClickListener(MediaColumns.TITLE, songPickListener);

    final String[] artistsColumns = new String[] {
      ArtistColumns.ARTIST,
      ArtistColumns.ARTIST_KEY
    };
    final int[] artistsResIDs = new int[] {
      R.id.media_value,
      R.id.media_key
    };
    final String[] albumsColumns = new String[] {
      AlbumColumns.ALBUM,
      AlbumColumns.ALBUM_KEY
    };
    final int[] albumsResIDs = new int[] {
      R.id.media_value,
      R.id.media_key
    };

    final ViewFlipper artistsFlipper = (ViewFlipper) body_view.findViewById(R.id.media_picker_artists);
    artistsFlipper.setAnimateFirstView(false);
    final MediaListView artistsList = new MediaListView(context);
    artistsFlipper.addView(artistsList);
    final MediaListView artistsAlbumList = new MediaListView(context);
    artistsFlipper.addView(artistsAlbumList);
    final MediaListView artistsAlbumSongList = new MediaListView(context);
    artistsFlipper.addView(artistsAlbumSongList);

    context.startManagingCursor(
      artistsList.query(Artists.EXTERNAL_CONTENT_URI, R.layout.media_picker_row,
      artistsColumns, artistsResIDs));
    artistsList.setOnItemClickListener(ArtistColumns.ARTIST_KEY,
      new OnItemClickListener() {
        @Override
        public void onItemClick(String name, Uri media) {
          // TODO(cgallek): these cursors can be re-used.
          context.startManagingCursor(
              artistsAlbumList.query(Albums.EXTERNAL_CONTENT_URI, ArtistColumns.ARTIST_KEY + " = '" + name + "'", R.layout.media_picker_row, albumsColumns, albumsResIDs));
          artistsFlipper.showNext();
        }
    });
    artistsAlbumList.setOnItemClickListener(AlbumColumns.ALBUM_KEY,
        new OnItemClickListener() {
          @Override
          public void onItemClick(String name, Uri media) {
            context.startManagingCursor(
                artistsAlbumSongList.query(Media.EXTERNAL_CONTENT_URI, AlbumColumns.ALBUM_KEY + " = '" + name + "'", R.layout.media_picker_sound_row,
                songsColumns, songsResIDs));
            artistsFlipper.showNext();
          }
      });
    artistsAlbumList.setOnKeyListener(new View.OnKeyListener() {
      @Override
      public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
          if (event.getAction() == KeyEvent.ACTION_UP) {
            artistsFlipper.showPrevious();
            mediaPlayer.stop();
          }
          return true;
        } else {
          return false;
        }
      }
    });

    artistsAlbumSongList.setOnItemClickListener(MediaColumns.TITLE, songPickListener);
    artistsAlbumSongList.setOnKeyListener(new View.OnKeyListener() {
      @Override
      public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
          if (event.getAction() == KeyEvent.ACTION_UP) {
            artistsFlipper.showPrevious();
            mediaPlayer.stop();
          }
          return true;
        } else {
          return false;
        }
      }
    });

    MediaListView albumsList = (MediaListView) body_view.findViewById(R.id.media_picker_albums);
    context.startManagingCursor(
      albumsList.query(Albums.EXTERNAL_CONTENT_URI, R.layout.media_picker_row,
      albumsColumns, albumsResIDs));
    albumsList.setOnItemClickListener(AlbumColumns.ALBUM_KEY,
      new OnItemClickListener() {
        @Override
        public void onItemClick(String name, Uri media) {
          Toast.makeText(getContext(),
              "Key: " + name +
              "\nURI: " + media.toString(),
              Toast.LENGTH_SHORT).show();
        }
    });

    tabs.setOnTabChangedListener(new OnTabChangeListener() {
      @Override
      public void onTabChanged(String tabId) {
        if (tabId.equals(ARTISTS_TAB)) {
          artistsFlipper.setDisplayedChild(0);
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
