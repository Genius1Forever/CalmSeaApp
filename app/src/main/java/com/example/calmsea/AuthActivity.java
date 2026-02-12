package com.example.calmsea;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
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

        // Инициализация FirebaseAuth
        auth = FirebaseAuth.getInstance();

        // ✅ Проверка, вошёл ли пользователь ранее
        if (auth.getCurrentUser() != null) {
            openMainActivity(); // если уже авторизован, переходим сразу
            return;
        }

        setContentView(R.layout.activity_auth);

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
        Typeface loraTypeface = ResourcesCompat.getFont(passwordField.getContext(), R.font.lora);

        passwordField.setText(placeholder);
        passwordField.setTypeface(loraTypeface);
        passwordField.setInputType(InputType.TYPE_CLASS_TEXT);
        passwordField.setTransformationMethod(null); // Видимый текст (для заглушки)

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
                    passwordField.setTransformationMethod(null); // Открытый текст
                }
            }
            passwordField.setTypeface(loraTypeface); // Восстановим шрифт всегда
        });
    }
}
