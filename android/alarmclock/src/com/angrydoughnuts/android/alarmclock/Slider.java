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

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.view.animation.Animation.AnimationListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ImageView.ScaleType;

/**
 * Widget that contains a slider bar used for acknowledgments.  The user
 * must slide an arrow sufficiently far enough across the bar in order
 * to trigger the acknowledgment.
 */
public class Slider extends ViewGroup {
  public interface OnCompleteListener {
    void complete();
  }

  private static final int FADE_MILLIS = 200;
  private static final int SLIDE_MILLIS = 200;
  private static final float SLIDE_ACCEL = (float) 1.0;
  private static final double PERCENT_REQUIRED = 0.72;

  private ImageView dot;
  private TextView tray;
  private boolean tracking;
  private OnCompleteListener completeListener;

  public Slider(Context context) {
    this(context, null, 0);
  }

  public Slider(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public Slider(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    // Setup the background which 'holds' the slider.
    tray = new TextView(getContext());
    tray.setBackgroundResource(R.drawable.slider_background);
    tray.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
    tray.setGravity(Gravity.CENTER);
    tray.setTextAppearance(getContext(), R.style.SliderText);
    tray.setText(R.string.dismiss);
    addView(tray);

    // Setup the object which will be slid.
    dot = new ImageView(getContext());
    dot.setImageResource(R.drawable.slider_icon);
    dot.setBackgroundResource(R.drawable.slider_btn);
    dot.setScaleType(ScaleType.CENTER);
    dot.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
    dot.setPadding(30, 10, 25, 15);
    addView(dot);

    reset();
  }

  public void setOnCompleteListener(OnCompleteListener listener) {
    completeListener = listener;
  }

  public void reset() {
    tracking = false;
    // Move the dot home and fade in.
    if (getVisibility() != View.VISIBLE) {
      dot.offsetLeftAndRight(getLeft() - dot.getLeft());
      setVisibility(View.VISIBLE);
      Animation fadeIn = new AlphaAnimation(0, 1);
      fadeIn.setDuration(FADE_MILLIS);
      startAnimation(fadeIn);
    }
  }

  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {
    if (!changed) {
      return;
    }
    // Start the dot left-aligned.
    dot.layout(0, 0, dot.getMeasuredWidth(), dot.getMeasuredHeight());
    // Make the tray fill the background.
    tray.layout(0, 0, getMeasuredWidth(), getMeasuredHeight());
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    tray.measure(widthMeasureSpec, heightMeasureSpec);
    dot.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        heightMeasureSpec);
    setMeasuredDimension(
        Math.max(tray.getMeasuredWidth(), dot.getMeasuredWidth()),
        Math.max(tray.getMeasuredHeight(), dot.getMeasuredHeight()));
  }

  private boolean withinX(View v, float x) {
    if (x < v.getLeft() || x > v.getRight()) {
      return false;
    } else {
      return true;
    }
  }

  private boolean withinY(View v, float y) {
    if (y < v.getTop() || y > v.getBottom()) {
      return false;
    } else {
      return true;
    }
  }

  private void slideDotHome() {
    int distanceFromStart = dot.getLeft() - getLeft();
    dot.offsetLeftAndRight(-distanceFromStart);
    Animation slideBack = new TranslateAnimation(distanceFromStart, 0, 0, 0);
    slideBack.setDuration(SLIDE_MILLIS);
    slideBack.setInterpolator(new DecelerateInterpolator(SLIDE_ACCEL));
    dot.startAnimation(slideBack);
  }

  private boolean isComplete() {
    double dotCenterY = dot.getLeft() + dot.getMeasuredWidth()/2.0;
    float progressPercent = (float)(dotCenterY - getLeft()) / (float)(getRight() - getLeft());
    if (progressPercent > PERCENT_REQUIRED) {
      return true;
    } else {
      return false;
    }
  }

  private void finishSlider() {
    setVisibility(View.INVISIBLE);
    Animation fadeOut = new AlphaAnimation(1, 0);
    fadeOut.setDuration(FADE_MILLIS);
    fadeOut.setAnimationListener(new AnimationListener() {
      @Override
      public void onAnimationEnd(Animation animation) {
        if (completeListener != null) {
          completeListener.complete();
        }
      }
      @Override
      public void onAnimationRepeat(Animation animation) {}
      @Override
      public void onAnimationStart(Animation animation) {}
    });
    startAnimation(fadeOut);
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    final int action = event.getAction();
    final float x = event.getX();
    final float y = event.getY();
    switch (action) {
      case MotionEvent.ACTION_DOWN:
        // Start tracking if the down event is in the dot.
        tracking = withinX(dot, x) && withinY(dot, y);
        return tracking || super.onTouchEvent(event);

      case MotionEvent.ACTION_CANCEL:
      case MotionEvent.ACTION_UP:
        // Ignore move events which did not originate in the dot.
        if (!tracking) {
          return super.onTouchEvent(event);
        }
        // The dot has been released, check to see if we've hit the threshold,
        // otherwise, send the dot home.
        tracking = false;
        if (isComplete()) {
          finishSlider();
        } else {
          slideDotHome();
        }
        return true;

      case MotionEvent.ACTION_MOVE:
        // Ignore move events which did not originate in the dot.
        if (!tracking) {
          return super.onTouchEvent(event);
        }
        // Update the current location.
        dot.offsetLeftAndRight((int) (x - dot.getLeft() - dot.getWidth()/2.0 ));

        // See if we have reached the threshold.
        if (isComplete()) {
          tracking = false;
          finishSlider();
          return true;
        }

        // Otherwise, we have not yet hit the completion threshold. Make sure
        // the move is still within bounds of the dot and redraw.
        if (!withinY(dot, y)) {
          // Slid out of the slider, reset to the beginning.
          tracking = false;
          slideDotHome();
        } else {
          invalidate();
        }
        return true;

      default:
        return super.onTouchEvent(event);
    }
  }
}
