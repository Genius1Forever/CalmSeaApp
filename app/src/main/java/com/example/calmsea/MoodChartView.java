package com.example.calmsea;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class MoodChartView extends View {
    private Paint linePaint;
    private Paint pointPaint;
    private Paint gridPaint;
    private Paint textPaint;

    private int[] moodValues = new int[7]; // Уровни настроений
    private Bitmap[] moodIcons; // Иконки настроений
    private String[] days = {"Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"}; // Дни недели

    private int chartPadding = 60; // Отступы
    private int iconSize = 80;     // Размер иконок

    public MoodChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // Настройки линий
        linePaint = new Paint();
        linePaint.setColor(Color.parseColor("#6495ED"));
        linePaint.setStrokeWidth(8f);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setAntiAlias(true);
        linePaint.setPathEffect(new CornerPathEffect(10f));

        // Настройки точек
        pointPaint = new Paint();
        pointPaint.setColor(Color.parseColor("#6495ED"));
        pointPaint.setStyle(Paint.Style.FILL);

        // Сетка
        gridPaint = new Paint();
        gridPaint.setColor(Color.LTGRAY);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(2f);

        // Текст
        textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(35f);
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setMoodValues(int[] moods) {
        this.moodValues = moods;
        invalidate();
    }

    public void setMoodIcons(Bitmap[] icons) {
        this.moodIcons = icons;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth() - chartPadding * 2;
        int height = getHeight() - chartPadding * 2;
        int cellWidth = width / 6;
        int cellHeight = height / 4;

        // Рисуем сетку
        for (int i = 0; i <= 4; i++) {
            canvas.drawLine(chartPadding, chartPadding + i * cellHeight,
                    width + chartPadding, chartPadding + i * cellHeight, gridPaint);
        }

        for (int i = 0; i <= 6; i++) {
            canvas.drawLine(chartPadding + i * cellWidth, chartPadding,
                    chartPadding + i * cellWidth, height + chartPadding, gridPaint);
        }

        // Рисуем линии графика только между существующими точками
        Path path = new Path();
        boolean firstPoint = true;
        float prevX = 0, prevY = 0;

        for (int i = 0; i < moodValues.length; i++) {
            if (moodValues[i] > 0) {  // Проверяем, есть ли данные
                float x = chartPadding + i * cellWidth;
                float y = chartPadding + (5 - moodValues[i]) * cellHeight;

                // Рисуем точку
                canvas.drawCircle(x, y, 10, pointPaint);
                Log.d("MoodChart", "Drawing point at: x=" + x + ", y=" + y);

                // Если это первая точка, начинаем путь
                if (firstPoint) {
                    path.moveTo(x, y);
                    firstPoint = false;
                } else {
                    // Соединяем с предыдущей точкой
                    path.lineTo(x, y);
                }

                // Запоминаем координаты текущей точки
                prevX = x;
                prevY = y;
            }
        }

// Отрисовываем линии только если есть хотя бы одна точка
        if (!firstPoint) {
            canvas.drawPath(path, linePaint);
        }
        canvas.drawPath(path, linePaint);

        // Рисуем дни недели
        for (int i = 0; i < days.length; i++) {
            float x = chartPadding + i * cellWidth;
            float y = getHeight() - 20;
            canvas.drawText(days[i], x, y, textPaint);
        }
    }
}
