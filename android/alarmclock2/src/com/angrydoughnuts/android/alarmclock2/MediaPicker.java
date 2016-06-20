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

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Process;
import android.provider.MediaStore.Audio;
import android.provider.MediaStore.MediaColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ResourceCursorAdapter;
import android.widget.ListView;
import android.widget.TabHost;
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

    final TabHost t = (TabHost)((LayoutInflater)getContext().getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE)).inflate(
                            R.layout.media_picker, null);
    t.setup();
    if (getContext().checkPermission(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Process.myPid(), Process.myUid()) ==
        PackageManager.PERMISSION_GRANTED) {
      t.addTab(t.newTabSpec("artists").setIndicator("artists").setContent(
                   new TabHost.TabContentFactory() {
                     @Override
                     public View createTabContent(String tag) {
                       return buildMediaList(
                           Audio.Artists.EXTERNAL_CONTENT_URI,
                           Audio.ArtistColumns.ARTIST,
                           Audio.Artists.DEFAULT_SORT_ORDER);
                     }
                   }));
      t.addTab(t.newTabSpec("external").setIndicator("external").setContent(
                   new TabHost.TabContentFactory() {
                     @Override
                     public View createTabContent(String tag) {
                       return buildMediaList(
                           Audio.Media.EXTERNAL_CONTENT_URI,
                           MediaColumns.TITLE,
                           Audio.Media.DEFAULT_SORT_ORDER);
                     }
                   }));
    }
    t.addTab(t.newTabSpec("internal").setIndicator("internal").setContent(
                 new TabHost.TabContentFactory() {
                   @Override
                     public View createTabContent(String tag) {
                     return buildMediaList(
                         Audio.Media.INTERNAL_CONTENT_URI,
                         MediaColumns.TITLE,
                         Audio.Media.DEFAULT_SORT_ORDER);
                   }
                 }));

    return new AlertDialog.Builder(getContext())
      .setTitle("Media picker")
      .setView(t)
      .setNegativeButton("Cancel", null)
      .setPositiveButton("OK", new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            if (listener != null && uri != null && title != null)
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

  private View buildMediaList(final Uri query, final String display,
                              final String sort) {
    final ResourceCursorAdapter adapter = new ResourceCursorAdapter(
        getContext(), R.layout.media_picker_item, null, 0) {
        @Override
        public void bindView(View v, Context context, Cursor c) {
          TextView t = (TextView)v;
          t.setText(c.getString(c.getColumnIndex(display)));
        }
      };

    final ListView list = new ListView(getContext());
    list.setId(View.generateViewId());
    list.setAdapter(adapter);
    list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View v, int x, long id) {
          TextView t = (TextView)v;
          // TODO: this assumes leaf-level media.  Handle artists, albums, etc
          uri = ContentUris.withAppendedId(query, id);
          title = t.getText().toString();
        }
      });

    final Loader<Cursor> loader = getLoaderManager().initLoader(
        list.getId(), null, new LoaderManager.LoaderCallbacks<Cursor>() {
            @Override
            public Loader<Cursor> onCreateLoader(int id, Bundle args) {
              return new CursorLoader(
                  getContext(), query, null, null, null, sort);
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
    return list;
  }
}
