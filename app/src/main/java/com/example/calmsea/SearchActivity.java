package com.example.calmsea;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.example.calmsea.NoteModel;

public class SearchActivity extends AppCompatActivity {

    private EditText searchEditText;
    private RecyclerView searchResultsRecyclerView;
    private NotesAdapter notesAdapter;
    private List<NoteModel> notesList = new ArrayList<>(); // Исходные заметки
    private List<NoteModel> filteredList = new ArrayList<>(); // Отфильтрованные заметки
    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        searchEditText = findViewById(R.id.searchEditText);
        searchResultsRecyclerView = findViewById(R.id.searchResultsRecyclerView);

        searchResultsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        notesAdapter = new NotesAdapter(filteredList, this::onNoteClick);
        searchResultsRecyclerView.setAdapter(notesAdapter);

        searchResultsRecyclerView.addItemDecoration(new JournalFragment.SpacesItemDecoration(14)); // 14dp отступ

        // Загружаем все заметки сразу
        loadAllNotes();

        // Обрабатываем ввод текста для поиска
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                filterNotes(charSequence.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });
        findViewById(R.id.btn_back_search).setOnClickListener(v -> finish());
    }

    private void loadAllNotes() {
        Log.d("SearchActivity", "Метод loadAllNotes() вызван");
        db.collection("users").document(userId).collection("notes")
        .get()
                .addOnSuccessListener(querySnapshot -> {
                    notesList.clear(); // Очищаем список перед добавлением новых данных
                    for (DocumentSnapshot doc : querySnapshot) {
                        NoteModel note = doc.toObject(NoteModel.class);
                        if (note != null) {
                            note.setId(doc.getId());
                            notesList.add(note);
                            Log.d("SearchActivity", "Загружена заметка: " + note.getNoteText());
                        }
                    }
                    notesAdapter.notifyDataSetChanged(); // Обновляем адаптер после загрузки
                })
                .addOnFailureListener(e -> Log.e("SearchActivity", "Ошибка загрузки заметок", e));
    }
    private void filterNotes(String query) {
        filteredList.clear();
        for (NoteModel note : notesList) {
            if (note.getNoteText() != null && note.getNoteText().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(note);
            }
        }
        Log.d("SearchActivity", "Фильтр по: " + query + " | Найдено заметок: " + filteredList.size());
        notesAdapter.notifyDataSetChanged();
    }

    private void onNoteClick(NoteModel note) {
        Intent intent = new Intent(this, NoteEditActivity.class);
        intent.putExtra("noteId", note.getId()); // Передача ID заметки
        intent.putExtra("noteText", note.getNoteText());
        intent.putExtra("noteMood", note.getMood());
        if (note.getDate() != null) {
            Timestamp timestamp = note.getDate();
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

            intent.putExtra("noteDate", noteDateStr);
            intent.putExtra("DATE_CHANGED", isDateChanged);

            Log.d("SearchActivity", "Передаём noteDate: " + noteDateStr);
            Log.d("SearchActivity", "Флаг изменения: " + isDateChanged);
        } else {
            intent.putExtra("noteDate", "Дата не указана");
            intent.putExtra("DATE_CHANGED", false);
        }

        startActivity(intent);
    }
}

