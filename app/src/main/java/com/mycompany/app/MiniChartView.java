package com.mycompany.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

/**
 * رسم بياني مصغّر (خط اتجاه) لعرض تطور الربح خلال عدة فترات متتالية،
 * بدون الحاجة لأي مكتبة رسوم بيانية خارجية.
 */
public class MiniChartView extends View {

  private float[] values = new float[0];
  private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

  public MiniChartView(Context context) {
    super(context);
    init();
  }

  public MiniChartView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  private void init() {
    linePaint.setColor(Color.parseColor("#1E88E5"));
    linePaint.setStrokeWidth(dp(2.5f));
    linePaint.setStyle(Paint.Style.STROKE);
    linePaint.setStrokeCap(Paint.Cap.ROUND);
    linePaint.setStrokeJoin(Paint.Join.ROUND);

    fillPaint.setColor(Color.parseColor("#331E88E5"));
    fillPaint.setStyle(Paint.Style.FILL);

    dotPaint.setColor(Color.parseColor("#1565C0"));
    dotPaint.setStyle(Paint.Style.FILL);

    gridPaint.setColor(Color.parseColor("#E0E0E0"));
    gridPaint.setStrokeWidth(dp(1f));
  }

  private float dp(float v) {
    return v * getResources().getDisplayMetrics().density;
  }

  public void setData(float[] newValues) {
    this.values = newValues != null ? newValues : new float[0];
    invalidate();
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);

    int w = getWidth();
    int h = getHeight();
    float paddingV = dp(10f);
    float paddingH = dp(6f);

    // خطوط شبكة أفقية خفيفة لسهولة القراءة
    canvas.drawLine(0, paddingV, w, paddingV, gridPaint);
    canvas.drawLine(0, h - paddingV, w, h - paddingV, gridPaint);

    if (values == null || values.length == 0) return;

    if (values.length == 1) {
      float y = h / 2f;
      canvas.drawCircle(w / 2f, y, dp(4f), dotPaint);
      return;
    }

    float min = values[0];
    float max = values[0];
    for (float v : values) {
      if (v < min) min = v;
      if (v > max) max = v;
    }
    if (max == min) {
      max = min + 1f;
    }

    float usableHeight = h - 2 * paddingV;
    float usableWidth = w - 2 * paddingH;
    float stepX = usableWidth / (values.length - 1);

    Path linePath = new Path();
    Path fillPath = new Path();

    float[] xs = new float[values.length];
    float[] ys = new float[values.length];

    for (int i = 0; i < values.length; i++) {
      float x = paddingH + i * stepX;
      float ratio = (values[i] - min) / (max - min);
      float y = paddingV + (1f - ratio) * usableHeight;
      xs[i] = x;
      ys[i] = y;
      if (i == 0) {
        linePath.moveTo(x, y);
        fillPath.moveTo(x, h - paddingV);
        fillPath.lineTo(x, y);
      } else {
        linePath.lineTo(x, y);
        fillPath.lineTo(x, y);
      }
    }
    fillPath.lineTo(xs[xs.length - 1], h - paddingV);
    fillPath.close();

    canvas.drawPath(fillPath, fillPaint);
    canvas.drawPath(linePath, linePaint);

    for (int i = 0; i < xs.length; i++) {
      canvas.drawCircle(xs[i], ys[i], dp(3f), dotPaint);
    }
  }
}
