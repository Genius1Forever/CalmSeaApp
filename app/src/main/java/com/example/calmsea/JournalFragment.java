package com.example.calmsea;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class JournalFragment extends Fragment {
    private FirebaseFirestore db;
    private String userId; private NotesAdapter adapter; //экземпляр адаптера
    private ActivityResultLauncher<Intent> addNoteLauncher;

    public JournalFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_journal, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.rv_notes);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new NotesAdapter(new ArrayList<>(), note -> {
            Intent intent = new Intent(getContext(), NoteEditActivity.class);
            intent.putExtra("noteId", note.getId());
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
                Log.d("JournalFragment", "Передаём noteDate: " + noteDateStr);
                Log.d("JournalFragment", "Флаг изменения: " + isDateChanged);
            } else {
                intent.putExtra("noteDate", "Дата не указана");
                intent.putExtra("DATE_CHANGED", false);
            }

            startActivity(intent);
        });

        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(new SpacesItemDecoration(16));

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        fetchNotesFromFirestore();

        //  Регистрируем обработчик результата
        addNoteLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        fetchNotesFromFirestore(); // Обновляем заметки
                    }
                }
        );

        LinearLayout addNoteButton = view.findViewById(R.id.addNoteButton);
        addNoteButton.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), MoodQuestionActivity.class);
            addNoteLauncher.launch(intent); //  запускаем с ожиданием результата
        });

        ImageButton button1 = view.findViewById(R.id.btn_change_date);
        button1.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), CalendarActivity.class);
            startActivity(intent);
        });

        ImageButton button = view.findViewById(R.id.btn_search);
        button.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), SearchActivity.class);
            startActivity(intent);
        });

        return view;
    }
    @Override
    public void onResume() {
        super.onResume();
        fetchNotesFromFirestore(); // Обновим заметки при возвращении на экран
    }

    private void fetchNotesFromFirestore() {
        db.collection("users").document(userId).collection("notes")
                .orderBy("date", Query.Direction.DESCENDING) // Сортировка на уровне Firestore
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<NoteModel> notes = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        NoteModel note = document.toObject(NoteModel.class);
                        if (note != null && note.getMood() != null && !note.getNoteText().isEmpty()) {
                            note.setId(document.getId()); // Устанавливаем ID из Firebase

                            // Устанавливаем дату (если она есть)
                            note.setDate(document.getTimestamp("date"));

                            // Проверяем, есть ли флаг dateChanged в Firestore
                            Boolean dateChanged = document.getBoolean("dateChanged");
                            note.setDateChanged(dateChanged != null && dateChanged);

                            // Логируем значение dateChanged
                            Log.d("Firestore", "Note ID: " + note.getId() + ", Date Changed: " + note.isDateChanged());

                            notes.add(note);
                        }
                    }
                    sortNotesByDate(notes); // Локальная сортировка (если потребуется)
                    adapter.setNotes(notes);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Ошибка загрузки заметок", Toast.LENGTH_SHORT).show();
                });
    }
    private void sortNotesByDate(List<NoteModel> notes) {
        Collections.sort(notes, (note1, note2) -> note2.getDate().compareTo(note1.getDate()));
    }

    public static class SpacesItemDecoration extends RecyclerView.ItemDecoration {
        private final int space;

        public SpacesItemDecoration(int spaceInDp) {
            this.space = (int) (spaceInDp * Resources.getSystem().getDisplayMetrics().density); // Преобразуем dp в пиксели
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view);
            int itemCount = state.getItemCount();

            outRect.left = space;
            outRect.right = space;
            outRect.top = space;

            // Нижний отступ добавляем только для элементов, кроме последнего
            if (position != itemCount - 1) {
                outRect.bottom = space;
            } else {
                outRect.bottom = 0;
            }
        }
    }

}

