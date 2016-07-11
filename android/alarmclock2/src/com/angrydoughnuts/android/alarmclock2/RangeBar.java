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

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class RangeBar extends FrameLayout {
  private final ImageView min;
  private final ImageView max;
  private final ImageView progress;
  private boolean tracking_min = false;
  private boolean tracking_max = false;

  private Listener listener;
  public void setListener(Listener l) { listener = l; }

  private int range = 100;
  public void setRange(int max) { range = max; }

  private int min_value = 0;
  private int max_value = 100;
  public void setPosition(int new_min, int new_max) {
    min_value = new_min;
    max_value = new_max;
    updatePosition();
  }

  public int getPositionMin() { return position(min); }
  public int getPositionMax() { return position(max); }

  public RangeBar(Context c) {
    this(c, null);
  }

  public RangeBar(Context c, AttributeSet a) {
    this(c, a, 0);
  }

  public RangeBar(Context c, AttributeSet a, int defStyleAtr) {
    this(c, a, defStyleAtr, 0);
  }

  public RangeBar(Context c, AttributeSet a, int defStyleAtr, int defStyleRes) {
    super(c, a, defStyleAtr, defStyleRes);

    ((LayoutInflater)c.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
      .inflate(R.layout.range_bar, this, true);
    min = (ImageView)findViewById(R.id.range_bar_min);
    max = (ImageView)findViewById(R.id.range_bar_max);
    progress = (ImageView)findViewById(R.id.range_bar_progress);
    final ImageView track = (ImageView)findViewById(R.id.range_bar_bg);

    final TypedArray ta = c.obtainStyledAttributes(
        a, new int[] {
          android.R.attr.background,
          android.R.attr.progressDrawable,
          android.R.attr.thumb },
        defStyleAtr, android.R.style.Widget_Material_SeekBar);
    final Drawable thumb_bg = ta.getDrawable(0);
    final Drawable track_bg = ta.getDrawable(1);
    final Drawable thumb = ta.getDrawable(2);
    ta.recycle();

    min.setImageDrawable(thumb);
    min.setBackground(thumb_bg);
    max.setImageDrawable(thumb.getConstantState().newDrawable());
    max.setBackground(thumb_bg.getConstantState().newDrawable());
    track.setImageDrawable(track_bg);
    progress.setImageDrawable(track_bg.getConstantState().newDrawable());
    progress.setImageLevel(10000);  // Scale 0 - 10000
  }

  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {
    super.onLayout(changed, l, t, r, b);
    updatePosition();
    min.invalidate();
    max.invalidate();
    progress.invalidate();
  }

  @Override
  public boolean onInterceptTouchEvent(MotionEvent event) {
    switch (event.getActionMasked()) {
    case MotionEvent.ACTION_DOWN:
      // Track max first on left side, min first on right side.
      if (event.getX() < getWidth() / 2) {
        if (within(max, event.getX()))
          tracking_max = true;
        else if (within(min, event.getX()))
          tracking_min = true;
      } else {
        if (within(min, event.getX()))
          tracking_min = true;
        else if (within(max, event.getX()))
          tracking_max = true;
      }
      break;

    case MotionEvent.ACTION_MOVE:
      if (tracking_min) {
        move(min, event.getX());
        if (min.getLeft() > max.getLeft())
          move(max, event.getX());
        if (listener != null)
          listener.onChange(position(min), position(max));
      } else if (tracking_max) {
        move(max, event.getX());
        if (min.getLeft() > max.getLeft())
          move(min, event.getX());
        if (listener != null)
          listener.onChange(position(min), position(max));
      }
      if (tracking_min || tracking_max) {
        progress.setLeft(min.getLeft());
        progress.setRight(max.getRight() - max.getWidth()/2);
      }
      break;

    case MotionEvent.ACTION_UP:
      if ((tracking_min || tracking_max) && listener != null)
        listener.onDone(position(min), position(max));
      tracking_min = false;
      tracking_max = false;
      break;

    case MotionEvent.ACTION_CANCEL:
      tracking_min = false;
      tracking_max = false;
      break;
    }
    return super.onInterceptTouchEvent(event);
  }

  private boolean within(View v, float x) {
    return x > v.getLeft() && x < v.getRight();
  }

  // L|---                                                         ---|R
  //  |---|-----------*---------------|---------------*-----------|---|
  //  |---         target           stride                         ---|
  private void move(View v, float x) {
    final int xmin = getLeft() + getPaddingLeft();
    final int xmax = getRight() - getPaddingRight() - v.getWidth();
    final int width =
      getWidth() - getPaddingLeft() - getPaddingRight() - v.getWidth();
    final float stride = width / (float)range;
    final int target = (int)((x + stride/2.0f - v.getWidth()/1.5f) / stride);
    x = stride * target + getLeft() + getPaddingLeft();
    if (x < xmin)
      x = xmin;
    else if (x > xmax)
      x = xmax;
    v.offsetLeftAndRight((int)(x - v.getLeft() - getLeft()));
  }

  private int position(View v) {
    final int width =
      getWidth() - getPaddingLeft() - getPaddingRight() - v.getWidth();
    final float stride = width / (float)range;
    return (int)((v.getLeft() - getPaddingLeft()) / stride);
  }

  private void updatePosition() {
    final int width =
      getWidth() - getPaddingLeft() - getPaddingRight() - min.getWidth();
    final float stride = width / (float)range;
    final int min_target = (int)(min_value * stride);
    final int max_target = (int)(max_value * stride);
    min.offsetLeftAndRight(min_target - min.getLeft() + getPaddingLeft());
    max.offsetLeftAndRight(max_target - max.getLeft() + getPaddingLeft());
    progress.setLeft(min.getLeft());
    progress.setRight(max.getRight() - max.getWidth()/2);
  }

  public static interface Listener {
    abstract void onChange(int min, int max);
    abstract void onDone(int min, int max);
  }
}
