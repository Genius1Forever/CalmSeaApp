package com.example.calmsea;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MoodQuestionActivity extends AppCompatActivity {
    private String selectedMood = "";
    private String selectedDate;
    private boolean isDateChanged = false; // Флаг, изменял ли пользователь дату
    private static final int REQUEST_CODE_ADD_NOTE = 1001;

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

        // Устанавливаем текущую дату с временем (если пользователь не меняет дату)
        selectedDate = new SimpleDateFormat("EEEE, dd MMMM yyyy, HH:mm", Locale.getDefault()).format(new Date());
        isDateChanged = false; // Изначально дата не изменена

        TextView dateTextView = findViewById(R.id.tv_current_date);
        dateTextView.setText(selectedDate);

        // Кнопка изменения даты
        ImageButton changeDateButton = findViewById(R.id.btn_change_date);
        changeDateButton.setOnClickListener(v -> showDatePickerDialog());


    }

    private void saveMoodAndProceed(String mood) {
        selectedMood = mood;
        sendMoodToAddNoteActivity();
    }

    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year1, month1, dayOfMonth) -> {
            Calendar selectedCalendar = Calendar.getInstance();
            selectedCalendar.set(year1, month1, dayOfMonth);

            // Устанавливаем дату без времени
            selectedDate = new SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(selectedCalendar.getTime());

            TextView dateTextView = findViewById(R.id.tv_current_date);
            dateTextView.setText(selectedDate);

            isDateChanged = true; // Фиксируем, что дата изменена
            Log.d("DatePicker", "Date changed by user. isDateChanged set to: " + isDateChanged);
        }, year, month, day);

        datePickerDialog.getDatePicker().setMaxDate(calendar.getTimeInMillis());
        datePickerDialog.show();
    }

    private void sendMoodToAddNoteActivity() {
        Intent intent = new Intent(MoodQuestionActivity.this, AddNoteActivity.class);
        intent.putExtra("SELECTED_MOOD", selectedMood);

        // Если дата была изменена пользователем, передаем без времени
        intent.putExtra("SELECTED_DATE", selectedDate);
        intent.putExtra("DATE_CHANGED", isDateChanged); // Передаем флаг
        Log.d("MoodQuestionActivity", "Sending mood and date. isDateChanged: " + isDateChanged);

        // Передаем результат дальше и завершаем MoodQuestionActivity
        intent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
        finish();

        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_ADD_NOTE && resultCode == RESULT_OK && data != null) {
            boolean noteAdded = data.getBooleanExtra("note_added", false);
            if (noteAdded) {
                // Отправляем результат в HouseFragment
                Intent intent = new Intent();
                intent.putExtra("note_added", true);
                setResult(Activity.RESULT_OK, intent);
                finish();
            }
        }
    }
}





