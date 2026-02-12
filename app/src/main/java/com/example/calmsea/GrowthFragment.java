package com.example.calmsea;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GrowthFragment extends Fragment {
    private MonthlyMoodChartView moodChartMonthView;
    private HistogramView histogramView;
    private FirebaseFirestore db;
    // Определяем формат даты
    // Формат с временем (используется по умолчанию в Firestore)
    private final SimpleDateFormat firestoreDateFormatFull = new SimpleDateFormat("EEEE, dd MMMM yyyy, HH:mm", Locale.getDefault());

    // Формат без времени (если пользователь изменил дату вручную)
    private final SimpleDateFormat firestoreDateFormatShort = new SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault());


    private String userId; private TextView yearTextView;
    private Calendar calendar; private TextView textMonth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_growth, container, false);

        // Инициализация UI элементов
        moodChartMonthView = view.findViewById(R.id.monthlymoodChartView);

        textMonth = view.findViewById(R.id.dateRangeText1);
        HorizontalScrollView horizontalScrollView = view.findViewById(R.id.horizontalScrollView);
        horizontalScrollView.post(() -> horizontalScrollView.scrollTo(0, 0));
        moodChartMonthView.getLayoutParams().width = 2000; // Или другой размер
        moodChartMonthView.requestLayout();

        histogramView = view.findViewById(R.id.histogramView);

        calendar = Calendar.getInstance();
        db = FirebaseFirestore.getInstance();
        updateMonth();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            userId = currentUser.getUid();
            loadMoodDataForMonth();
        }
        getActivity().runOnUiThread(() -> histogramView.invalidate());
        loadMoodData();
        yearTextView = view.findViewById(R.id.dateRangeText2);
        updateYear();

        return view;
    }

    private void loadMoodDataForMonth() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            userId = user.getUid();
        } else {
            Log.e("GrowthFragment", "Ошибка: User is null");
            return;
        }

        if (userId == null) {
            Log.e("GrowthFragment", "Ошибка: userId равен null");
            return;
        }

        Log.d("GrowthFragment", "User ID: " + userId);

        if (moodChartMonthView != null) {
            Log.d("MoodChartMonth", "Очищаем старые данные перед загрузкой...");
            moodChartMonthView.setMoodValues(new int[31]); // Обнуляем массив настроений
            moodChartMonthView.invalidate();
        }

        // Определяем начало и конец месяца
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat firestoreFormatFull = new SimpleDateFormat("EEEE, dd MMMM yyyy, HH:mm", Locale.getDefault());
        SimpleDateFormat firestoreFormatShort = new SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault());

        calendar.set(Calendar.DAY_OF_MONTH, 1);
        String startOfMonth = sdf.format(calendar.getTime());
        int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        calendar.set(Calendar.DAY_OF_MONTH, daysInMonth);
        String endOfMonth = sdf.format(calendar.getTime());

        Log.d("MoodChartMonth", "Start of month: " + startOfMonth);
        Log.d("MoodChartMonth", "End of month: " + endOfMonth);

        // Загружаем ВСЕ заметки, так как нельзя фильтровать строки в Firestore
        db.collection("users").document(userId).collection("notes")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        int[] moodValues = new int[daysInMonth];
                        boolean[] hasData = new boolean[daysInMonth];

                        Log.d("MoodChartMonth", "Total notes found in DB: " + task.getResult().size());

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Timestamp timestamp = document.getTimestamp("date");
                            Date noteDate = null;

                            if (timestamp != null) {
                                noteDate = timestamp.toDate(); // Если дата в формате Timestamp, получаем Date
                            } else {
                                // Если дата хранится как строка, пытаемся ее распарсить
                                String dateString = document.getString("date");
                                if (dateString != null) {
                                    try {
                                        if (dateString.length() > 12) { // Проверяем наличие времени в формате
                                            noteDate = firestoreFormatFull.parse(dateString);
                                        } else {
                                            noteDate = firestoreFormatShort.parse(dateString);
                                        }
                                    } catch (ParseException e) {
                                        Log.e("MoodChartMonth", "Ошибка парсинга даты: " + dateString, e);
                                        continue; // Пропускаем ошибочные записи
                                    }
                                }
                            }

                            if (noteDate == null) continue;

                            String formattedNoteDate = sdf.format(noteDate);
                            String mood = document.getString("mood");

                            if (mood == null) continue;

                            Log.d("MoodChartMonth", "Checking date: " + formattedNoteDate);

                            // Проверяем, входит ли дата в текущий месяц
                            if (formattedNoteDate.compareTo(startOfMonth) >= 0 && formattedNoteDate.compareTo(endOfMonth) <= 0) {
                                int dayIndex = getDayIndex(formattedNoteDate);
                                int moodValue = getMoodValue(mood);

                                Log.d("MoodChartMonth", "Processing note - Date: " + formattedNoteDate + ", Mood: " + mood);

                                if (dayIndex >= 0 && dayIndex < daysInMonth) {
                                    if (!hasData[dayIndex]) {
                                        moodValues[dayIndex] = moodValue;
                                    } else {
                                        moodValues[dayIndex] = (moodValues[dayIndex] + moodValue) / 2; // Усреднение
                                    }
                                    hasData[dayIndex] = true;
                                }
                            }
                        }

                        for (int i = 0; i < daysInMonth; i++) {
                            if (!hasData[i]) {
                                moodValues[i] = 0;
                            }
                        }

                        Log.d("MoodChartMonth", "Передача данных в setMoodValues: " + Arrays.toString(moodValues));
                        if (moodChartMonthView == null) {
                            Log.e("MoodChartMonth", "moodChartMonthView == null! График не найден!");
                            return;
                        }

                        Log.d("MoodChartMonth", "moodChartMonthView найден, передача данных...");
                        moodChartMonthView.setMoodValues(moodValues);
                        moodChartMonthView.invalidate();
                    } else {
                        Log.e("MoodChartMonth", "Error loading notes", task.getException());
                    }
                });
    }

    private int getDayIndex(String formattedDate) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = sdf.parse(formattedDate);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            return calendar.get(Calendar.DAY_OF_MONTH) - 1;
        } catch (ParseException e) {
            e.printStackTrace();
            return -1;
        }
    }
   private int getMoodValue(String mood) {
        int value;
        switch (mood) {
            case "Отличное": value = 5; break;
            case "Хорошее": value = 4; break;
            case "Нормальное": value = 3; break;
            case "Плохое": value = 2; break;
            case "Ужасное": value = 1; break;
            default: value = 0;
        }
        Log.d("MoodChartMonth", "Mood: " + mood + " -> Value: " + value);
        return value;
    }
    private void updateMonth() {
        SimpleDateFormat sdf = new SimpleDateFormat("LLLL", new Locale("ru"));
        String formattedMonth = sdf.format(calendar.getTime());
        textMonth.setText(formattedMonth.substring(0, 1).toUpperCase() + formattedMonth.substring(1));

        loadMoodDataForMonth();
    }
    private void loadMoodData() {
        db.collection("users").document(userId).collection("notes")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Map<Integer, List<Float>> monthlyMoods = new HashMap<>();

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Timestamp timestamp = doc.getTimestamp("date");
                        Date date = null;

                        if (timestamp != null) {
                            date = timestamp.toDate(); // Если дата в формате Timestamp
                        } else {
                            // Если дата хранится как строка, пытаемся ее распарсить
                            String dateString = doc.getString("date");
                            if (dateString != null) {
                                try {
                                    if (dateString.length() > 12) { // Проверяем наличие времени в формате
                                        date = firestoreDateFormatFull.parse(dateString);
                                    } else {
                                        date = firestoreDateFormatShort.parse(dateString);
                                    }
                                } catch (ParseException e) {
                                    Log.e("DateParsing", "Ошибка преобразования даты: " + dateString, e);
                                    continue; // Пропускаем ошибочные записи
                                }
                            }
                        }

                        if (date == null) continue;

                        String moodStr = doc.getString("mood"); // mood как строка

                        // Логируем, что получили
                        Log.d("FirestoreData", "Загружено: date=" + date + ", mood=" + moodStr);

                        if (moodStr == null) continue;

                        Calendar calendar = Calendar.getInstance();
                        calendar.setTime(date);
                        int month = calendar.get(Calendar.MONTH); // 0 - Январь, 11 - Декабрь

                        // Преобразуем строку настроения в число
                        float moodValue = convertMoodToFloat(moodStr);

                        // Добавляем значение в Map
                        if (!monthlyMoods.containsKey(month)) {
                            monthlyMoods.put(month, new ArrayList<>());
                        }
                        monthlyMoods.get(month).add(moodValue);

                        Log.d("MoodProcessing", "Добавлено: месяц=" + month + ", настроение=" + moodValue);
                    }

                    // Вычисляем среднее настроение для каждого месяца
                    List<Float> avgMoods = new ArrayList<>();
                    for (int i = 0; i < 12; i++) {
                        List<Float> moods = monthlyMoods.getOrDefault(i, new ArrayList<>());
                        if (moods.isEmpty()) {
                            avgMoods.add(0f);
                        } else {
                            float sum = 0;
                            for (float m : moods) {
                                sum += m;
                            }
                            avgMoods.add(sum / moods.size());
                        }
                    }

                    Log.d("FinalData", "Средние настроения перед обновлением: " + avgMoods);

                    histogramView.setAvgMoods(avgMoods);
                    histogramView.postInvalidate();
                })
                .addOnFailureListener(e -> Log.e("FirestoreError", "Ошибка загрузки данных", e));
    }

    private float convertMoodToFloat(String mood) {
        switch (mood) {
            case "Отличное": return 5.0f;
            case "Хорошее": return 4.0f;
            case "Нормальное": return 3.0f;
            case "Плохое": return 2.0f;
            case "Ужасное": return 1.0f;
            default: return 0.0f; // Если настроение неизвестно
        }
    }
    private void updateYear() {
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        yearTextView.setText(String.valueOf(currentYear));
    }
}

