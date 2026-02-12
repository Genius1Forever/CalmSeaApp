package com.example.calmsea;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

public class FullRecommendationActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_recommendation);

        TextView fullTextView = findViewById(R.id.fullRecommendationText);
        String text = getIntent().getStringExtra("recommendation_text");
        fullTextView.setText(text);
        Button shareButton = findViewById(R.id.btn_share);

        shareButton.setOnClickListener(v -> {
            String recommendationText = fullTextView.getText().toString();

            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, recommendationText);
            sendIntent.setType("text/plain");

            Intent shareIntent = Intent.createChooser(sendIntent, "Поделиться рекомендацией через...");
            startActivity(shareIntent);
        });


        findViewById(R.id.btn_back_rec).setOnClickListener(v -> finish());
    }
}