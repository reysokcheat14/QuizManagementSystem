package com.cscorner.appproject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class Language extends AppCompatActivity {

    private static final String TAG = "LanguageActivity";
    private static final String[] ADJECTIVES = {"Swift", "Silent", "Clever", "Brave", "Wise", "Fierce"};
    private static final String[] NOUNS = {"Lion", "Tiger", "Eagle", "Shark", "Wolf", "Dragon"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_language);

        checkFirstLaunch();

        Button btnKhmer = findViewById(R.id.btnKhmer);
        Button btnEnglish = findViewById(R.id.btnEnglish);

        btnKhmer.setOnClickListener(v -> {
            Intent intent = new Intent(Language.this, MainActivity.class);
            intent.putExtra("LANGUAGE", "kh"); // Pass Khmer language code
            startActivity(intent);
            finish();
        });

        btnEnglish.setOnClickListener(v -> {
            Intent intent = new Intent(Language.this, MainActivity.class);
            intent.putExtra("LANGUAGE", "en"); // Pass English language code
            startActivity(intent);
            finish();
        });
    }

    private void checkFirstLaunch() {
        SharedPreferences prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        String userId = prefs.getString("user_id", null);

        if (userId == null) {
            Log.d(TAG, "First launch detected, creating new user.");
            String newUserId = UUID.randomUUID().toString();
            String newUsername = generateRandomUsername();

            // Save the new user ID and username to SharedPreferences IMMEDIATELY and SYNCHRONOUSLY.
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("user_id", newUserId);
            editor.putString("username", newUsername);
            editor.commit(); // Use commit() to ensure the data is saved before continuing.
            Log.d(TAG, "New user ID saved locally: " + newUserId);

            // Now, create the user document in Firestore in the background.
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            Map<String, Object> user = new HashMap<>();
            user.put("username", newUsername);
            user.put("totalScore", 0);
            user.put("lastGameScore", 0);
            user.put("unlockedAchievements", new ArrayList<String>());
            user.put("seenQuestions", new HashMap<String, Object>()); // Add this line for the new feature

            db.collection("users").document(newUserId).set(user)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "New user document created successfully in Firestore.");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating user document in Firestore", e);
                });
        }
    }

    private String generateRandomUsername() {
        Random random = new Random();
        String adjective = ADJECTIVES[random.nextInt(ADJECTIVES.length)];
        String noun = NOUNS[random.nextInt(NOUNS.length)];
        int number = random.nextInt(9000) + 1000;
        return adjective + noun + number;
    }
}
