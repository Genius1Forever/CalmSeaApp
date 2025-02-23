package com.example.calmsea;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HouseFragment extends Fragment {

    private FirebaseFirestore db;
    private String userId;
    private NotesPagerAdapter adapter;

    private TextView dateRangeText;
    private MoodChartView moodChartView; // График настроений
    private TextView recommendationTextView;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_house, container, false);

        // Привязка элементов интерфейса
        TextView greetingTextView = view.findViewById(R.id.greetingTextView);
        TextView dayOfWeek = view.findViewById(R.id.dayOfWeek);
        TextView day = view.findViewById(R.id.day);
        TextView month = view.findViewById(R.id.month);
        dateRangeText = view.findViewById(R.id.dateRangeText);
        moodChartView = view.findViewById(R.id.moodChartView); // Привязка графика

        // Приветствие
        greetingTextView.setText(GreetingHelper.getGreeting());

        // Дата
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dayFormat = new SimpleDateFormat("EE", Locale.getDefault());
        SimpleDateFormat dayNumFormat = new SimpleDateFormat("d", Locale.getDefault());
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM", Locale.getDefault());

        dayOfWeek.setText(dayFormat.format(calendar.getTime()).toUpperCase());
        day.setText(dayNumFormat.format(calendar.getTime()));
        month.setText(monthFormat.format(calendar.getTime()));

        // Заголовок недели
        updateDateRange();

        // Календарный виджет
        RelativeLayout calendarWidget = view.findViewById(R.id.calendar_widget);
        calendarWidget.setOnClickListener(v -> openCalendarActivity());

        Button btnAddNote = view.findViewById(R.id.add_button);
        btnAddNote.setOnClickListener(v -> {
            startActivity(new Intent(getContext(), MoodQuestionActivity.class));
        });

        // Настройка ViewPager2
        ViewPager2 viewPager = view.findViewById(R.id.viewPager_notes);
        viewPager.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        viewPager.setPageTransformer(new ZoomOutPageTransformer());

        // Получение заметок
        List<NoteModel> notesList = new ArrayList<>();
        adapter = new NotesPagerAdapter(notesList, note -> {
            Log.d("NoteDebug", "ID заметки: " + note.getId());

            if (note.getId() != null && !note.getId().isEmpty()) {
                Intent intent = new Intent(getContext(), NoteEditActivity.class);
                intent.putExtra("noteId", note.getId());
                intent.putExtra("noteText", note.getNoteText());
                intent.putExtra("noteMood", note.getMood());
                intent.putExtra("noteDate", note.getDate());
                startActivity(intent);
            } else {
                Toast.makeText(getContext(), "Ошибка: ID заметки не найден!", Toast.LENGTH_SHORT).show();
            }
        });
        viewPager.setAdapter(adapter);

        // Firestore
        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        fetchNotesFromFirestore(); // Загрузка заметок
        loadMoodData(); // Загрузка данных для графика
        setupMoodChart();

        recommendationTextView = view.findViewById(R.id.textView_Recommendation);
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e("HouseFragment", "Ошибка: пользователь не авторизован");
            return view;
        }
        String userId = currentUser.getUid();
        if (userId == null) {
            Log.e("HouseFragment", "Ошибка: userId равен null, рекомендации не загрузятся.");
        }
        loadRecommendation(userId);

        return view;
    }
    private void setupMoodChart() {
        // Загрузка иконок настроений
        Bitmap[] moodIcons = new Bitmap[]{
                BitmapFactory.decodeResource(getResources(), R.drawable.happy_face),
                BitmapFactory.decodeResource(getResources(), R.drawable.smile_face),
                BitmapFactory.decodeResource(getResources(), R.drawable.neutral_face),
                BitmapFactory.decodeResource(getResources(), R.drawable.bad_face),
                BitmapFactory.decodeResource(getResources(), R.drawable.crying_face)
        };
        moodChartView.setMoodIcons(moodIcons);
    }
    private void updateDateRange() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM", Locale.getDefault());

        // Начало недели
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        String startOfWeek = sdf.format(calendar.getTime());

        // Конец недели
        calendar.add(Calendar.DAY_OF_WEEK, 6);
        String endOfWeek = sdf.format(calendar.getTime());

        dateRangeText.setText(String.format("%s - %s, %d", startOfWeek, endOfWeek, calendar.get(Calendar.YEAR)));
    }

    private void fetchNotesFromFirestore() {
        db.collection("users").document(userId).collection("notes")
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<NoteModel> notes = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        NoteModel note = document.toObject(NoteModel.class);
                        if (note != null) {
                            note.setId(document.getId());
                            if (note.getMood() != null && !note.getNoteText().isEmpty()) {
                                notes.add(note);
                            }
                        }
                    }
                    sortNotesByDate(notes);
                    adapter.setNotes(notes);
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Ошибка загрузки заметок", Toast.LENGTH_SHORT).show());
    }

    private void loadMoodData() {
        // Определяем начало и конец текущей недели
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        String startOfWeek = sdf.format(calendar.getTime());
        calendar.add(Calendar.DAY_OF_WEEK, 6);
        String endOfWeek = sdf.format(calendar.getTime());

        // Загружаем данные из коллекции "notes"
        db.collection("users").document(userId).collection("notes")
                .whereGreaterThanOrEqualTo("date", startOfWeek)
                .whereLessThanOrEqualTo("date", endOfWeek)
                .orderBy("date", Query.Direction.ASCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        // Подготовка данных для графика
                        int[] moodValues = new int[7]; // Уровни настроений для каждого дня недели
                        boolean[] hasData = new boolean[7]; // Флаги наличия данных для дней

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String date = document.getString("date");
                            String mood = document.getString("mood");

                            int dayIndex = getDayIndex(date); // Получаем индекс дня недели
                            int moodValue = getMoodValue(mood); // Преобразуем настроение в уровень

                            if (dayIndex >= 0 && dayIndex < 7) {
                                moodValues[dayIndex] = moodValue;
                                hasData[dayIndex] = true; // Отмечаем, что данные есть
                            }
                        }

                        // Заполняем пустые дни нейтральным значением
                        for (int i = 0; i < 7; i++) {
                            if (!hasData[i]) {
                                moodValues[i] = 3; // Нейтральное настроение
                            }
                        }

                        // Передаём данные в график
                        moodChartView.setMoodValues(moodValues);
                    } else {
                        Toast.makeText(getContext(), "Ошибка загрузки данных для графика", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private int getDayIndex(String date) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date d = sdf.parse(date);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(d);
            int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);

            return (dayOfWeek == Calendar.SUNDAY) ? 6 : dayOfWeek - 2; // Пн = 0, Вс = 6
        } catch (ParseException e) {
            e.printStackTrace();
            return -1; // Ошибка
        }
    }


    private int getMoodValue(String mood) {
        switch (mood) {
            case "Отличное": return 5;
            case "Хорошее": return 4;
            case "Нормальное": return 3;
            case "Плохое": return 2;
            case "Ужасное": return 1;
            default: return 0;
        }
    }

    private void sortNotesByDate(List<NoteModel> notes) {
        Collections.sort(notes, (note1, note2) -> note2.getDate().compareTo(note1.getDate()));
    }

    private void openCalendarActivity() {
        startActivity(new Intent(getActivity(), CalendarActivity.class));
    }


    private void loadRecommendation(String userId) {
        if (userId == null) {
            Log.e("HouseFragment", "Ошибка: userId равен null при загрузке рекомендаций.");
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String recommendation = documentSnapshot.getString("latestRecommendation");
                        if (recommendation != null) {
                            showRecommendation(recommendation);
                        } else {
                            Log.e("HouseFragment", "Рекомендация в Firestore пустая.");
                        }
                    } else {
                        Log.e("HouseFragment", "Документ пользователя не найден.");
                    }
                })
                .addOnFailureListener(e -> Log.e("HouseFragment", "Ошибка загрузки рекомендаций: " + e.getMessage()));
    }

    // Метод для обновления TextView с рекомендацией
    private void showRecommendation(String recommendation) {
        requireActivity().runOnUiThread(() -> {
            if (recommendationTextView != null) {
                recommendationTextView.setText(recommendation);
            }
        });
    }

}
