package com.example.calmsea;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class NoteEditActivity extends AppCompatActivity {

    private EditText editTextNote; private TextView moodTextView;
    private TextView dateTextView; private FirebaseFirestore db;

    private String userId; private String noteId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_edit);

        // Инициализация UI компонентов
        editTextNote = findViewById(R.id.et_editNote);
        moodTextView = findViewById(R.id.note_mood_editNote);
        dateTextView = findViewById(R.id.note_date_editNote);
        Button saveButton = findViewById(R.id.saveButton_editNote);
        Button cancelButton = findViewById(R.id.cancelButton_editNote);
        Button deleteButton = findViewById(R.id.deleteButton_editNote);

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Получение данных из Intent
        Intent intent = getIntent();
        noteId = intent.getStringExtra("noteId");
        String noteText = intent.getStringExtra("noteText");
        String moodText = intent.getStringExtra("noteMood");
        String dateText = intent.getStringExtra("noteDate");

        Log.d("NoteEditActivity", "Полученный noteId: " + noteId);

        if (noteId == null || noteId.isEmpty()) {
            Toast.makeText(this, "Ошибка: ID заметки не передан!", Toast.LENGTH_SHORT).show();
            finish();
        }


        // Установка значений в UI
        moodTextView.setText(moodText != null ? "Настроение: " + moodText : "Настроение: не указано");
        dateTextView.setText(dateText != null ? dateText : "");
        editTextNote.setText(noteText != null ? noteText : "");

        // Обработчик кнопки "Сохранить"
        saveButton.setOnClickListener(v -> {
            String updatedText = editTextNote.getText().toString().trim();
            if (!updatedText.isEmpty()) {
                saveNote(updatedText);
            } else {
                Toast.makeText(this, "Введите текст заметки", Toast.LENGTH_SHORT).show();
            }
        });

        // Обработчик кнопки "Отмена"
        cancelButton.setOnClickListener(v -> finish());

        // Обработчик кнопки "Удалить"
        deleteButton.setOnClickListener(v -> showDeleteConfirmationDialog(noteId));
    }

    //Метод сохранения заметки
    private void saveNote(String updatedText) {
        if (noteId != null) {
            Map<String, Object> updatedNote = new HashMap<>();
            updatedNote.put("noteText", updatedText); // Обновленный текст заметки

            db.collection("users")
                    .document(userId)
                    .collection("notes")
                    .document(noteId)
                    .update(updatedNote)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Заметка обновлена", Toast.LENGTH_SHORT).show();
                        finish(); // Закрываем активность
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Ошибка обновления заметки", Toast.LENGTH_SHORT).show();
                    });
        } else {
            Toast.makeText(this, "ID заметки не найден!", Toast.LENGTH_SHORT).show();
        }
    }

    //Подтверждение удаления заметки

    private void showDeleteConfirmationDialog(String noteId) {
        new AlertDialog.Builder(this)
                .setTitle("Удалить заметку")
                .setMessage("Вы точно хотите удалить заметку?")
                .setPositiveButton("Да", (dialog, which) -> deleteNoteFromFirestore(noteId))
                .setNegativeButton("Нет", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }

    //Удаление заметки из Firestore
    private void deleteNoteFromFirestore(String noteId) {
        if (noteId != null) {
            db.collection("users")
                    .document(userId)
                    .collection("notes")
                    .document(noteId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Заметка удалена", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Ошибка удаления", Toast.LENGTH_SHORT).show());
        }
    }
}
