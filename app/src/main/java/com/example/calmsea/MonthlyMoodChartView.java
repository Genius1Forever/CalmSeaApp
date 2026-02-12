package com.example.calmsea;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.Arrays;
import java.util.Calendar;

public class MonthlyMoodChartView extends View {
    private Paint linePaint, pointPaint, gridPaint, textPaint;
    private int[] moodValues = new int[31]; // Уровни настроений за месяц
    private String[] days = new String[31]; // Дни месяца
    private int chartPadding = 60; // Отступы
    private int iconSize = 80;

    public MonthlyMoodChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        textPaint = new Paint();
        textPaint.setColor(Color.BLACK); // Контрастный цвет
        textPaint.setTextSize(30); // Размер шрифта
        textPaint.setAntiAlias(true); // Сглаживание текста
        init();
    }

    private void init() {
        linePaint = new Paint();
        linePaint.setColor(Color.parseColor("#6495ED"));
        linePaint.setStrokeWidth(6f);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setAntiAlias(true);
        linePaint.setPathEffect(new CornerPathEffect(10f)); // Сглаживание углов

        pointPaint = new Paint();
        pointPaint.setColor(Color.parseColor("#6495ED"));
        pointPaint.setStyle(Paint.Style.FILL);

        gridPaint = new Paint();
        gridPaint.setColor(Color.BLACK);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(2f);

        textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(30f);
        textPaint.setTextAlign(Paint.Align.CENTER);

        generateDays();
    }

    private void generateDays() {
        for (int i = 0; i < 31; i++) {
            days[i] = String.valueOf(i + 1);
        }
    }

    public void setMoodValues(int[] moods) {
        Log.d("MoodChart", "Setting mood values: " + Arrays.toString(moods));
        this.moodValues = moods;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Log.d("MoodChart", "onDraw вызван! Размеры: " + getWidth() + "x" + getHeight());

        // Определяем текущий месяц и количество дней
        Calendar calendar = Calendar.getInstance();
        int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

        // Создаём массив дней, если он пустой или неправильный
        if (days == null || days.length != daysInMonth) {
            days = new String[daysInMonth];
            for (int i = 0; i < daysInMonth; i++) {
                days[i] = String.valueOf(i + 1);
            }
        }

        Log.d("MoodChart", "onDraw вызван! Дни в массиве: " + Arrays.toString(days));

        // Проверяем размеры холста
        int width = getWidth() - chartPadding * 2;
        int height = getHeight() - chartPadding * 2;
        if (width <= 0 || height <= 0) return;

        int cellWidth = Math.max(1, width / daysInMonth); // Точное количество дней
        int cellHeight = height / 5; // 5 уровней настроения

        // Рисуем горизонтальные линии сетки
        for (int i = 0; i <= 5; i++) {
            float y = chartPadding + i * cellHeight;
            canvas.drawLine(chartPadding, y, chartPadding + daysInMonth * cellWidth, y, gridPaint);
        }

        // Рисуем вертикальные линии сетки + подписи дней
        for (int i = 0; i < daysInMonth; i++) {
            float x = chartPadding + (i * cellWidth);
            canvas.drawLine(x, chartPadding, x, chartPadding + height, gridPaint);

            if (days[i] != null) {
                Log.d("MoodChart", "days[" + i + "] = " + days[i]);
                // Рисуем числа прямо под вертикальными линиями
                canvas.drawText(days[i], x, chartPadding + height + 30, textPaint);
            }
        }

        // Теперь горизонтальная линия заканчивается ровно на последней вертикальной линии
        float lastX = chartPadding + (daysInMonth - 1) * cellWidth;
        float lastY = chartPadding + 5 * cellHeight;
        canvas.drawLine(chartPadding, lastY, lastX, lastY, gridPaint);

        // Рисуем график (если есть данные)
        if (moodValues != null && moodValues.length > 0) {
            Path path = new Path();
            boolean firstPoint = true;
            float lastGraphX = 0, lastGraphY = 0;

            Log.d("MoodChart", "moodValues: " + Arrays.toString(moodValues));

            for (int i = 0; i < moodValues.length && i < daysInMonth; i++) {
                if (moodValues[i] > 0) {
                    float x = chartPadding + i * cellWidth;
                    float y = chartPadding + (5 - moodValues[i]) * cellHeight;

                    // Точка графика
                    canvas.drawCircle(x, y, 10, pointPaint);

                    // Линия графика
                    if (firstPoint) {
                        path.moveTo(x, y);
                        firstPoint = false;
                    } else {
                        path.lineTo(x, y);
                    }

                    lastGraphX = x;
                    lastGraphY = y;
                }
            }

            // Завершаем график
            if (!firstPoint) {
                path.lineTo(lastGraphX, lastGraphY);
                canvas.drawPath(path, linePaint);
            }
        }

        Log.d("MoodChart", "График и сетка обновлены!");
    }


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        Log.d("MoodChart", "onSizeChanged: width=" + w + ", height=" + h);
    }
}


