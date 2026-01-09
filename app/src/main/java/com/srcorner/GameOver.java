package com.cscorner.appproject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

public class GameOver extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_over);

        int score = getIntent().getIntExtra("score", 0);
        String language = getIntent().getStringExtra("LANGUAGE");
        String topic = getIntent().getStringExtra("TOPIC");

        if (language == null) {
            language = "kh"; // Default to Khmer
        }

        updateUserStatsInFirestore(score, topic);

        TextView gameOverTitle = findViewById(R.id.game_over_title);
        TextView finalScore = findViewById(R.id.finalScore);
        Button btnRetry = findViewById(R.id.btnRetry);
        Button btnHome = findViewById(R.id.btnHome);

        if ("en".equals(language)) {
            gameOverTitle.setText("Game Over");
            finalScore.setText("Final Score: " + score);
            btnRetry.setText("Play Again");
            btnHome.setText("Home");
        } else {
            gameOverTitle.setText("ចប់ហ្គេម");
            finalScore.setText("ពិន្ទុចុងក្រោយ: " + score);
            btnRetry.setText("លេងម្ដងទៀត");
            btnHome.setText("ទំព័រដើម");
        }

        final String finalLanguage = language;
        btnRetry.setOnClickListener(v -> {
            Intent intent = new Intent(GameOver.this, Question.class);
            intent.putExtra("TOPIC", topic);
            intent.putExtra("LANGUAGE", finalLanguage);
            startActivity(intent);
            finish();
        });

        btnHome.setOnClickListener(v -> {
            Intent intent = new Intent(GameOver.this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void updateUserStatsInFirestore(int score, String topic) {
        SharedPreferences prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        String userId = prefs.getString("user_id", null);

        if (userId != null && topic != null) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            db.collection("users").document(userId)
                    .update(
                            "totalScore", FieldValue.increment(score),
                            "scoresByTopic." + topic, FieldValue.increment(score),
                            "lastGameScore", score
                    )
                    .addOnSuccessListener(aVoid -> Log.d("GameOver", "User stats updated successfully"))
                    .addOnFailureListener(e -> Log.e("GameOver", "Error updating user stats", e));
        }
    }
}
