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

// NOTE: this was mostly lifted from
// core/java/com/android/internal/widget/SlidingTab.java
// in the base framework repository.
// TODO(cgallek): This file needs lots of cleanup.

package com.angrydoughnuts.android.alarmclock;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.TranslateAnimation;
import android.view.animation.Animation.AnimationListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ImageView.ScaleType;

public class Slider extends ViewGroup {
  private static final float THRESHOLD = 8.5f / 10.0f;
  private static final int TRACKING_MARGIN = 50;
  private static final int ANIM_DURATION = 250;

  private OnTriggerListener mOnTriggerListener;
  private boolean mTriggered = false;

  private Slide mSlider;
  private boolean mTracking;
  private float mThreshold;
  private boolean mAnimating;
  private Rect mTmpRect;

  private final AnimationListener mAnimationDoneListener = new AnimationListener() {
    public void onAnimationStart(Animation animation) {
    }

    public void onAnimationRepeat(Animation animation) {
    }

    public void onAnimationEnd(Animation animation) {
      onAnimationDone();
    }
  };

  public interface OnTriggerListener {
    void onTrigger(View v);
  }

  private static class Slide {
    private static final int STATE_NORMAL = 0;
    private static final int STATE_PRESSED = 1;
    private static final int STATE_ACTIVE = 2;

    private final ImageView tab;
    private final TextView text;
    private int currentState = STATE_NORMAL;
    private int alignment_value;

    Slide(ViewGroup parent, int tabId, int barId) {
      tab = new ImageView(parent.getContext());
      tab.setBackgroundResource(tabId);
      tab.setScaleType(ScaleType.CENTER);
      tab.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,
          LayoutParams.WRAP_CONTENT));

