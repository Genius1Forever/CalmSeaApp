package com.example.calmsea;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class EditActivity extends AppCompatActivity {
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private EditText editNameEditText, editPhoneEditText;
;
    private TextView editBirthdateTextView, editGenderTextView, editEmailTextView;
    private ImageView editAvatarImageView;
    private Button saveButton, cancelButton;
    private Uri avatarUri;
    private String userId; private ListenerRegistration userListenerRegistration;
    private SharedViewModel sharedViewModel;
    private ActivityResultLauncher<Intent> selectImageLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);

        // Инициализация Firebase и ViewModel
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        sharedViewModel = new ViewModelProvider(this).get(SharedViewModel.class);

        // Инициализация элементов интерфейса
        editNameEditText = findViewById(R.id.editNameEditText);
        editPhoneEditText = findViewById(R.id.editPhoneEditText);
        setupPhoneMask(editPhoneEditText);
        editBirthdateTextView = findViewById(R.id.editBirthdateTextView);
        editGenderTextView = findViewById(R.id.editGenderTextView);
        editAvatarImageView = findViewById(R.id.editAvatarImageView);
        editEmailTextView = findViewById(R.id.editEmailTextView);
        saveButton = findViewById(R.id.saveButton_Edit);
        cancelButton = findViewById(R.id.cancelButton_Edit);

        // Загрузка данных пользователя
        loadUserData();

        // Подписка на изменения данных пользователя в Firestore
        listenForUserChanges();

        // Установка обработчиков событий
        editBirthdateTextView.setOnClickListener(v -> showDatePicker());
        editGenderTextView.setOnClickListener(v -> showGenderPicker());
        editAvatarImageView.setOnClickListener(v -> openGallery());

        saveButton.setOnClickListener(v -> {
            String email = editEmailTextView.getText().toString().trim();
            String phone = editPhoneEditText.getText().toString().trim(); // Получаем строку с маской

            // Очищаем телефон от всех символов, кроме цифр
            String cleanPhone = phone.replaceAll("[^\\d]", "");

            if (!isValidEmail(email)) {
                editEmailTextView.setError("Введите корректный email");
            } else if (!cleanPhone.isEmpty() && !isValidPhoneNumber(phone)) {
                editPhoneEditText.setError("Введите корректный номер телефона");
            } else {
                saveUserData();
            }
        });

        cancelButton.setOnClickListener(v -> finish());

        // Инициализация ActivityResultLauncher
        selectImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        avatarUri = result.getData().getData();
                        editAvatarImageView.setImageURI(avatarUri); // Показать выбранное изображение

                        // Преобразуем Uri в File
                        File avatarFile = uriToFile(avatarUri);

                        // Загружаем аватарку в Cloudinary
                        uploadAvatarToCloudinary(avatarFile, userId);
                    }
                });

        loadAvatarImage(userId, editAvatarImageView);

    }

    // Метод для подписки на изменения данных пользователя
    private void listenForUserChanges() {
        if (userId == null) return;

        DocumentReference userRef = firestore.collection("users").document(userId);

        userListenerRegistration = userRef.addSnapshotListener((documentSnapshot, error) -> {
            if (error != null) {
                Log.e("Firestore", "Ошибка при получении данных", error);
                return;
            }

            if (documentSnapshot != null && documentSnapshot.exists()) {
                User user = documentSnapshot.toObject(User.class);
                if (user != null) {
                    editNameEditText.setText(user.getName());
                    editEmailTextView.setText(user.getEmail());
                    String birthDate = user.getBirthDate();
                    if (birthDate != null && !birthDate.trim().isEmpty()) {
                        editBirthdateTextView.setText(birthDate);
                    } else {
                        editBirthdateTextView.setText("Нажать, чтобы добавить");
                    }
                    String gender = user.getGender();
                    if (gender != null && !gender.trim().isEmpty()) {
                        editGenderTextView.setText(gender);
                    } else {
                        editGenderTextView.setText("Нажать, чтобы добавить");
                    }
                    if (user.getPhoneNumber() != null) {
                        editPhoneEditText.setText(user.getPhoneNumber());
                    }

                    if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
                        Glide.with(EditActivity.this)
                                .load(user.getProfileImageUrl())
                                .into(editAvatarImageView);
                    }
                }
            }
        });
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (userListenerRegistration != null) {
            userListenerRegistration.remove();
        }
    }
    private File uriToFile(Uri uri) {
        try {
            File file = File.createTempFile("temp_avatar", ".jpg", getCacheDir());
            try (InputStream inputStream = getContentResolver().openInputStream(uri);
                 OutputStream outputStream = new FileOutputStream(file)) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }
            }
            return file;
        } catch (IOException e) {
            Log.e("EDIT_USER", "Ошибка при создании файла", e);
            return null;
        }
    }
    public void loadAvatarImage(String userId, ImageView imageView) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Получаем ссылку на пользователя
        DocumentReference userDocRef = db.collection("users").document(userId);

        // Получаем URL аватарки
        userDocRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String avatarUrl = documentSnapshot.getString("avatar");

                // Загружаем аватарку с помощью Glide
                Glide.with(imageView.getContext())
                        .load(avatarUrl)
                        .placeholder(R.drawable.user) // Место для аватарки по умолчанию
                        .into(imageView);
            }
        }).addOnFailureListener(e -> Log.e("Firestore", "Ошибка при загрузке аватарки: " + e.getMessage()));
    }

    // Метод для проверки валидности телефона
    private boolean isValidPhoneNumber(String phone) {
        if (phone.isEmpty()) {
            return true; // Если телефон не введен, считаем его корректным
        }

        // Удаляем все символы, кроме цифр, чтобы проверить только цифры
        String cleanPhone = phone.replaceAll("[^\\d]", "");

        // Регулярное выражение для номера с кодом страны +7 или 8, за которым идут 10 цифр
        String regex = "^(?:7|8)?\\d{10}$";

        return cleanPhone.matches(regex); // Проверка на соответствие формату
    }


    // Метод для проверки валидности email
    private boolean isValidEmail(String email) {
        String regex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$";
        return email.matches(regex);
    }

    // Метод для загрузки данных пользователя
    private void loadUserData() {
        if (userId != null) {
            firestore.collection("users").document(userId)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        if (snapshot.exists()) {
                            editNameEditText.setText(snapshot.getString("name"));
                            editBirthdateTextView.setText(snapshot.getString("birthDate"));
                            editGenderTextView.setText(snapshot.getString("gender"));
                            editEmailTextView.setText(snapshot.getString("email"));

                            // Устанавливаем номер телефона только если он есть
                            String phone = snapshot.getString("phone");
                            if (phone != null) {
                                editPhoneEditText.setText(phone);
                            }
                            String avatarUrl = snapshot.getString("avatar");
                            if (avatarUrl != null) {
                                Glide.with(this)
                                        .load(avatarUrl)
                                        .circleCrop()
                                        .placeholder(R.drawable.user)
                                        .error(R.drawable.user)
                                        .into(editAvatarImageView);
                            }
                            else {
                                editAvatarImageView.setImageResource(R.drawable.user);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {Toast.makeText(this, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show();});
        }
    }

    // Метод для выбора даты
    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String formattedDate = String.format("%02d.%02d.%04d", selectedDay, selectedMonth + 1, selectedYear);
                    editBirthdateTextView.setText(formattedDate);
                },
                year, month, day
        );
        datePickerDialog.getDatePicker().setMaxDate(calendar.getTimeInMillis());
        datePickerDialog.show();
    }

    // Метод для выбора пола
    private void showGenderPicker() {
        String[] genderOptions = {"Мужской", "Женский"};

        new AlertDialog.Builder(this)
                .setTitle("Выберите пол")
                .setItems(genderOptions, (dialog, which) -> editGenderTextView.setText(genderOptions[which]))
                .show();
    }

    // Метод для открытия галереи
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        selectImageLauncher.launch(intent);  // Запуск ActivityResultLauncher
    }

    // Метод для загрузки аватара в Cloudinary
    public void uploadAvatarToCloudinary(File imageFile, String userId) {
        OkHttpClient client = new OkHttpClient();
        String publicId = "avatars/" + userId + "/avatar_" + System.currentTimeMillis();

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", imageFile.getName(),
                        RequestBody.create(imageFile, MediaType.parse("image/*")))
                .addFormDataPart("upload_preset", "CalmSea") // Укажите имя Upload Preset
                .addFormDataPart("public_id", publicId) // public_id
                .build();

        Request request = new Request.Builder()
                .url("https://api.cloudinary.com/v1_1/dcjqchjip/image/upload") // Ваш Cloud Name
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("Cloudinary", "Ошибка загрузки: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(EditActivity.this, "Ошибка загрузки изображения", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    JsonObject jsonObject = JsonParser.parseString(responseBody).getAsJsonObject();
                    String imageUrl = jsonObject.get("secure_url").getAsString();

                    // Сохраняем URL аватарки в Firestore
                    saveAvatarUrlToFirestore(userId, imageUrl);
                } else {
                    runOnUiThread(() -> Toast.makeText(EditActivity.this, "Ошибка загрузки изображения", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    // Метод для сохранения URL аватарки в Firestore
    private void saveAvatarUrlToFirestore(String userId, String imageUrl) {
        FirebaseFirestore.getInstance().collection("users").document(userId)
                .update("avatar", imageUrl)
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firestore", "Аватар обновлен в Firestore");

                    // Уведомляем ViewModel об изменении данных
                    sharedViewModel.setDataUpdated(true);

                    // Обновляем изображение в UI
                    runOnUiThread(() -> Glide.with(EditActivity.this)
                            .load(imageUrl)
                            .placeholder(R.drawable.user)
                            .error(R.drawable.user)
                            .into(editAvatarImageView));
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Ошибка обновления аватарки", e));
    }


    // Метод для сохранения данных пользователя
    private void saveUserData() {
        if (userId == null) {
            Toast.makeText(this, "Пользователь не найден", Toast.LENGTH_SHORT).show();
            return;
        }

        String name = editNameEditText.getText().toString();
        String phone = editPhoneEditText.getText().toString();
        String birthDate = editBirthdateTextView.getText().toString();
        String gender = editGenderTextView.getText().toString();
        String email = editEmailTextView.getText().toString();

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("birthDate", birthDate);
        updates.put("gender", gender);
        updates.put("email", email);
        // Сохраняем номер телефона, если он есть
        if (!phone.isEmpty()) {
            updates.put("phone", phone);
        }

        firestore.collection("users").document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d("SAVE_USER", "Данные успешно сохранены");
                    sharedViewModel.setDataUpdated(true); // Уведомляем ViewModel об обновлении данных
                    Toast.makeText(this, "Данные успешно обновлены", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Ошибка обновления данных", Toast.LENGTH_SHORT).show());
    }
    private void setupPhoneMask(EditText editText) {
        editText.addTextChangedListener(new TextWatcher() {
            private boolean isEditing = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isEditing || s.length() < 3) {
                    return;
                }

                isEditing = true;

                String clean = s.toString().replaceAll("[^\\d]", ""); // Удаляем всё, кроме цифр

                // Добавляем "+7" в начало, если номер начинается не с 7 или 8
                if (clean.startsWith("8")) {
                    clean = "7" + clean.substring(1);
                } else if (!clean.startsWith("7")) {
                    clean = "7" + clean;
                }

                // Ограничиваем длину номера
                if (clean.length() > 11) {
                    clean = clean.substring(0, 11);
                }

                // Форматируем в "+7 (XXX) XXX-XX-XX"
                String formatted = "+7";
                if (clean.length() > 1) formatted += " (" + clean.substring(1, Math.min(4, clean.length()));
                if (clean.length() > 4) formatted += ") " + clean.substring(4, Math.min(7, clean.length()));
                if (clean.length() > 7) formatted += "-" + clean.substring(7, Math.min(9, clean.length()));
                if (clean.length() > 9) formatted += "-" + clean.substring(9);

                editText.setText(formatted);
                editText.setSelection(formatted.length()); // Ставим курсор в конец

                isEditing = false;
            }
        });
    }

}
