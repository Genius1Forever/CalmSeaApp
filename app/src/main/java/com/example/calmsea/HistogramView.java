package com.example.calmsea;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.List;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import androidx.core.content.ContextCompat;
import java.util.List;

public class HistogramView extends View {
    private Paint barPaint;
    private Paint textPaint;
    private List<Float> avgMoods;

    public HistogramView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        barPaint = new Paint();
        barPaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(30);
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setAvgMoods(List<Float> avgMoods) {
        this.avgMoods = avgMoods;
        invalidate(); // Перерисовка
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (avgMoods == null || avgMoods.isEmpty()) return;

        int width = getWidth();
        int height = getHeight();
        int padding = 50;
        int barWidth = (width - padding * 2) / 12;
        float maxMood = 5.0f; // Максимальный уровень настроения

        Paint gridPaint = new Paint();
        gridPaint.setColor(Color.BLACK); // Цвет линий сетки
        gridPaint.setStrokeWidth(2);
        gridPaint.setStyle(Paint.Style.STROKE);

        // Нарисовать горизонтальные линии
        int numLines = 5; // Количество линий (по уровням настроения)
        for (int i = 0; i <= numLines; i++) {
            float y = height - 100 - (i * (height - 200) / numLines);
            canvas.drawLine(padding, y, width - padding, y, gridPaint);
        }
        for (int i = 0; i < avgMoods.size(); i++) {
            float mood = avgMoods.get(i);
            float barHeight = (mood / maxMood) * (height - 200); // Высота столбца
            int x = padding + i * barWidth;
            int y = height - (int) barHeight - 100; // Смещение вверх

            barPaint.setColor(getMoodColor(mood));
            canvas.drawRect(x, y, x + barWidth - 10, height - 100, barPaint); // Отрисовка столбца

            canvas.drawText(String.valueOf(i + 1), x + barWidth / 2 - 5, height - 50, textPaint);
        }
    }
    private int getMoodColor(float mood) {
        if (mood >= 4.5f) return Color.parseColor("#FFF9C4"); // Отличное (Светло-желтый)
        if (mood >= 3.5f) return Color.parseColor("#C8E6C9"); // Хорошее (Светло-зеленый)
        if (mood >= 2.5f) return Color.parseColor("#BBDEFB"); // Нормальное (Светло-голубой)
        if (mood >= 1.5f) return Color.parseColor("#FFE0B2"); // Плохое (Светло-оранжевый)
        return Color.parseColor("#FFCDD2"); // Ужасное (Светло-красный)
    }
}


