package com.cscorner.appproject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class UserProfileActivity extends AppCompatActivity {

    private TextView usernameTextView;
    private TextView achievementsTextView;
    private TextView friendsTextView; // Changed to lastScoreTextView for clarity
    private Button openButton;
    private ImageView backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        usernameTextView = findViewById(R.id.username);
        achievementsTextView = findViewById(R.id.achievements_text);
        friendsTextView = findViewById(R.id.friends_text); // Re-purposing this view
        openButton = findViewById(R.id.open_button);
        backButton = findViewById(R.id.back_button);

        loadUserProfile();

        View.OnClickListener goHomeListener = v -> {
            Intent intent = new Intent(UserProfileActivity.this, MainActivity.class);
            startActivity(intent);
        };

        openButton.setOnClickListener(goHomeListener);
        backButton.setOnClickListener(goHomeListener);
    }

    private void loadUserProfile() {
        SharedPreferences prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        String userId = prefs.getString("user_id", null);
        String username = prefs.getString("username", "Player"); // Get saved username

        // Set the username immediately from local storage
        usernameTextView.setText(username);

        // Now, fetch the latest scores and achievements from the database
        if (userId != null) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users").document(userId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            // Update username
                            usernameTextView.setText(documentSnapshot.getString("username"));

                            // Update achievements count
                            List<String> unlockedAchievements = (List<String>) documentSnapshot.get("unlockedAchievements");
                            if (unlockedAchievements != null) {
                                int achievementCount = unlockedAchievements.size();
                                achievementsTextView.setText("Achievements\n" + achievementCount + " completed");
                            }

                            // Update last game score
                            Long lastGameScore = documentSnapshot.getLong("lastGameScore");
                            if (lastGameScore != null) {
                                friendsTextView.setText("Last Score:\n" + lastGameScore);
                            }
                        }
                    });
        }
    }
}
