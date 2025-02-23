package com.example.calmsea;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Инициализация BottomNavigationView
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Установить слушатель для навигации
        bottomNavigationView.setOnItemSelectedListener(navListener);

        // Загрузить HomeFragment при запуске
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
