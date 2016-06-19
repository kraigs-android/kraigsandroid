/****************************************************************************
 * Copyright 2016 kraigs.android@gmail.com
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

package com.angrydoughnuts.android.alarmclock2;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore.Audio;
import android.provider.MediaStore.MediaColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ResourceCursorAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class MediaPicker extends DialogFragment {
  public static interface Listener {
    abstract void onMediaPick(Uri uri, String title);
  }

  private Uri uri = null;
  private String title = null;
  private Listener listener = null;
  public void setListener(Listener l) { listener = l; }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    super.onCreateDialog(savedInstanceState);

    if (savedInstanceState != null) {
      uri = savedInstanceState.getParcelable("uri");
      title = savedInstanceState.getString("title");
    }

    final View v =
      ((LayoutInflater)getContext().getSystemService(
          Context.LAYOUT_INFLATER_SERVICE)).inflate(
              R.layout.media_picker, null);

    final ResourceCursorAdapter adapter = new ResourceCursorAdapter(
        getContext(), R.layout.media_picker_item, null, 0) {
        @Override
        public void bindView(View v, Context context, Cursor c) {
          TextView t = (TextView)v;
          t.setText(c.getString(c.getColumnIndex(MediaColumns.TITLE)));
        }
      };

    ListView list = (ListView)v;
    list.setAdapter(adapter);
    list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View v, int x, long id) {
          TextView t = (TextView)v;
          uri = ContentUris.withAppendedId(Audio.Media.INTERNAL_CONTENT_URI, id);
          title = t.getText().toString();
        }
      });

    final Loader<Cursor> loader = getLoaderManager().initLoader(
        0, null, new LoaderManager.LoaderCallbacks<Cursor>() {
            @Override
            public Loader<Cursor> onCreateLoader(int id, Bundle args) {
              return new CursorLoader(
                  getContext(), Audio.Media.INTERNAL_CONTENT_URI,
                  null, null, null, null);
            }
            @Override
            public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
              adapter.changeCursor(data);
            }
            @Override
            public void onLoaderReset(Loader<Cursor> loader) {
              adapter.changeCursor(null);
            }
          });

    return new AlertDialog.Builder(getContext())
      .setTitle("Media picker")
      .setView(v)
      .setNegativeButton("Cancel", null)
      .setPositiveButton("OK", new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            if (listener != null)
              listener.onMediaPick(uri, title);
          }
        }).create();
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    if (uri != null) outState.putParcelable("uri", uri);
    if (title != null) outState.putString("title", title);
  }
}
