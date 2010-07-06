package com.angrydoughnuts.android.alarmclock;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

public class Slider2 extends ViewGroup {
  private ImageView dot;
  private ImageView target;

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
}
