package com.angrydoughnuts.android.alarmclock;

import java.util.ArrayList;
import java.util.Arrays;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.ViewFlipper;
import android.widget.AdapterView.OnItemClickListener;

public class MediaListView extends ListView implements OnItemClickListener {
  public interface MediaPickListener {
    public void onMediaPick(Uri uri, String name);
  }

  private Cursor cursor;
  private MediaPlayer mPlayer;
  private ViewFlipper flipper;
  private Activity cursorManager;
  private Uri contentUri;
  private String nameColumn;
  private String sortOrder;
  private MediaPickListener listener;

  private String selectedName;
  private Uri selectedUri;

  public MediaListView(Context context) {
    this(context, null);
  }

  public MediaListView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public MediaListView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    this.setOnKeyListener(new OnKeyListener() {
      @Override
      public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (flipper == null || flipper.getDisplayedChild() == 0) {
          return false;
        }
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
          if (event.getAction() == KeyEvent.ACTION_UP) {
            if (mPlayer != null) {
              mPlayer.stop();
            }
            flipper.setInAnimation(getContext(), R.anim.slide_in_right);
            flipper.setOutAnimation(getContext(), R.anim.slide_out_right);
            flipper.showPrevious();
          }
          return true;
        }
        return false;
      }
    });
  }

  public void setMediaPlayer(MediaPlayer mPlayer) {
    this.mPlayer = mPlayer;
  }

  protected MediaPlayer getMediaPlayer() {
    return mPlayer;
  }

  public void addToFlipper(ViewFlipper flipper) {
    this.flipper = flipper;
    flipper.setAnimateFirstView(false);
    flipper.addView(this);
  }

  protected ViewFlipper getFlipper() {
    return flipper;
  }

  public void setCursorManager(Activity activity) {
    this.cursorManager = activity;
  }

  protected void manageCursor(Cursor cursor) {
    cursorManager.startManagingCursor(cursor);
  }

  protected void query(Uri contentUri, String nameColumn,
      int rowResId, String[] displayColumns, int[] resIDs) {
    query(contentUri, nameColumn, null, rowResId, displayColumns, resIDs);
  }

  protected void query(Uri contentUri, String nameColumn, String selection,
      int rowResId, String[] displayColumns, int[] resIDs) {
    this.nameColumn = nameColumn;
    final ArrayList<String> queryColumns =
      new ArrayList<String>(displayColumns.length + 1);
    queryColumns.addAll(Arrays.asList(displayColumns));
    // The ID column is required for the SimpleCursorAdapter.  Make sure to
    // add it if it's not already there.
    if (!queryColumns.contains(BaseColumns._ID)) {
      queryColumns.add(BaseColumns._ID);
    }

    cursor = getContext().getContentResolver().query(
        contentUri, queryColumns.toArray(new String[] {}),
        selection, null, sortOrder);
    manageCursor(cursor);
    this.contentUri = contentUri;

    final SimpleCursorAdapter adapter = new SimpleCursorAdapter(
        getContext(), rowResId, cursor, displayColumns, resIDs);
    setAdapter(adapter);
    setOnItemClickListener(this);
  }

  public void overrideSortOrder(String sortOrder) {
    this.sortOrder = sortOrder;
  }

  public String getLastSelectedName() {
    return selectedName;
  }

  public Uri getLastSelectedUri() {
    return selectedUri;
  }

  public void setMediaPickListener(MediaPickListener listener) {
    this.listener = listener;
  }

  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    cursor.moveToPosition(position);
    selectedName = cursor.getString(cursor.getColumnIndex(nameColumn));
    selectedUri = Uri.withAppendedPath(contentUri, cursor.getString(cursor.getColumnIndex(BaseColumns._ID)));
    if (listener != null) {
      listener.onMediaPick(selectedUri, selectedName);
    }
  }
}