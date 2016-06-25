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
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ResourceCursorAdapter;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.ViewAnimator;

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
                       return buildFlipper();
                       /*
                       return buildMediaList(
                           Audio.Artists.EXTERNAL_CONTENT_URI,
                           Audio.ArtistColumns.ARTIST,
                           Audio.Artists.DEFAULT_SORT_ORDER);
                       */
                     }
                   }));
      t.addTab(t.newTabSpec("external").setIndicator("external").setContent(
                   new TabHost.TabContentFactory() {
                     @Override
                     public View createTabContent(String tag) {
                       return buildMediaList(
                           Audio.Media.EXTERNAL_CONTENT_URI,
                           MediaColumns.TITLE,
                           null,
                           Audio.Media.DEFAULT_SORT_ORDER,
                           null,
                           new AdapterView.OnItemClickListener() {
                             @Override
                             public void onItemClick(AdapterView<?> parent, View v, int x, long id) {
                               TextView t = (TextView)v;
                               // TODO: this assumes leaf-level media.  Handle artists, albums, etc
                               uri = ContentUris.withAppendedId(Audio.Media.EXTERNAL_CONTENT_URI, id);
                               title = t.getText().toString();
                             }
                           });
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
                         null,
                         Audio.Media.DEFAULT_SORT_ORDER,
                         null,
                         new AdapterView.OnItemClickListener() {
                           @Override
                           public void onItemClick(AdapterView<?> parent, View v, int x, long id) {
                             TextView t = (TextView)v;
                             // TODO: this assumes leaf-level media.  Handle artists, albums, etc
                             uri = ContentUris.withAppendedId(Audio.Media.INTERNAL_CONTENT_URI, id);
                             title = t.getText().toString();
                           }
                         });
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

  private View buildFlipper() {
    final ViewAnimator flip = new ViewAnimator(getContext());
    flip.setInAnimation(getContext(), R.anim.slide_in_right);
    flip.setOutAnimation(getContext(), R.anim.slide_out_left);
    flip.addView(buildMediaList(
                  Audio.Artists.EXTERNAL_CONTENT_URI,
                  Audio.ArtistColumns.ARTIST,
                  null,
                  Audio.Artists.DEFAULT_SORT_ORDER,
                  null,
                  new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View v, int x, long id) {
                      flip.addView(buildMediaList(
                                       Audio.Artists.Albums.getContentUri(Audio.Artists.EXTERNAL_CONTENT_URI.getPathSegments().get(0) /* "external" */, id),
                                       Audio.Albums.ALBUM,
                                       null,
                                       Audio.Albums.DEFAULT_SORT_ORDER,
                                       flip,
                                       new AdapterView.OnItemClickListener() {
                                         @Override
                                         public void onItemClick(AdapterView<?> parent, View v, int x, long id) {
                                           flip.addView(buildMediaList(
                                                            Audio.Media.EXTERNAL_CONTENT_URI,
                                                            MediaColumns.TITLE,
                                                            Audio.AudioColumns.ALBUM_ID + " == " + id,
                                                            Audio.AudioColumns.TRACK,
                                                            flip,
                                                            new AdapterView.OnItemClickListener() {
                                                              @Override
                                                              public void onItemClick(AdapterView<?> parent, View v, int x, long id) {
                                                                TextView t = (TextView)v;
                                                                uri = ContentUris.withAppendedId(Audio.Media.EXTERNAL_CONTENT_URI, id);
                                                                title = t.getText().toString();
}
                                                            }), -1, new ViewGroup.LayoutParams(
                                                                ViewGroup.LayoutParams.MATCH_PARENT,
                                                                ViewGroup.LayoutParams.MATCH_PARENT));
                                           flip.showNext();
                                         }
                                       })
                                   , -1,
                                new ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT));
                      flip.showNext();
                    }
                  }) , -1,
              new ViewGroup.LayoutParams(
                  ViewGroup.LayoutParams.MATCH_PARENT,
                  ViewGroup.LayoutParams.MATCH_PARENT));
    return flip;
  }

  private View buildMediaList(final Uri query, final String display,
                              final String where, final String sort,
                              final ViewAnimator flip,
                              AdapterView.OnItemClickListener click) {
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
    list.setOnItemClickListener(click);
    if (flip != null) {
      list.setOnKeyListener(new View.OnKeyListener() {
          @Override
          public boolean onKey(View v, int keyCode, KeyEvent event) {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
              flip.setInAnimation(getContext(), R.anim.slide_in_left);
              flip.setOutAnimation(getContext(), R.anim.slide_out_right);
              flip.showPrevious();
              flip.removeView(list);
              flip.setInAnimation(getContext(), R.anim.slide_in_right);
              flip.setOutAnimation(getContext(), R.anim.slide_out_left);
              return true;
            } else {
              return false;
            }
          }
        });
    }

    final Loader<Cursor> loader = getLoaderManager().initLoader(
        list.getId(), null, new LoaderManager.LoaderCallbacks<Cursor>() {
            @Override
            public Loader<Cursor> onCreateLoader(int id, Bundle args) {
              return new CursorLoader(
                  getContext(), query, null, where, null, sort);
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
