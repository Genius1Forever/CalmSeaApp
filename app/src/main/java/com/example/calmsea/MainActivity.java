package com.example.calmsea;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.firebase.ui.common.BuildConfig;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.lang.ref.WeakReference;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectAll() // отслеживает все потенциальные ошибки в UI-потоке
                    .penaltyLog() // выводит предупреждения в Logcat
                    .build());

            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectAll() // отслеживает ошибки работы с памятью
                    .penaltyLog()
                    .build());
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        WeakReference<Context> contextRef = new WeakReference<>(this);

        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            throwable.printStackTrace();

            new Handler(Looper.getMainLooper()).post(() -> {
                Context context = contextRef.get();
                if (context != null) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setTitle("Произошла ошибка")
                            .setMessage("Приложение перезапустится для корректной работы.")
                            .setCancelable(false)
                            .setPositiveButton("ОК", (dialog, which) -> {
                                Intent intent = new Intent(context.getApplicationContext(), MainActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                context.startActivity(intent);

                                android.os.Process.killProcess(android.os.Process.myPid());
                                System.exit(1);
                            });

                    builder.show();
                }
            });
        });

        // Инициализация BottomNavigationView
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Установить слушатель для навигации
        bottomNavigationView.setOnItemSelectedListener(navListener);

        // Загрузить HouseFragment при запуске
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new HouseFragment())
                .commit();

    }

    // Слушатель для BottomNavigationView
    private final BottomNavigationView.OnItemSelectedListener navListener =
            new BottomNavigationView.OnItemSelectedListener() {
                @Override

                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    Fragment selectedFragment = null;

                    if (item.getItemId() == R.id.navigation_house) {
                        selectedFragment = new HouseFragment();
                    } else if (item.getItemId() == R.id.navigation_journal) {
                        selectedFragment = new JournalFragment();
                    }
                    else if (item.getItemId() == R.id.navigation_growth) {
                        selectedFragment = new GrowthFragment();
                    }
                    else if (item.getItemId() == R.id.navigation_profile) {
                        selectedFragment = new ProfileFragment();
                    }

                    // Заменить текущий фрагмент
                    if (selectedFragment != null) {
                        getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.fragment_container, selectedFragment)
                                .commit();
                    }

                    return true;
                }
            };
}
