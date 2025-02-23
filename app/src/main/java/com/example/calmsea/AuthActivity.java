package com.example.calmsea;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import com.google.firebase.auth.FirebaseAuth;

public class AuthActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private EditText emailEditText, passwordEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        // Инициализация FirebaseAuth
        auth = FirebaseAuth.getInstance();

        // Привязка полей и кнопок к разметке
        emailEditText = findViewById(R.id.edittext_auth_email);
        passwordEditText = findViewById(R.id.edittext_auth_password);
        Button loginButton = findViewById(R.id.button_auth);
        Button registerButton = findViewById(R.id.button_for_reg);

        // Обработка нажатия на кнопку входа
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailEditText.getText().toString().trim();
                String password = passwordEditText.getText().toString().trim();

                if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                    Toast.makeText(AuthActivity.this, "Заполните все поля", Toast.LENGTH_SHORT).show();
                    return;
                }

                loginUser(email, password);
            }
        });

        // Обработка нажатия на кнопку регистрации
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openRegistrationActivity();
            }
        });


        EditText emailEditText = findViewById(R.id.edittext_auth_email);
        EditText passwordEditText = findViewById(R.id.edittext_auth_password);

        setEditTextPlaceholderBehavior(emailEditText, "Введите ваш email");
        configurePasswordField(passwordEditText, "Введите пароль");

    }

    // Метод для входа пользователя
    private void loginUser(String email, String password) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Успешный вход
                        Toast.makeText(AuthActivity.this, "Вход выполнен", Toast.LENGTH_SHORT).show();
                        openMainActivity();
                    } else {
                        // Ошибка входа
                        Toast.makeText(AuthActivity.this, "Неверный email или пароль ", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Переход на экран регистрации
    private void openRegistrationActivity() {
        Intent intent = new Intent(AuthActivity.this, RegistrationActivity.class);
        startActivity(intent);
    }

    // Переход на главный экран (после успешного входа)
    private void openMainActivity() {
        Intent intent = new Intent(AuthActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
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
        // Устанавливаем Typeface для шрифта Lora
        Typeface loraTypeface = ResourcesCompat.getFont(passwordField.getContext(), R.font.lora);
        passwordField.setTypeface(loraTypeface);

        // Устанавливаем заглушку при запуске
        passwordField.setText(placeholder);
        passwordField.setInputType(InputType.TYPE_CLASS_TEXT); // Отображаем текст как обычный

        passwordField.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                // Очистить текст, если совпадает с заглушкой
                if (passwordField.getText().toString().equals(placeholder)) {
                    passwordField.setText("");
                }
            } else {
                // Вернуть заглушку, если пользователь ничего не ввел
                if (passwordField.getText().toString().isEmpty()) {
                    passwordField.setText(placeholder);
                    passwordField.setInputType(InputType.TYPE_CLASS_TEXT); // Отображать текст как обычный
                    passwordField.setTypeface(loraTypeface); // Возвращаем шрифт Lora для заглушки
                }
            }
        });

        // Добавляем слушатель для ввода текста
        passwordField.addTextChangedListener(new TextWatcher() {
            private boolean isPlaceholderCleared = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Если пользователь начинает ввод, убираем заглушку и включаем скрытие текста
                if (!isPlaceholderCleared && s.toString().equals(placeholder)) {
                    isPlaceholderCleared = true;
                    passwordField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    passwordField.setTypeface(loraTypeface); // Сохраняем шрифт Lora для вводимого текста
                    passwordField.setText(""); // Убираем заглушку
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Не требуется
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Не требуется
            }
        });
    }
}
