package com.example.calmsea;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AddNoteActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_note);


        // Инициализация Firestore
        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Получаем переданное настроение и дату
        String mood = getIntent().getStringExtra("SELECTED_MOOD");
        String date = getIntent().getStringExtra("SELECTED_DATE");

        // Обновляем UI в зависимости от настроения
        TextView moodTextView = findViewById(R.id.note_mood);
        TextView dateTextView = findViewById(R.id.note_date);
        if (mood != null && !mood.isEmpty()) {
            moodTextView.setText("Настроение: " + mood);
        } else {
            moodTextView.setText("Настроение: не указано");
        }
        dateTextView.setText(date);

        EditText etNote = findViewById(R.id.note_text);
        Button btnSave = findViewById(R.id.save_note_button);

        btnSave.setOnClickListener(v -> {
            String noteText = etNote.getText().toString().trim();
            if (!noteText.isEmpty()) {
                saveNoteToFirestore(mood, noteText, date);
                setResult(Activity.RESULT_OK); // Уведомляем об успешном сохранении
                finish(); // Закрываем активность
            } else {
                Toast.makeText(this, "Введите текст заметки!", Toast.LENGTH_SHORT).show();
            }
        });
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    private void saveNoteToFirestore(String mood, String noteText, String date) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            showMessage("Ошибка: пользователь не авторизован");
            return;
        }

        String userId = currentUser.getUid();
        String color = getColorForMood(mood);

        NoteModel note = new NoteModel(null, mood, noteText, date, color);

        db.collection("users").document(userId).collection("notes")
                .add(note)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Заметка сохранена", Toast.LENGTH_SHORT).show();
                    updateNoteCount(userId);
                    FirestoreUtils.updateAverageMood(userId);

                    // ✅ Вызов анализа после успешного сохранения
                    getLastNotesAndGenerateRecommendation();
                })
                .addOnFailureListener(e -> showMessage("Ошибка сохранения: " + e.getMessage()));
    }

    private void updateNoteCount(String userId) {
        db.collection("users").document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            Long entriesCount = document.getLong("entriesCount");
                            if (entriesCount == null) {
                                entriesCount = 0L; // Если поле отсутствует, инициализируем с нуля
                            }
                            db.collection("users").document(userId)
                                    .update("entriesCount", entriesCount + 1);
                        }
                    } else {
                        Log.w("Firestore", "Ошибка получения документа пользователя", task.getException());
                    }
                });
    }

    private String getColorForMood(String mood) {
        switch (mood) {
            case "Отличное":
                return "#FFF9C4"; // Пример цвета
            case "Хорошее":
                return "#C8E6C9"; // Пример цвета
            case "Нормальное":
                return "#BBDEFB"; // Пример цвета
            case "Плохое":
                return "#FFE0B2"; // Пример цвета
            case "Ужасное":
                return "#FFCDD2"; // Пример цвета
            default:
                return "#FFFFFF"; // Цвет по умолчанию
        }
    }

    private void getLastNotesAndGenerateRecommendation() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            showMessage("Ошибка: пользователь не авторизован");
            return; // Прерываем выполнение метода
        }

        String userId = currentUser.getUid(); // Теперь userId точно не null
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users").document(userId).collection("notes")
                .orderBy("date", Query.Direction.DESCENDING)
                .limit(5)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<String> notesList = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        notesList.add(document.getString("noteText")); // Текст заметки
                    }
                    if (!notesList.isEmpty()) {
                        sendRequestToHuggingFace(notesList);
                    } else {
                        showMessage("Недостаточно данных для анализа");
                    }
                })
                .addOnFailureListener(e -> showMessage("Ошибка загрузки заметок: " + e.getMessage()));
    }
    private void sendRequestToHuggingFace(List<String> notes) {
        OkHttpClient client = new OkHttpClient();

        Log.d("AI Request", "Отправляем запрос в Hugging Face: " + notes.toString());

        String prompt = "Проанализируй следующие записи дневника и дай рекомендацию: " + String.join(" ", notes);
        String jsonRequest = "{ \"inputs\": \"" + prompt + "\" }";

        RequestBody body = RequestBody.create(jsonRequest, MediaType.get("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url("https://api-inference.huggingface.co/models/facebook/blenderbot-3B")
                .header("Authorization", "Bearer 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") // ⚠️ Убедись, что токен корректный!
                .post(body)
                .build();

        Log.d("AI Request", "JSON-запрос: " + jsonRequest);

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("AI Error", "Ошибка при обращении к нейросети: " + e.getMessage());
                showMessage("Ошибка при обращении к нейросети");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.d("AI Response", "Код ответа: " + response.code());

                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    Log.d("AI Response", "Ответ от нейросети: " + responseBody);

                    String recommendation = extractRecommendation(responseBody);
                    Log.d("AI Response", "Извлеченная рекомендация: " + recommendation);

                    // Сохраняем рекомендацию в Firestore
                    saveRecommendationToFirestore(recommendation);
                } else {
                    Log.e("AI Response", "Ошибка генерации рекомендации. Код: " + response.code());
                    showMessage("Ошибка генерации рекомендации");
                }
            }
        });
    }


    private String extractRecommendation(String responseBody) {
        try {
            JSONArray jsonArray = new JSONArray(responseBody);
            if (jsonArray.length() > 0) {
                String recommendation = jsonArray.getString(0);
                Log.d("AI Response", "Извлеченная рекомендация: " + recommendation);
                return recommendation;
            }
        } catch (JSONException e) {
            Log.e("AI Response", "Ошибка парсинга JSON", e);
        }
        return "Не удалось получить рекомендацию";
    }


    private void saveRecommendationToFirestore(String recommendation) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        if (recommendation == null || recommendation.isEmpty()) {
            Log.e("Firestore", "Попытка сохранить пустую рекомендацию!");
            return;
        }

        Log.d("Firestore", "Сохраняем рекомендацию: " + recommendation);

        Map<String, Object> recommendationData = new HashMap<>();
        recommendationData.put("text", recommendation);
        recommendationData.put("date", new Timestamp(new Date()));

        db.collection("users").document(userId).collection("recommendations")
                .add(recommendationData)
                .addOnSuccessListener(documentReference -> Log.d("Firestore", "Рекомендация сохранена"))
                .addOnFailureListener(e -> Log.e("Firestore", "Ошибка сохранения рекомендации", e));
    }


    private void showMessage(String message) {
        runOnUiThread(() ->
                Toast.makeText(AddNoteActivity.this, message, Toast.LENGTH_SHORT).show()
        );
    }

}