      text = new TextView(parent.getContext());
      text.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,
          LayoutParams.FILL_PARENT));
      text.setBackgroundResource(barId);
      // text.setTextAppearance(parent.getContext(),
      // R.style.TextAppearance_SlidingTabNormal);

      parent.addView(tab);
      parent.addView(text);
    }

    void setIcon(int iconId) {
      tab.setImageResource(iconId);
    }

    void setTabBackgroundResource(int tabId) {
      tab.setBackgroundResource(tabId);
    }

    void setBarBackgroundResource(int barId) {
      text.setBackgroundResource(barId);
    }

    void setHintText(int resId) {
      text.setText(resId);
    }

    void setState(int state) {
      text.setPressed(state == STATE_PRESSED);
      tab.setPressed(state == STATE_PRESSED);
      if (state == STATE_ACTIVE) {
        final int[] activeState = new int[] { R.drawable.slider_head };
        if (text.getBackground().isStateful()) {
          text.getBackground().setState(activeState);
        }
        if (tab.getBackground().isStateful()) {
          tab.getBackground().setState(activeState);
        }
        // text.setTextAppearance(text.getContext(),
        // R.style.TextAppearance_SlidingTabActive);
      } else {
        // text.setTextAppearance(text.getContext(),
        // R.style.TextAppearance_SlidingTabNormal);
      }
      currentState = state;
    }

    void reset(boolean animate) {
      setState(STATE_NORMAL);
      text.setVisibility(View.VISIBLE);
      tab.setVisibility(View.VISIBLE);
      int dx = alignment_value - tab.getLeft();
      int dy = 0;
      if (animate) {
        TranslateAnimation trans = new TranslateAnimation(0, dx, 0, dy);
        trans.setDuration(ANIM_DURATION);
        trans.setFillAfter(false);
        text.startAnimation(trans);
        tab.startAnimation(trans);
      }
      text.offsetLeftAndRight(dx);
      tab.offsetLeftAndRight(dx);
      text.clearAnimation();
      tab.clearAnimation();
    }

    void layout(int l, int t, int r, int b) {
      final Drawable tabBackground = tab.getBackground();
      final int handleWidth = tabBackground.getIntrinsicWidth();
      final int handleHeight = tabBackground.getIntrinsicHeight();
      final int parentWidth = r - l;
      final int parentHeight = b - t;

      final int top = (parentHeight - handleHeight) / 2;
      final int bottom = (parentHeight + handleHeight) / 2;
      tab.layout(0, top, handleWidth, bottom);
      text.layout(0 - parentWidth, top, 0, bottom);
      text.setGravity(Gravity.RIGHT);
      alignment_value = l;
    }

    public void updateDrawableStates() {
      setState(currentState);
    }

    public void measure() {
      tab.measure(View.MeasureSpec.makeMeasureSpec(0,
          View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0,
              View.MeasureSpec.UNSPECIFIED));
      text.measure(View.MeasureSpec.makeMeasureSpec(0,
          View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0,
              View.MeasureSpec.UNSPECIFIED));
    }

    public int getTabWidth() {
      return tab.getMeasuredWidth();
    }

    public int getTabHeight() {
      return tab.getMeasuredHeight();
    }

    public void startAnimation(Animation anim1, Animation anim2) {
      tab.startAnimation(anim1);
      text.startAnimation(anim2);
    }
  }

  public Slider(Context context) {
    this(context, null);
  }

  public Slider(Context context, AttributeSet attrs) {
    super(context, attrs);

    // Allocate a temporary once that can be used everywhere.
    mTmpRect = new Rect();

    mSlider = new Slide(this, R.drawable.slider_head, R.drawable.slider_tail);
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
    int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);

    int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);

    if (widthSpecMode == MeasureSpec.UNSPECIFIED
        || heightSpecMode == MeasureSpec.UNSPECIFIED) {
      throw new RuntimeException("cannot have UNSPECIFIED dimensions");
    }

    mSlider.measure();
    final int width = Math.max(widthSpecSize, mSlider.getTabWidth());
    final int height = mSlider.getTabHeight();
    setMeasuredDimension(width, height);
  }

  @Override
  public boolean onInterceptTouchEvent(MotionEvent event) {
    final int action = event.getAction();
    final float x = event.getX();
    final float y = event.getY();

    if (mAnimating) {
      return false;
    }

    View handle = mSlider.tab;
    handle.getHitRect(mTmpRect);
    boolean hit = mTmpRect.contains((int) x, (int) y);

    if (!mTracking && !hit) {
      return false;
    }

    switch (action) {
    case MotionEvent.ACTION_DOWN: {
      mTracking = true;
      mTriggered = false;
      if (hit) {
        mThreshold = THRESHOLD;
      } else {
        mThreshold = 1.0f - THRESHOLD;
      }
      mSlider.setState(Slide.STATE_PRESSED);
      break;
    }
    }

    return true;
  }

  public void reset(boolean animate) {
    mSlider.reset(animate);
  }

  @Override
  public void setVisibility(int visibility) {
    // Clear animations so sliders don't continue to animate when we show the
    // widget again.
    if (visibility != getVisibility() && visibility == View.INVISIBLE) {
      reset(false);
    }
    super.setVisibility(visibility);
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    if (mTracking) {
      final int action = event.getAction();
      final float x = event.getX();
      final float y = event.getY();

      switch (action) {
      case MotionEvent.ACTION_MOVE:
        if (withinView(x, y, this)) {
          moveHandle(x, y);
          // float position = isHorizontal() ? x : y;
          // float target = mThreshold * (isHorizontal() ? getWidth() :
          // getHeight());
          float position = x;
          float target = mThreshold * getWidth();
          boolean thresholdReached;
          thresholdReached = position > target;
          if (!mTriggered && thresholdReached) {
            mTriggered = true;
            mTracking = false;
            mSlider.setState(Slide.STATE_ACTIVE);
            if (mOnTriggerListener != null) {
              mOnTriggerListener.onTrigger(this);
            }
            startAnimating(true);
          }
          break;
        }
        // Intentionally fall through - we're outside tracking rectangle

      case MotionEvent.ACTION_UP:
      case MotionEvent.ACTION_CANCEL:
        mTracking = false;
        mTriggered = false;
        mSlider.reset(true);
        break;
      }
    }

    return mTracking || super.onTouchEvent(event);
  }

  void startAnimating(final boolean holdAfter) {
    mAnimating = true;
    final Animation trans1;
    final Animation trans2;
    final int dx;
    final int dy;
    int width = mSlider.tab.getWidth();
    int left = mSlider.tab.getLeft();
    int viewWidth = getWidth();
    int holdOffset = holdAfter ? 0 : width; // how much of tab to show at the
    // end of anim
    dx = (viewWidth - left) + viewWidth - holdOffset;
    dy = 0;
    trans1 = new TranslateAnimation(0, dx, 0, dy);
    trans1.setDuration(ANIM_DURATION);
    trans1.setInterpolator(new LinearInterpolator());
    trans1.setFillAfter(true);
    trans2 = new TranslateAnimation(0, dx, 0, dy);
    trans2.setDuration(ANIM_DURATION);
    trans2.setInterpolator(new LinearInterpolator());
    trans2.setFillAfter(true);

    trans1.setAnimationListener(new AnimationListener() {
      public void onAnimationEnd(Animation animation) {
        Animation anim;
        if (holdAfter) {
          anim = new TranslateAnimation(dx, dx, dy, dy);
          anim.setDuration(1000); // plenty of time for transitions
          mAnimating = false;
        } else {
          anim = new AlphaAnimation(0.5f, 1.0f);
          anim.setDuration(ANIM_DURATION);
          resetView();
        }
        anim.setAnimationListener(mAnimationDoneListener);

        /* Animation can be the same for these since the animation just holds */
        mSlider.startAnimation(anim, anim);
      }

      public void onAnimationRepeat(Animation animation) {

      }

      public void onAnimationStart(Animation animation) {

      }

    });

    mSlider.startAnimation(trans1, trans2);
  }

  private void onAnimationDone() {
    resetView();
    mAnimating = false;
  }

  private boolean withinView(final float x, final float y, final View view) {
    return y > -TRACKING_MARGIN && y < TRACKING_MARGIN + view.getHeight();

  }

  private void resetView() {
    mSlider.reset(false);
  }

  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {
    if (!changed)
      return;
    mSlider.layout(l, t, r, b);
  }

  private void moveHandle(float x, float y) {
    final View handle = mSlider.tab;
    final View content = mSlider.text;
    int deltaX = (int) x - handle.getLeft() - (handle.getWidth() / 2);
    handle.offsetLeftAndRight(deltaX);
    content.offsetLeftAndRight(deltaX);
    invalidate(); // TODO: be more conservative about what we're invalidating
  }

  public void setResources(int iconId, int barId, int tabId) {
    mSlider.setIcon(iconId);
    mSlider.setBarBackgroundResource(barId);
    mSlider.setTabBackgroundResource(tabId);
    mSlider.updateDrawableStates();
  }

  public void setHintText(int resId) {
    mSlider.setHintText(resId);
  }

  public void setOnTriggerListener(OnTriggerListener listener) {
    mOnTriggerListener = listener;
  }
}
