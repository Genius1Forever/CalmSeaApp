package com.example.calmsea;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.viewpager2.widget.ViewPager2;

public class ZoomOutPageTransformer implements ViewPager2.PageTransformer {
    private static final float MIN_SCALE = 0.85f; // Минимальный масштаб страницы
    private static final float MIN_ALPHA = 0.5f; // Минимальная прозрачность страницы

    @Override
    public void transformPage(@NonNull View page, float position) {
        int pageWidth = page.getWidth();
        int pageHeight = page.getHeight();

        if (position < -1) {
            // Страница слишком далеко влево
            page.setAlpha(0f);

        } else if (position <= 1) {
            // Страница видима
            float scaleFactor = Math.max(MIN_SCALE, 1 - Math.abs(position));
            float marginOffset = pageWidth * (1 - scaleFactor) / 2;

            // Смещение с учетом вертикального и горизонтального отступа
            page.setTranslationX(position < 0 ? marginOffset : -marginOffset);

            // Установка масштаба и прозрачности
            page.setScaleX(scaleFactor);
            page.setScaleY(scaleFactor);
            page.setAlpha(MIN_ALPHA + (scaleFactor - MIN_SCALE) / (1 - MIN_SCALE) * (1 - MIN_ALPHA));

        } else {
            // Страница слишком далеко вправо
            page.setAlpha(0f);
        }
    }
}

