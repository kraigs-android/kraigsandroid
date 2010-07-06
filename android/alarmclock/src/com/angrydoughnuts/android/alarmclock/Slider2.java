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

// TODO comment/cleanup this file.
// TODO remove 'slide to dismiss' text from notification.
public class Slider2 extends ViewGroup {
  public interface OnCompleteListener {
    void complete();
  }

  private static final int FADE_MILLIS = 200;
  private static final int SLIDE_MILLIS = 200;
  private static final float SLIDE_ACCEL = (float) 1.0;
  private static final double PERCENT_REQUIRED = 0.75;

  private ImageView dot;
  private TextView tray;
  private boolean tracking;
  private OnCompleteListener completeListener;

  public Slider2(Context context) {
    this(context, null, 0);
  }

  public Slider2(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public Slider2(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    tray = new TextView(getContext());
    tray.setBackgroundResource(R.drawable.slider_background);
    tray.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
    tray.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
    tray.setTextAppearance(getContext(), R.style.SliderText);
    tray.setText(R.string.dismiss);
    addView(tray);

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
    dot.measure(
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
    setMeasuredDimension(
        Math.max(tray.getMeasuredWidth(), dot.getMeasuredWidth()),
        Math.max(tray.getMeasuredHeight(), dot.getMeasuredHeight()));
  }

  // TODO(cgallek): Add some wiggle room to these.
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
        if (!tracking) {
          return super.onTouchEvent(event);
        }
        // The dot has been released, slide it back to the beginning.
        tracking = false;
        slideDotHome();
        return true;

      case MotionEvent.ACTION_MOVE:
        // Ignore move events which did not originate in the dot.
        if (!tracking) {
          return super.onTouchEvent(event);
        }
        // Slid out of the slider, reset to the beginning.
        if (!withinY(dot, y)) {
          tracking = false;
          slideDotHome();
          return true;
        }
        // If we haven't hit the threshold yet, simply move the dot.
        float progressPercent = (float)(dot.getLeft() - getLeft()) / (float)(getRight() - getLeft());
        if (progressPercent < PERCENT_REQUIRED) {
          dot.offsetLeftAndRight((int) (x - dot.getLeft() - dot.getWidth()/2 ));
          invalidate();
          return true;
        }
        // At this point, the dot has made it to the threshold.
        // Make the entire widgit fade away and then call the listener.
        tracking = false;
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
        return true;
      default:
        return super.onTouchEvent(event);
    }
  }
}
