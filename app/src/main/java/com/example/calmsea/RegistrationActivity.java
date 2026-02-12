package com.example.calmsea;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;


public class RegistrationActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        // Инициализация Firebase
        FirebaseApp.initializeApp(this);
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        EditText nameEditText = findViewById(R.id.name_reg);
        EditText emailEditText = findViewById(R.id.edittext_reg_email);
        EditText passwordEditText = findViewById(R.id.edittext_reg_password);
        EditText password2EditText = findViewById(R.id.reg2password);
        Button registerButton = findViewById(R.id.button_reg);

        registerButton.setOnClickListener(v -> {
            String userName = nameEditText.getText().toString().trim();
            String userEmail = emailEditText.getText().toString().trim();
            String userPassword = passwordEditText.getText().toString().trim();
            String confirmPassword = password2EditText.getText().toString().trim();

            // Проверка на пустоту полей
            if (userName.isEmpty()) {
                nameEditText.setError("Введите ваше имя");
                return;
            }
            if (userEmail.isEmpty()) {
                emailEditText.setError("Введите вашу электронную почту");
                return;
            }
            if (userPassword.isEmpty() || userPassword.length() < 6) {
                passwordEditText.setError("Пароль должен быть не менее 6 символов");
                return;
            }
            if (!userPassword.equals(confirmPassword)) {
                password2EditText.setError("Пароли не совпадают");
                return;
            }
            // Дополнительные проверки на пароль
            if (!containsDigit(userPassword)) {
                passwordEditText.setError("Пароль должен содержать хотя бы одну цифру");
                return;
            }
            if (!containsSpecialCharacter(userPassword)) {
                passwordEditText.setError("Пароль должен содержать хотя бы один специальный символ");
                return;
            }
            if (!containsUpperCase(userPassword)) {
                passwordEditText.setError("Пароль должен содержать хотя бы одну заглавную букву");
                return;
            }
            if (!containsLowerCase(userPassword)) {
                passwordEditText.setError("Пароль должен содержать хотя бы одну строчную букву");
                return;
            }
            // Регистрация через Firebase Authentication
            auth.createUserWithEmailAndPassword(userEmail, userPassword)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            String userId = auth.getCurrentUser().getUid();
                            initializeUserDocument(userId, userName, userEmail);
                        } else {
                            Exception exception = task.getException();
                            if (exception instanceof FirebaseAuthUserCollisionException) {
                                emailEditText.setError("Этот email уже зарегистрирован");
                            } else if (exception instanceof FirebaseAuthInvalidCredentialsException) {
                                emailEditText.setError("Неверный формат email");
                            } else {
                                Toast.makeText(this, "Ошибка: " + exception.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        });

        // Настройка слушателей для EditText
        setEditTextPlaceholderBehavior(nameEditText, "Введите ваше имя");
        setEditTextPlaceholderBehavior(emailEditText, "Введите ваш email");


        // Настраиваем поведение для полей ввода пароля
        configurePasswordField(passwordEditText, "Введите пароль");
        configurePasswordField(password2EditText, "Подтвердите пароль");
    }

    private void initializeUserDocument(String userId, String userName, String userEmail) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("name", userName);
        userData.put("email", userEmail);
        userData.put("entriesCount", 0); // Инициализация поля noteCount

        db.collection("users").document(userId)
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                Toast.makeText
                   (this, "Регистрация успешна!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent
                            (RegistrationActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText
                    (this,
                    "Ошибка сохранения данных: " + e.getLocalizedMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }
    // Метод для настройки поведения EditText с заглушками
    private void setEditTextPlaceholderBehavior(EditText editText, String placeholder) {
        editText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                // Очистить текст, если совпадает с заглушкой
                if (editText.getText().toString().equals(placeholder)) {
                    editText.setText("");
                }
            } else {
                // Вернуть заглушку, если пользователь ничего не ввел
                if (editText.getText().toString().isEmpty()) {
                    editText.setText(placeholder);
                }
            }
        });
    }

    private void configurePasswordField(EditText passwordField, String placeholder) {
        Typeface loraTypeface = ResourcesCompat.getFont(passwordField.getContext(), R.font.lora);

        passwordField.setText(placeholder);
        passwordField.setTypeface(loraTypeface);
        passwordField.setInputType(InputType.TYPE_CLASS_TEXT);
        passwordField.setTransformationMethod(null); // Текст видим

        passwordField.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                if (passwordField.getText().toString().equals(placeholder)) {
                    passwordField.setText("");
                    passwordField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    passwordField.setTransformationMethod(PasswordTransformationMethod.getInstance()); // Скрываем
                }
            } else {
                if (passwordField.getText().toString().isEmpty()) {
                    passwordField.setText(placeholder);
                    passwordField.setInputType(InputType.TYPE_CLASS_TEXT);
                    passwordField.setTransformationMethod(null); // Видимый текст
                }
            }
            // Обязательно восстанавливаем шрифт
            passwordField.setTypeface(loraTypeface);
        });
    }

    // Проверка на наличие цифр
    private boolean containsDigit(String password) {
        return password.matches(".*\\d.*");
    }

    // Проверка на наличие специальных символов
    private boolean containsSpecialCharacter(String password) {
        return password.matches(".*[!@#$%^&*].*");
    }

    // Проверка на наличие заглавных букв
    private boolean containsUpperCase(String password) {
        return password.matches(".*[A-Z].*");
    }

    // Проверка на наличие строчных букв
    private boolean containsLowerCase(String password) {
        return password.matches(".*[a-z].*");
    }
}
