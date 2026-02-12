package com.example.calmsea;

import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import android.graphics.Color;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class CalendarActivity extends AppCompatActivity {
    private MaterialCalendarView calendarView;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private NotesAdapter notesAdapter;
    private List<NoteModel> notesList = new ArrayList<>();
    private Set<CalendarDay> eventDates = new HashSet<>();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, dd MMMM yyyy, HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        calendarView = findViewById(R.id.calendarView);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        notesAdapter = new NotesAdapter(notesList, note -> {
            Intent intent = new Intent(CalendarActivity.this, NoteEditActivity.class);
            intent.putExtra("noteId", note.getId());
            startActivity(intent);
        });

        loadNotesFromFirestore();

        findViewById(R.id.calendarAddNoteButton).setOnClickListener(v -> openMoodQuestionActivity());
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        calendarView.setOnDateChangedListener((widget, date, selected) -> {
            if (selected) {
                showNotesForSelectedDate(date);
            }
        });
        calendarView.setHeaderTextAppearance(R.style.CalendarTitleStyle);
    }

    private void openMoodQuestionActivity() {
        startActivity(new Intent(CalendarActivity.this, MoodQuestionActivity.class));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotesFromFirestore();
    }

    private void loadNotesFromFirestore() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;

        String userId = currentUser.getUid();
        db.collection("users").document(userId).collection("notes")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        eventDates.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Timestamp timestamp = document.getTimestamp("date");
                            if (timestamp != null) {
                                CalendarDay day = convertTimestampToCalendarDay(timestamp);
                                if (day != null) {
                                    eventDates.add(day);
                                }
                            }
                        }
                        updateCalendar();
                    } else {
                        Log.e("CalendarActivity", "Ошибка загрузки данных Firestore", task.getException());
                    }
                });
    }

    private CalendarDay convertTimestampToCalendarDay(Timestamp timestamp) {
        // Преобразование Timestamp в CalendarDay
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp.getSeconds() * 1000);  // Получаем время из timestamp
        return new CalendarDay(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
    }

    private void updateCalendar() {
        // Убираем старые декораторы и обновляем календарь
        calendarView.removeDecorators();
        calendarView.invalidateDecorators(); // Важно для обновления!

        // Добавляем новые декораторы с точками
        calendarView.addDecorator(new EventDecorator(Color.BLUE, eventDates));

        Log.d("CalendarActivity", "Календарь обновлен, количество дат: " + eventDates.size());
    }

    private void showNotesForSelectedDate(CalendarDay selectedDate) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;

        String userId = currentUser.getUid();

        db.collection("users").document(userId).collection("notes")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        notesList.clear();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Timestamp timestamp = document.getTimestamp("date");

                            if (timestamp != null) {
                                // Получаем календарь из Timestamp
                                Calendar noteCalendar = Calendar.getInstance();
                                noteCalendar.setTimeInMillis(timestamp.getSeconds() * 1000);  // Конвертируем timestamp в миллисекунды
                                // Сравниваем только ГОД, МЕСЯЦ и ДЕНЬ
                                if (noteCalendar.get(Calendar.YEAR) == selectedDate.getYear() &&
                                        noteCalendar.get(Calendar.MONTH) == selectedDate.getMonth() &&
                                        noteCalendar.get(Calendar.DAY_OF_MONTH) == selectedDate.getDay()) {

                                    NoteModel note = document.toObject(NoteModel.class);
                                    note.setId(document.getId()); // Передаём ID документа Firestore
                                    notesList.add(note);
                                }
                            }
                        }
                        notesAdapter.notifyDataSetChanged();

                        if (notesList.isEmpty()) {
                            Toast.makeText(CalendarActivity.this, "Нет заметок на этот день", Toast.LENGTH_SHORT).show();
                        } else {
                            showNotesBottomSheet(notesList);
                        }
                    } else {
                        Log.e("CalendarActivity", "Ошибка загрузки заметок", task.getException());
                    }
                });
    }

    private void showNotesBottomSheet(List<NoteModel> notes) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_notes, null);
        bottomSheetDialog.setContentView(view);

        TextView title = view.findViewById(R.id.tv_notes_title);
        RecyclerView recyclerView = view.findViewById(R.id.rv_notes1);
        Button closeButton = view.findViewById(R.id.btn_close);

        title.setText("Заметки за выбранный день");

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new SpaceItemDecoration(20));

        NotesAdapter adapter = new NotesAdapter(notes, note -> {
            Timestamp timestamp = note.getDate();
            if (timestamp != null) {
                Date date = timestamp.toDate();

                SimpleDateFormat fullFormat = new SimpleDateFormat("EEEE, dd MMMM yyyy, HH:mm", Locale.getDefault());
                SimpleDateFormat shortFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
                SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.getDefault());

                String fullDateStr = fullFormat.format(date);
                boolean isDateChanged = fullDateStr.endsWith(", 00:00");

                String noteDateStr;
                if (isDateChanged) {
                    String dayOfWeek = dayFormat.format(date);
                    String mainDate = shortFormat.format(date);
                    noteDateStr = dayOfWeek + ", " + mainDate;
                } else {
                    noteDateStr = fullDateStr;
                }

                // Передаём данные
                Intent intent = new Intent(CalendarActivity.this, NoteEditActivity.class);
                intent.putExtra("noteId", note.getId());
                intent.putExtra("noteText", note.getNoteText());
                intent.putExtra("noteMood", note.getMood());
                intent.putExtra("noteDate", noteDateStr); // ✅ Главное: передаем именно в "noteDate"
                intent.putExtra("DATE_CHANGED", isDateChanged);

                Log.d("CalendarActivity", "Передаём noteDate: " + noteDateStr);
                Log.d("CalendarActivity", "Флаг DATE_CHANGED: " + isDateChanged);

                startActivity(intent);
            }
        });

        recyclerView.setAdapter(adapter);

        closeButton.setOnClickListener(v -> bottomSheetDialog.dismiss());

        bottomSheetDialog.show();
    }
    // Класс для добавления отступов между элементами списка
    public class SpaceItemDecoration extends RecyclerView.ItemDecoration {
        private final int spaceHeight;

        public SpaceItemDecoration(int spaceHeight) {
            this.spaceHeight = spaceHeight;
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                                   @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            outRect.bottom = spaceHeight; // Отступ снизу у каждого элемента
        }
    }
}








