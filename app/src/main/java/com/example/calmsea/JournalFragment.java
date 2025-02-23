package com.example.calmsea;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JournalFragment extends Fragment {
    private FirebaseFirestore db;
    private String userId; private NotesAdapter adapter;

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
            intent.putExtra("noteId", note.getId()); // Передача ID заметки
            intent.putExtra("noteText", note.getNoteText());
            intent.putExtra("noteMood", note.getMood());
            intent.putExtra("noteDate", note.getDate());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        recyclerView.addItemDecoration(new SpacesItemDecoration(16)); // 16dp отступ

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Получаем заметки из Firestore и сортируем
        fetchNotesFromFirestore();

        LinearLayout addNoteButton = view.findViewById(R.id.addNoteButton);
        addNoteButton.setOnClickListener(v -> {
            startActivity(new Intent(getContext(), MoodQuestionActivity.class));
        });

        ImageButton button1 = view.findViewById(R.id.btn_change_date);
        button1.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), CalendarActivity.class);
            startActivity(intent);
        });

        return view;
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

