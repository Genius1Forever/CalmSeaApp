package com.example.calmsea;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MoodQuestionActivity extends AppCompatActivity {
    // Добавить переменные для хранения выбранного настроения и цвета
    private String selectedMood = "";
    private String selectedDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mood_question);

        // Кнопка закрытия
        findViewById(R.id.btn_close).setOnClickListener(v -> finish());

        // Обработчики нажатия на смайлики
        findViewById(R.id.iv_mood_excellent).setOnClickListener(v -> saveMoodAndProceed("Отличное"));
        findViewById(R.id.iv_mood_good).setOnClickListener(v -> saveMoodAndProceed("Хорошее"));
        findViewById(R.id.iv_mood_normal).setOnClickListener(v -> saveMoodAndProceed("Нормальное"));
        findViewById(R.id.iv_mood_bad).setOnClickListener(v -> saveMoodAndProceed("Плохое"));
        findViewById(R.id.iv_mood_terrible).setOnClickListener(v -> saveMoodAndProceed("Ужасное"));

        // Устанавливаем текущую дату
        selectedDate = new SimpleDateFormat("EEEE, dd MMMM yyyy, HH:mm", Locale.getDefault()).format(new Date());
        TextView dateTextView = findViewById(R.id.tv_current_date);
        dateTextView.setText(selectedDate);

        // Кнопка изменения даты
        ImageButton changeDateButton = findViewById(R.id.btn_change_date);
        changeDateButton.setOnClickListener(v -> showDatePickerDialog());

        // Обработка кнопки "Пропустить"
        Button btnSkip = findViewById(R.id.btn_skip);
        btnSkip.setOnClickListener(v -> {
            // Переход на AddNoteActivity без настроения
            Intent intent = new Intent(MoodQuestionActivity.this, AddNoteActivity.class);
            intent.putExtra("SELECTED_MOOD", selectedMood); // Передаем текущее настроение (если выбрано)
            intent.putExtra("SELECTED_DATE", selectedDate); // Передаем текущую дату
            startActivity(intent);
            finish();
        });
    }

    private void saveMoodAndProceed(String mood) {
        selectedMood = mood; // Сохраняем выбранное настроение
        sendMoodToAddNoteActivity(mood);
    }

    private void sendMoodToAddNoteActivity(String mood) {
        Intent intent = new Intent(MoodQuestionActivity.this, AddNoteActivity.class);
        intent.putExtra("SELECTED_MOOD", mood); // Передаем настроение
        intent.putExtra("SELECTED_DATE", selectedDate); // Передаем дату
        startActivity(intent);
        finish(); // Закрываем текущую активность
    }
    private void showDatePickerDialog() {
        // Получаем текущую дату
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        // Создаем DatePickerDialog
        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year1, month1, dayOfMonth) -> {
            // Создаем объект Calendar и устанавливаем выбранную дату
            Calendar selectedCalendar = Calendar.getInstance();
            selectedCalendar.set(year1, month1, dayOfMonth);

            // Форматируем дату
            selectedDate = new SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault())
                    .format(selectedCalendar.getTime());

            // Устанавливаем выбранную дату в TextView
            TextView dateTextView = findViewById(R.id.tv_current_date);
            dateTextView.setText(selectedDate);
        }, year, month, day);

        // Устанавливаем максимальную дату на текущий день
        datePickerDialog.getDatePicker().setMaxDate(calendar.getTimeInMillis());

        // Показываем диалог выбора даты
        datePickerDialog.show();
    }
}


