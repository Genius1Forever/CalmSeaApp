package com.example.calmsea;

import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class FirestoreUtils {

    private static final String USERS_COLLECTION = "users";
    private static final String NOTES_COLLECTION = "notes";

    /**
     * Пересчитывает среднее настроение пользователя и обновляет его в базе данных.
     *
     * @param userId ID пользователя, для которого пересчитывается среднее настроение.
     */
    public static void updateAverageMood(String userId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection(USERS_COLLECTION)
                .document(userId)
                .collection(NOTES_COLLECTION)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Integer> moodValues = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String moodStr = document.getString("mood");
                        int moodValue = convertMoodToNumber(moodStr);
                        moodValues.add(moodValue);
                    }

                    if (!moodValues.isEmpty()) {
                        int sum = 0;
                        for (int mood : moodValues) {
                            sum += mood;
                        }
                        double average = (double) sum / moodValues.size();
                        String averageMood = convertNumberToMood(average);

                        // Обновляем среднее настроение в Firestore
                        db.collection(USERS_COLLECTION)
                                .document(userId)
                                .update("averageMood", averageMood)
                                .addOnSuccessListener(aVoid ->
                                        Log.d("FirestoreUtils", "Среднее настроение обновлено"))
                                .addOnFailureListener(e ->
                                        Log.e("FirestoreUtils", "Ошибка при обновлении среднего настроения", e));
                    } else {
                        // Если заметок нет, сбрасываем среднее настроение
                        db.collection(USERS_COLLECTION)
                                .document(userId)
                                .update("averageMood", "Не указано")
                                .addOnSuccessListener(aVoid ->
                                        Log.d("FirestoreUtils", "Среднее настроение сброшено"))
                                .addOnFailureListener(e ->
                                        Log.e("FirestoreUtils", "Ошибка при сбросе среднего настроения", e));
                    }
                })
                .addOnFailureListener(e ->
                        Log.e("FirestoreUtils", "Ошибка при загрузке заметок", e));
    }

    private static int convertMoodToNumber(String mood) {
        switch (mood) {
            case "Отличное":
                return 5;
            case "Хорошее":
                return 4;
            case "Нормальное":
                return 3;
            case "Плохое":
                return 2;
            case "Ужасное":
                return 1;
            default:
                return 0; // "Не указано" или любые некорректные значения
        }
    }
    private static String convertNumberToMood(double average) {
        if (average >= 4.5) {
            return "Отличное";
        } else if (average >= 3.5) {
            return "Хорошее";
        } else if (average >= 2.5) {
            return "Нормальное";
        } else if (average >= 1.5) {
            return "Плохое";
        } else {
            return "Ужасное";
        }
    }
}

