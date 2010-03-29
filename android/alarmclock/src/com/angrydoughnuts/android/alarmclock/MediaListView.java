package com.angrydoughnuts.android.alarmclock;

import java.util.ArrayList;
import java.util.Arrays;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.AdapterView.OnItemClickListener;

public final class MediaListView extends ListView implements OnItemClickListener {
  public interface OnItemClickListener {
    void onItemClick(String name, Uri media);
  }

  private Cursor cursor = null;
  private Uri contentUri = null;
  private OnItemClickListener listener = null;
  private String nameColumn = null;

  public MediaListView(Context context) {
    this(context, null);
  }

  public MediaListView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public MediaListView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

  public Cursor query(Uri contentUri, int rowResId,
      String[] displayColumns, int[] resIDs) {
    final ArrayList<String> queryColumns =
      new ArrayList<String>(displayColumns.length + 1);
    queryColumns.addAll(Arrays.asList(displayColumns));
    // The ID column is required for the SimpleCursorAdapter.  Make sure to
    // add it if it's not already there.
    if (!queryColumns.contains(BaseColumns._ID)) {
      queryColumns.add(BaseColumns._ID);
    }
    this.cursor = getContext().getContentResolver().query(
        contentUri, queryColumns.toArray(new String[] {}),
        null, null, null);
    this.contentUri = contentUri;

    final SimpleCursorAdapter adapter = new SimpleCursorAdapter(
        getContext(), rowResId, cursor, displayColumns, resIDs);
    setAdapter(adapter);
    setOnItemClickListener(this);

    return cursor;
  }

  public void setOnItemClickListener(String nameColumn, OnItemClickListener listener) {
    this.listener = listener;
    this.nameColumn = nameColumn;
  }

  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    if (listener == null) {
      return;
    }
    cursor.moveToPosition(position);
    final String name = cursor.getString(cursor.getColumnIndex(nameColumn));
    final Uri selection = Uri.withAppendedPath(contentUri, cursor.getString(cursor.getColumnIndex(BaseColumns._ID)));

    listener.onItemClick(name, selection);
  }
}