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

import java.util.ArrayList;
import java.util.Arrays;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.database.MergeCursor;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.ViewFlipper;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.SimpleCursorAdapter.ViewBinder;

/**
 * An extension to the ListView widget specialized for selecting audio media.
 * Use one of the concrete implementations MediaSongsView, MediaArtistsView
 * or MediaAlbumsView.
 */
public class MediaListView extends ListView implements OnItemClickListener {
  public interface OnItemPickListener {
    public void onItemPick(Uri uri, String name);
  }

  protected static int DEFAULT_TONE_INDEX = -69;

  private Cursor cursor;
  private Cursor staticCursor;
  private MediaPlayer mPlayer;
  private ViewFlipper flipper;
  private Activity cursorManager;
  private Uri contentUri;
  private String nameColumn;
  private String sortOrder;
  private OnItemPickListener listener;

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
    setChoiceMode(CHOICE_MODE_SINGLE);
    setOnKeyListener(new OnKeyListener() {
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

    Cursor dbCursor = getContext().getContentResolver().query(
        contentUri, queryColumns.toArray(new String[] {}),
        selection, null, sortOrder);
    if (staticCursor != null) {
      Cursor[] cursors = new Cursor[] { staticCursor, dbCursor };
      cursor = new MergeCursor(cursors);
    } else {
      cursor = dbCursor;
    }
    manageCursor(cursor);

    this.contentUri = contentUri;

    final SimpleCursorAdapter adapter = new SimpleCursorAdapter(
        getContext(), rowResId, cursor, displayColumns, resIDs);
    // Use a custom binder to highlight the selected element.
    adapter.setViewBinder(new ViewBinder() {
      @Override
      public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
        if (view.getVisibility() == View.VISIBLE && view instanceof TextView) {
          TextView text = (TextView) view;
          if (isItemChecked(cursor.getPosition())) {
            text.setTypeface(Typeface.DEFAULT_BOLD);
          } else {
            text.setTypeface(Typeface.DEFAULT);
          }
        }
        // Let the default binder do the real work.
        return false;
      }});
    setAdapter(adapter);
    setOnItemClickListener(this);
  }

  public void overrideSortOrder(String sortOrder) {
    this.sortOrder = sortOrder;
  }

  protected void includeStaticCursor(Cursor cursor) {
    staticCursor = cursor;
  }

  // TODO(cgallek): get rid of these two accessor methods in favor of
  // onClick callbacks.
  public String getLastSelectedName() {
    return selectedName;
  }

  public Uri getLastSelectedUri() {
    return selectedUri;
  }

  public void setMediaPickListener(OnItemPickListener listener) {
    this.listener = listener;
  }

  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    setItemChecked(position, true);
    cursor.moveToPosition(position);
    selectedName = cursor.getString(cursor.getColumnIndex(nameColumn));
    final int toneIndex = cursor.getInt(cursor.getColumnIndex(BaseColumns._ID));
    if (toneIndex == DEFAULT_TONE_INDEX) {
      selectedUri = AlarmUtil.getDefaultAlarmUri();
    } else {
      selectedUri = Uri.withAppendedPath(contentUri, "" + toneIndex);
    }
    if (listener != null) {
      listener.onItemPick(selectedUri, selectedName);
    }
  }
}