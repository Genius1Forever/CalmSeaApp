package com.example.calmsea;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.FieldValue;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
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
    private String fullRecommendation;
    private ListenerRegistration statusListener;
    private ListenerRegistration notesListener;
    private ListenerRegistration listenerRegistration;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getParentFragmentManager().setFragmentResultListener("note_update", this, (requestKey, result) -> {
            if (result.getBoolean("note_added", false)) {
                fetchNotesFromFirestore(); // Загружаем новые заметки
            }
        });
    }
    private final ActivityResultLauncher<Intent> addNoteLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    fetchNotesFromFirestore(); // Обновляем список заметок
                }
            }
    );

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
            Intent intent = new Intent(getContext(), MoodQuestionActivity.class);
            addNoteLauncher.launch(intent); // Теперь ждем результат
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
                Timestamp timestamp = note.getDate();
                String formattedDate = "";
                boolean isDateChanged = false;

                if (timestamp != null) {
                    Date date = timestamp.toDate();

                    // Форматы даты
                    SimpleDateFormat fullFormat = new SimpleDateFormat("EEEE, dd MMMM yyyy, HH:mm", Locale.getDefault());
                    SimpleDateFormat shortFormat = new SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault());

                    String fullDateStr = fullFormat.format(date);

                    // Проверяем, содержит ли дата "00:00"
                    if (fullDateStr.endsWith(", 00:00")) {
                        formattedDate = shortFormat.format(date);
                        isDateChanged = true; // Фиксируем, что дата была изменена пользователем
                    } else {
                        formattedDate = fullDateStr;
                    }
                }

                Intent intent = new Intent(getContext(), NoteEditActivity.class);
                intent.putExtra("noteId", note.getId());
                intent.putExtra("noteText", note.getNoteText());
                intent.putExtra("noteMood", note.getMood());
                intent.putExtra("noteDate", formattedDate); // Уже отформатированная дата
                intent.putExtra("DATE_CHANGED", isDateChanged); // Передаем флаг

                startActivity(intent);
            } else {
                Toast.makeText(getContext(), "Ошибка: ID заметки не найден!", Toast.LENGTH_SHORT).show();
            }
        });
        viewPager.setAdapter(adapter);

        // Firestore
        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();


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

        recommendationTextView.setOnClickListener(v -> {
            if (fullRecommendation != null) {
                Intent intent = new Intent(getActivity(), FullRecommendationActivity.class);
                intent.putExtra("recommendation_text", fullRecommendation);
                startActivity(intent);
            }
        });

        getParentFragmentManager().setFragmentResultListener("note_added_result", this, (key, bundle) -> {
            loadRecommendation(userId); // или другой метод обновления
        });

        return view;
    }
    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            listenToRecommendationStatus(user.getUid());
            userId = user.getUid();
            fetchNotesFromFirestore();
        }
    }
    private void listenToRecommendationStatus(String userId) {
        statusListener = db.collection("users").document(userId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null || !snapshot.exists()) return;

                    FragmentActivity activity = getActivity();
                    View view = getView();
                    if (!isAdded() || activity == null || view == null) return;

                    String status = snapshot.getString("recommendationStatus");

                    if ("generating".equals(status)) {
                        recommendationTextView.setText("Загрузка новой рекомендации...");
                    } else if ("ready".equals(status)) {
                        loadRecommendation(userId);
                        db.collection("users").document(userId)
                                .update("recommendationStatus", FieldValue.delete());
                    }
                });
    }
    @Override
    public void onResume() {
        super.onResume();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            loadRecommendation(user.getUid());  // Загружаем рекомендацию
            loadMoodData();                     // Загружаем и обновляем график
        }
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
        if (userId == null || userId.isEmpty() || !isAdded()) return;

        // Отписываемся от предыдущего слушателя, если он существует
        if (notesListener != null) {
            notesListener.remove();
            notesListener = null;
        }

        notesListener = db.collection("users")
                .document(userId)
                .collection("notes")
                .orderBy("date", Query.Direction.DESCENDING)
                .addSnapshotListener((queryDocumentSnapshots, error) -> {
                    if (!isAdded()) return; // Проверяем, что фрагмент все еще прикреплен

                    if (error != null) {
                        Log.e("HouseFragment", "Ошибка загрузки заметок", error);
                        return;
                    }
                    if (queryDocumentSnapshots == null) return;

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

                    if (isAdded() && adapter != null) {
                        adapter.updateNotes(notes);
                    }
                });
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Отписываемся от всех слушателей
        if (statusListener != null) {
            statusListener.remove();
            statusListener = null;
        }

        if (notesListener != null) {
            notesListener.remove();
            notesListener = null;
        }

        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }

        // Очищаем ссылки на View
        dateRangeText = null;
        moodChartView = null;
        recommendationTextView = null;
        adapter = null;
    }

    private void loadMoodData() {
        // Определяем начало и конец текущей недели
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat storageFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()); // Для Firestore-сортировки
        SimpleDateFormat displayFormat = new SimpleDateFormat("EEEE, dd MMMM yyyy, HH:mm", new Locale("ru")); // Для отображения
        SimpleDateFormat userFormat = new SimpleDateFormat("EEEE, dd MMMM yyyy", new Locale("ru")); // Если пользователь выбрал дату вручную

        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        String startOfWeek = storageFormat.format(calendar.getTime());
        calendar.add(Calendar.DAY_OF_WEEK, 6);
        String endOfWeek = storageFormat.format(calendar.getTime());

        try {
            // Преобразуем границы недели в Date
            Date startDate = storageFormat.parse(startOfWeek);
            Date endDate = storageFormat.parse(endOfWeek);

            Log.d("MoodChart", "Start of week: " + startOfWeek);
            Log.d("MoodChart", "End of week: " + endOfWeek);

            // Запрос к Firestore
            db.collection("users").document(userId).collection("notes")
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            int[] moodValues = new int[7];
                            boolean[] hasData = new boolean[7];

                            Log.d("MoodChart", "Total notes found: " + task.getResult().size());

                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Object dateField = document.get("date"); // Дата может быть разного типа
                                String mood = document.getString("mood");
                                String formattedDate = null;

                                if (dateField instanceof Timestamp) {
                                    // Если дата в формате Timestamp
                                    Date date = ((Timestamp) dateField).toDate();
                                    formattedDate = storageFormat.format(date);
                                } else if (dateField instanceof String) {
                                    // Если дата в формате строки (выбранная пользователем)
                                    try {
                                        Date date = userFormat.parse((String) dateField);
                                        formattedDate = storageFormat.format(date);
                                    } catch (ParseException e) {
                                        Log.e("MoodChart", "Error parsing user-selected date: " + dateField, e);
                                    }
                                }

                                if (formattedDate != null) {
                                    Log.d("MoodChart", "Checking date: " + formattedDate);

                                    if (formattedDate.compareTo(startOfWeek) >= 0 && formattedDate.compareTo(endOfWeek) <= 0) {
                                        int dayIndex = getDayIndex(formattedDate);
                                        int moodValue = getMoodValue(mood);

                                        Log.d("MoodChart", "Processing note - Date: " + formattedDate + ", Mood: " + mood);

                                        if (dayIndex >= 0 && dayIndex < 7) {
                                            moodValues[dayIndex] = moodValue;
                                            hasData[dayIndex] = true;
                                        }
                                    }
                                }
                            }

                            // Заполняем дни, где нет данных
                            for (int i = 0; i < 7; i++) {
                                if (!hasData[i]) {
                                    moodValues[i] = 0;
                                }
                            }

                            Log.d("MoodChart", "Processed Mood Values: " + Arrays.toString(moodValues));
                            moodChartView.setMoodValues(moodValues);
                        } else {
                            Log.e("MoodChart", "Error loading notes", task.getException());
                        }
                    });

        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
    private int getDayIndex(String date) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date d = sdf.parse(date);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(d);

            int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);

            // Firestore Sunday = 1, но у нас Понедельник = 0, передвигаем индексы
            int index = (dayOfWeek + 5) % 7;

            Log.d("MoodChart", "Converted date " + date + " to day index: " + index);
            return index;
        } catch (ParseException e) {
            e.printStackTrace();
            return -1; // Ошибка
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
        Log.d("MoodChart", "Mood: " + mood + " -> Value: " + value);
        return value;
    }


    private void sortNotesByDate(List<NoteModel> notes) {
        Collections.sort(notes, (note1, note2) -> note2.getDate().compareTo(note1.getDate()));
    }

    private void openCalendarActivity() {
        startActivity(new Intent(getActivity(), CalendarActivity.class));
    }


    private void loadRecommendation(String userId) {
        if (userId == null || userId.isEmpty() || !isAdded()) {
            Log.e("HouseFragment", "Ошибка: userId пустой или null при загрузке рекомендаций.");
            return;
        }

        if (recommendationTextView != null) {
            recommendationTextView.setText("Загрузка рекомендации...");
        }

        // Отписываемся от предыдущего слушателя, если он существует
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }

        listenerRegistration = db.collection("users").document(userId)
                .collection("recommendations")
                .orderBy("date", Query.Direction.DESCENDING)
                .limit(1)
                .addSnapshotListener((queryDocumentSnapshots, error) -> {
                    if (!isAdded()) return; // Проверяем, что фрагмент все еще прикреплен

                    if (error != null) {
                        Log.e("HouseFragment", "Ошибка загрузки рекомендации", error);
                        if (recommendationTextView != null) {
                            recommendationTextView.setText("Ошибка загрузки рекомендации");
                        }
                        return;
                    }

                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                        String recommendation = document.getString("text");
                        if (recommendation != null && !recommendation.isEmpty()) {
                            fullRecommendation = recommendation;
                            if (recommendationTextView != null) {
                                showRecommendation(recommendation);
                            }
                        }
                    } else if (recommendationTextView != null) {
                        recommendationTextView.setText("Нет рекомендаций");
                    }
                });
    }

    private void showRecommendation(String recommendation) {
        fullRecommendation = recommendation;

        Activity activity = getActivity();
        if (activity == null || !isAdded() || getView() == null) return;

        activity.runOnUiThread(() -> {
            if (recommendationTextView != null) {
                SpannableStringBuilder preview = new SpannableStringBuilder("🍃 ");
                preview.append(getPreviewText(recommendation));
                recommendationTextView.setText(preview);
            }
        });
    }

    private SpannableStringBuilder getPreviewText(String fullText) {
        if (fullText == null || fullText.trim().isEmpty()) {
            return new SpannableStringBuilder("Рекомендация недоступна");
        }

        String[] words = fullText.trim().split("\\s+");
        int half = Math.max(1, words.length / 2);  // хотя бы одно слово

        StringBuilder previewBuilder = new StringBuilder();
        for (int i = 0; i < half; i++) {
            previewBuilder.append(words[i]).append(" ");
        }

        String mainPart = previewBuilder.toString().trim();
        String suffix = "… Нажмите, чтобы читать дальше";

        SpannableStringBuilder spannable = new SpannableStringBuilder(mainPart + " " + suffix);
        int start = spannable.length() - suffix.length();

        // Добавляем подчеркивание
        spannable.setSpan(
                new UnderlineSpan(),
                start,
                spannable.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        return spannable;
    }

}
