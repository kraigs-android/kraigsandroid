package com.angrydoughnuts.android.alarmclock;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ImageView.ScaleType;

public class Slider2 extends ViewGroup {
  private ImageView dot;
  private ImageView target;
  private boolean tracking;

  public Slider2(Context context) {
    this(context, null, 0);
  }

  public Slider2(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public Slider2(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    setBackgroundResource(android.R.color.white);
    dot = new ImageView(getContext());
    dot.setImageResource(android.R.drawable.ic_menu_add);
    dot.setScaleType(ScaleType.CENTER);
    dot.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
    addView(dot);

    target = new ImageView(getContext());
    target.setImageResource(android.R.drawable.ic_menu_delete);
    target.setScaleType(ScaleType.CENTER);
    target.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
    addView(target);

    tracking = false;
  }

  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {
    if (!changed) {
      return;
    }
    int dot_width = dot.getMeasuredWidth();
    int target_width = target.getMeasuredWidth();
    dot.layout(0, 0, dot_width, dot.getMeasuredHeight());
    target.layout(r-target_width, 0, r, target.getMeasuredHeight());
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    dot.measure(
      View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
      View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
    target.measure(
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
    setMeasuredDimension(
        Math.max(MeasureSpec.getSize(widthMeasureSpec),
            dot.getMeasuredWidth() + target.getMeasuredWidth()),
        Math.max(dot.getMeasuredHeight(), target.getMeasuredHeight()));
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


  @Override
  public boolean onTouchEvent(MotionEvent event) {
    final int action = event.getAction();
    final float x = event.getX();
    final float y = event.getY();
    switch (action) {
      case MotionEvent.ACTION_DOWN:
        tracking = withinX(dot, x) && withinY(dot, y);
        return tracking || super.onTouchEvent(event);
      case MotionEvent.ACTION_MOVE:
        if (!tracking) {
          return super.onTouchEvent(event);
        }
        if (withinY(dot, y)) {
          dot.offsetLeftAndRight((int) (x - dot.getLeft() - dot.getWidth()/2 ));
          float dot_x_center = dot.getLeft() + dot.getWidth()/2;
          float progress = dot_x_center - getLeft();
          float progress_percent = progress / (getRight() - getLeft());
          if (progress_percent > 0.85) {
            Toast.makeText(getContext(), "COMPLETE", Toast.LENGTH_SHORT).show();
            tracking = false;
            dot.offsetLeftAndRight(getLeft() - dot.getLeft());
          }
        } else {
          tracking = false;
          dot.offsetLeftAndRight(getLeft() - dot.getLeft());
        }
        invalidate();
        return true;
      case MotionEvent.ACTION_CANCEL:
      case MotionEvent.ACTION_UP:
        if (!tracking) {
          return super.onTouchEvent(event);
        }
        tracking = false;
        dot.offsetLeftAndRight(getLeft() - dot.getLeft());
        invalidate();
        return true;
      default:
        return super.onTouchEvent(event);
    }
  }
}
