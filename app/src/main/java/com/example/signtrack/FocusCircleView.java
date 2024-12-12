package com.example.signtrack;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;

public class FocusCircleView extends View {

    private Paint paint;
    private RectF focusCircle;

    private Handler handler;
    private Runnable removeFocusRunnable;

    public FocusCircleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5f);

        handler = new Handler(Looper.getMainLooper());
        removeFocusRunnable = new Runnable() {
            @Override
            public void run() {
                focusCircle = null;
                invalidate();
            }
        };
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (focusCircle != null) {
            // Calculate the outer circle radius
            float outerRadius = focusCircle.width() / 1.2f;

            // Calculate the inner circle radius
            float innerRadius = outerRadius / 2;

            // Draw the outer circle
            canvas.drawCircle(focusCircle.centerX(), focusCircle.centerY(), outerRadius, paint);

            // Draw the inner circle
            canvas.drawCircle(focusCircle.centerX(), focusCircle.centerY(), innerRadius, paint);

            scheduleFocusCircleRemoval();
        }
    }

    private void scheduleFocusCircleRemoval() {
        handler.removeCallbacks(removeFocusRunnable);
        handler.postDelayed(removeFocusRunnable, 2000); // Remove focus circle after 2 seconds
    }

    public void setFocusCircle(RectF focusCircle) {
        this.focusCircle = focusCircle;
        invalidate();
    }
}