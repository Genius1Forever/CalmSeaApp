package com.example.calmsea;

import android.app.Activity;
import android.content.Intent;
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
import org.json.JSONObject;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AddNoteActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String userId; private Call openRouterCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_note);

        db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            showMessage("Ошибка: пользователь не авторизован");
            finish();
            return;
        }
        userId = currentUser.getUid();
        // Получаем переданные данные
        String mood = getIntent().getStringExtra("SELECTED_MOOD");
        String dateString = getIntent().getStringExtra("SELECTED_DATE");
        final boolean isDateChanged = getIntent().getBooleanExtra("DATE_CHANGED", false);

        Log.d("AddNoteActivity", "Received data. isDateChanged: " + isDateChanged);

        // Обновляем UI
        TextView moodTextView = findViewById(R.id.note_mood);
        TextView dateTextView = findViewById(R.id.note_date);
        EditText etNote = findViewById(R.id.note_text);
        Button btnSave = findViewById(R.id.save_note_button);

        if (mood != null && !mood.isEmpty()) {
            moodTextView.setText("Настроение: " + mood);
        } else {
            moodTextView.setText("Настроение: не указано");
        }

        if (dateString != null && !dateString.isEmpty()) {
            Log.d("DateCheck", "До форматирования: " + dateString);
            dateTextView.setText(formatDateWithoutTime(dateString, isDateChanged));
        } else {
            dateTextView.setText("Дата не указана");
        }

        btnSave.setOnClickListener(v -> {
            String noteText = etNote.getText().toString().trim();
            if (!noteText.isEmpty()) {
                saveNoteToFirestore(mood, noteText, dateString, isDateChanged);
            } else {
                showMessage("Введите текст заметки!");
            }
        });

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    /**
     * Сохранение заметки в Firestore.
     */
    private void saveNoteToFirestore(String mood, String noteText, String dateString, boolean isDateChanged) {
        try {
            // Определяем формат даты
            SimpleDateFormat fullFormat = new SimpleDateFormat("EEEE, dd MMMM yyyy, HH:mm", Locale.getDefault());
            SimpleDateFormat shortFormat = new SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault());

            Date date;
            try {
                date = fullFormat.parse(dateString);
            } catch (ParseException e) {
                Log.w("DateParsing", "Не удалось разобрать дату с временем, пробуем без времени: " + dateString);
                date = shortFormat.parse(dateString);
            }

            if (date == null) {
                throw new ParseException("Дата не распознана: " + dateString, 0);
            }

            Timestamp timestamp = new Timestamp(date);
            String color = getColorForMood(mood);

            NoteModel note = new NoteModel(null, mood, noteText, timestamp, color);
            note.setDateChanged(isDateChanged); // Устанавливаем флаг изменения даты

            Log.d("FirestoreSave", "Saving note with dateChanged: " + isDateChanged);

            db.collection("users").document(userId).collection("notes")
                    .add(note)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(this, "Заметка сохранена", Toast.LENGTH_SHORT).show();
                        updateNoteCount();
                        FirestoreUtils.updateAverageMood(userId);

                        // Устанавливаем флаг генерации рекомендации
                        db.collection("users").document(userId)
                                .update("recommendationStatus", "generating")
                                .addOnSuccessListener(aVoid -> {
                                    // Генерация рекомендации после установки флага
                                    getLastNotesAndGenerateRecommendation();
                                });

                        // Передаём сигнал в HouseFragment о необходимости обновления списка
                        sendUpdateToHouseFragment();

                        // После успешного добавления заметки
                        setResult(RESULT_OK);
                        finish();
                    })
                    .addOnFailureListener(e -> showMessage("Ошибка сохранения: " + e.getMessage()));

        } catch (ParseException e) {
            Log.e("DateError", "Ошибка преобразования даты: " + dateString, e);
            showMessage("Ошибка обработки даты");
        }
    }

    /**
     * Отправка сигнала в HouseFragment об обновлении списка.
     */
    private void sendUpdateToHouseFragment() {
        Intent intent = new Intent();
        intent.putExtra("note_added", true);
        setResult(Activity.RESULT_OK, intent);
        finish(); // Закрываем активность и возвращаемся в HouseFragment
    }

    /**
     * Форматирование даты: убираем время, если пользователь изменил её.
     */
    private String formatDateWithoutTime(String date, boolean isDateChanged) {
        if (!isDateChanged) return date; // Если дата не менялась, возвращаем её как есть

        SimpleDateFormat fullFormat = new SimpleDateFormat("EEEE, dd MMMM yyyy, HH:mm", Locale.getDefault());
        SimpleDateFormat shortFormat = new SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault());

        try {
            Date parsedDate = fullFormat.parse(date);
            return shortFormat.format(parsedDate); // Убираем время
        } catch (ParseException e) {
            return date; // Если не удалось обработать, возвращаем как есть
        }
    }

    private void updateNoteCount() {
        // Используем уже сохранённый userId
        db.collection("users").document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            Long entriesCount = document.getLong("entriesCount");
                            if (entriesCount == null) {
                                entriesCount = 0L;
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
        if (mood == null) return "#FFFFFF";
        switch (mood) {
            case "Отличное":
                return "#FFF9C4";
            case "Хорошее":
                return "#C8E6C9";
            case "Нормальное":
                return "#BBDEFB";
            case "Плохое":
                return "#FFE0B2";
            case "Ужасное":
                return "#FFCDD2";
            default:
                return "#FFFFFF";
        }
    }

    private void getLastNotesAndGenerateRecommendation() {
        // Запрос последних 3 заметки (можно настроить количество, если нужно учитывать меньше, например, 3)
        db.collection("users").document(userId).collection("notes")
                .orderBy("date", Query.Direction.DESCENDING)
                .limit(3)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<String> notesList = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String text = document.getString("noteText");
                        if (text != null && !text.isEmpty()) {
                            notesList.add(text);
                        }
                    }
                    if (!notesList.isEmpty()) {
                        sendRequestToOpenRouter(notesList);
                    } else {
                        showMessage("Недостаточно данных для анализа");
                    }
                })
                .addOnFailureListener(e -> showMessage("Ошибка загрузки заметок: " + e.getMessage()));
    }

    private void sendRequestToOpenRouter(List<String> notes) {
        OkHttpClient client = new OkHttpClient();

        String prompt = "Ты — профессиональный психолог. Проанализируй следующие записи дневника и в максимум 3 законченных коротких предложениях предложи конкретные и реалистичные рекомендации для улучшения эмоционального состояния (обязательно на русском языке, с учетом эмоционального контекста пользователя, пиши нумерацию предложений, не пиши лишние цифры в рекомендацию):\n\n" + String.join("\n", notes);

        JSONObject json = new JSONObject();
        try {
            JSONArray messages = new JSONArray();
            messages.put(new JSONObject().put("role", "system").put("content", "Ты — эмпатичный психолог."));
            messages.put(new JSONObject().put("role", "user").put("content", prompt));

            json.put("model", "deepseek/deepseek-chat-v3-0324:free");
            json.put("messages", messages);
            json.put("temperature", 0.7);
            json.put("max_tokens", 0);

        } catch (JSONException e) {
            Log.e("OpenRouter", "Ошибка формирования JSON", e);
            return;
        }

        RequestBody body = RequestBody.create(json.toString(), MediaType.get("application/json"));

        Request request = new Request.Builder()
                .url("https://openrouter.ai/api/v1/chat/completions")
                .header("Authorization", "Bearer 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")
                .header("Content-Type", "application/json")
                .header("HTTP-Referer", "https://calmsea.app") // можно заменить на любое уникальное значение
                .header("X-Title", "CalmSea Android")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("OpenRouter", "Ошибка запроса: " + e.getMessage());
                showMessage("Ошибка при обращении к нейросети");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    Log.d("OpenRouter", "Ответ: " + responseBody);

                    try {
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        String recommendation = jsonResponse
                                .getJSONArray("choices")
                                .getJSONObject(0)
                                .getJSONObject("message")
                                .getString("content")
                                .trim();
                        saveRecommendationToFirestore(recommendation);
                    } catch (JSONException e) {
                        Log.e("OpenRouter", "Ошибка парсинга", e);
                        showMessage("Ошибка при обработке ответа");
                    }

                } else {
                    Log.e("OpenRouter", "Код ошибки: " + response.code());
                    showMessage("Ошибка от нейросети");
                }
            }
        });
    }

    private void saveRecommendationToFirestore(String recommendation) {
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
                .addOnSuccessListener(documentReference -> {
                    Log.d("Firestore", "Рекомендация сохранена");

                    // Обновляем статус после успешного сохранения
                    db.collection("users").document(userId)
                            .update("recommendationStatus", "ready")
                            .addOnSuccessListener(aVoid ->
                                    Log.d("Firestore", "recommendationStatus обновлён на 'ready'"))
                            .addOnFailureListener(e ->
                                    Log.e("Firestore", "Ошибка обновления recommendationStatus", e));
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Ошибка сохранения рекомендации", e));
    }

    private void showMessage(String message) {
        runOnUiThread(() ->
                Toast.makeText(AddNoteActivity.this, message, Toast.LENGTH_SHORT).show()
        );
    }

}

