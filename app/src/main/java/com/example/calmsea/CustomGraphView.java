package com.example.calmsea;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class CustomGraphView extends View {
    private Paint linePaint, textPaint, circlePaint;
    private float[] points = {0.2f, 0.8f, 0.6f, 0.4f, 0.5f, 0.7f, 0.3f}; // Значения эмоций на дни недели

    public CustomGraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // Настройка кистей
        linePaint = new Paint();
        linePaint.setColor(Color.LTGRAY);
        linePaint.setStrokeWidth(2);
        linePaint.setStyle(Paint.Style.STROKE);

        textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(30);
        textPaint.setAntiAlias(true);

        circlePaint = new Paint();
        circlePaint.setColor(Color.BLUE);
        circlePaint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float width = getWidth();
        float height = getHeight();
        float padding = 50;

        // Рисуем сетку
        for (int i = 0; i <= 5; i++) {
            float y = padding + i * ((height - 2 * padding) / 5);
            canvas.drawLine(padding, y, width - padding, y, linePaint);
        }

        for (int i = 0; i <= 7; i++) {
            float x = padding + i * ((width - 2 * padding) / 7);
            canvas.drawLine(x, padding, x, height - padding, linePaint);
        }

        // Подписи осей
        String[] days = {"пн", "вт", "ср", "чт", "пт", "сб", "вс"};
        for (int i = 0; i < days.length; i++) {
            float x = padding + i * ((width - 2 * padding) / 7);
            canvas.drawText(days[i], x - 15, height - 20, textPaint);
        }

        // Отрисовка данных
        for (int i = 0; i < points.length; i++) {
            float x = padding + i * ((width - 2 * padding) / 7);
            float y = height - padding - points[i] * (height - 2 * padding);
            canvas.drawCircle(x, y, 10, circlePaint);
        }
    }
}

