package com.example.calmsea;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;


public class WelcomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        // Задержка перед переходом на главное окно
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent mainIntent = new Intent(WelcomeActivity.this, AuthActivity.class);
                startActivity(mainIntent);
                finish();
            }
        }, 2000); // (2 секунды)
    }
}