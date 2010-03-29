package com.angrydoughnuts.android.alarmclock;

import android.content.Context;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.MediaStore.MediaColumns;
import android.provider.MediaStore.Audio.AudioColumns;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public final class MediaListView extends ListView implements OnItemClickListener {
  private LayoutInflater inflater;
  private Cursor cursor;
  private Uri contentUri;
  private Uri selection;
  private MediaPlayer mediaPlayer;

  public MediaListView(Context context) {
    this(context, null);
  }

  public MediaListView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public MediaListView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    this.inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
  }

  public Cursor query(Uri contentUri, Uri defaultUri) {
    this.cursor = getContext().getContentResolver().query(contentUri, null, null, null, null);
    this.contentUri = contentUri;
    this.selection = defaultUri;

    final CursorAdapter adapter = new CursorAdapter(getContext(), cursor, true) {
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
        return inflater.inflate(R.layout.media_picker_row, parent, false);
      }
    };

    setAdapter(adapter);
    setOnItemClickListener(this);
    return cursor;
  }

  public void setMediaPlayer(MediaPlayer mediaPlayer) {
    this.mediaPlayer = mediaPlayer;
  }

  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    cursor.moveToPosition(position);
    String titleStr = cursor.getString(cursor.getColumnIndex(MediaColumns.TITLE));
    selection = Uri.withAppendedPath(contentUri, cursor.getString(cursor.getColumnIndex(BaseColumns._ID)));
    Toast.makeText(getContext(),
        "TITLE: " + titleStr +
        "\nURI: " + selection.toString(),
        Toast.LENGTH_SHORT).show();

    mediaPlayer.reset();
    try {
      mediaPlayer.setDataSource(getContext(), selection);
      mediaPlayer.prepare();
      mediaPlayer.start();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}