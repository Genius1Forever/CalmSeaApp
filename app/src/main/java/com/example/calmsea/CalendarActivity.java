package com.example.calmsea;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

public class CalendarActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);
        LinearLayout button = findViewById(R.id.calendarAddNoteButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openMoodQuestionActivity();
            }
        });
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }
    private void openMoodQuestionActivity() {
        Intent intent=new Intent(CalendarActivity.this,MoodQuestionActivity.class);
        startActivity(intent);
    }
}




