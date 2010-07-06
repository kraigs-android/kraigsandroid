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

import android.content.Context;
import android.database.MatrixCursor;
import android.database.MatrixCursor.RowBuilder;
import android.media.MediaPlayer;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.MediaStore.MediaColumns;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView;

public class MediaSongsView extends MediaListView implements OnItemClickListener {
  private final String[] songsColumns = new String[] {
    MediaColumns.TITLE,
  };

  final int[] songsResIDs = new int[] {
      R.id.media_value,
  };

  public MediaSongsView(Context context) {
    this(context, null);
  }

  public MediaSongsView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public MediaSongsView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    overrideSortOrder(MediaColumns.TITLE + " ASC");
  }

  public void query(Uri contentUri) {
    query(contentUri, null);
  }

  public void query(Uri contentUri, String selection) {
    super.query(contentUri, MediaColumns.TITLE, selection, R.layout.media_picker_row, songsColumns, songsResIDs);
  }

  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    super.onItemClick(parent, view, position, id);

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

  public void includeDefault() {
    final ArrayList<String> defaultColumns =
      new ArrayList<String>(songsColumns.length + 1);
    defaultColumns.addAll(Arrays.asList(songsColumns));
    defaultColumns.add(BaseColumns._ID);
    final MatrixCursor defaultsCursor = new MatrixCursor(defaultColumns.toArray(new String[] {}));
    RowBuilder row = defaultsCursor.newRow();
    row.add("Default");
    row.add(DEFAULT_TONE_INDEX);
    includeStaticCursor(defaultsCursor);
  }
}
