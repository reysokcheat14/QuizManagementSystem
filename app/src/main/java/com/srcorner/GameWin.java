package com.cscorner.appproject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.HashMap;
import java.util.Map;

public class GameWin extends AppCompatActivity {

    private static final String TAG = "GameWin";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_win);

        int score = getIntent().getIntExtra("score", 0);
        String language = getIntent().getStringExtra("LANGUAGE");
        String topic = getIntent().getStringExtra("TOPIC");

        if (language == null) {
            language = "kh"; // Default to Khmer
        }

        if (topic != null) {
            updateUserStatsInFirestore(score, topic);
        }

        TextView winScoreText = findViewById(R.id.win_score_text);
        Button btnPlayAgain = findViewById(R.id.btn_play_again);
        Button btnMainMenu = findViewById(R.id.btn_main_menu);

        if ("en".equals(language)) {
            winScoreText.setText("Final Score: " + score);
            btnPlayAgain.setText("Play Again");
            btnMainMenu.setText("Home");
        } else {
            winScoreText.setText("ពិន្ទុចុងក្រោយ: " + score);
            btnPlayAgain.setText("លេងម្ដងទៀត");
            btnMainMenu.setText("ំព័រដើម");
        }

        final String finalLanguage = language;
        btnPlayAgain.setOnClickListener(v -> {
            Intent intent = new Intent(GameWin.this, Question.class);
            intent.putExtra("TOPIC", topic);
            intent.putExtra("LANGUAGE", finalLanguage);
            startActivity(intent);
            finish();
        });

        btnMainMenu.setOnClickListener(v -> {
            Intent intent = new Intent(GameWin.this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void updateUserStatsInFirestore(int score, String topic) {
        SharedPreferences prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        String userId = prefs.getString("user_id", null);

        if (userId == null) {
            Log.e(TAG, "User ID is null, cannot update stats.");
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        final DocumentReference userRef = db.collection("users").document(userId);

        db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(userRef);

            if (!snapshot.exists()) {
                Log.e(TAG, "User document does not exist! Cannot update stats.");
                return null;
            }

            // Increment Total Score
            long currentTotalScore = snapshot.contains("totalScore") ? snapshot.getLong("totalScore") : 0;
            long newTotalScore = currentTotalScore + score;

            // Handle Scores By Topic
            Map<String, Object> scoresByTopic = snapshot.contains("scoresByTopic")
                    ? (Map<String, Object>) snapshot.get("scoresByTopic")
                    : new HashMap<>();

            long currentTopicScore = scoresByTopic.containsKey(topic) ? (long) scoresByTopic.get(topic) : 0;
            long newTopicScore = currentTopicScore + score;
            scoresByTopic.put(topic, newTopicScore);

            // Perform all updates
            transaction.update(userRef, "totalScore", newTotalScore);
            transaction.update(userRef, "scoresByTopic", scoresByTopic);
            transaction.update(userRef, "lastGameScore", score);
            transaction.update(userRef, "unlockedAchievements", FieldValue.arrayUnion("first_victory"));

            return null;
        }).addOnSuccessListener(aVoid -> Log.d(TAG, "User stats updated successfully in transaction."))
          .addOnFailureListener(e -> Log.e(TAG, "Error updating user stats in transaction", e));
    }
}
