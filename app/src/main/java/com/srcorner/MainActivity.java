package com.cscorner.appproject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String PREFS_NAME = "user_prefs";
    private static final String KEY_LANGUAGE = "language";

    CardView mathTopic, logicTopic, historyTopic, countryTopic;
    TextView chooseTopicTitle, mathTopicTitle, logicTopicTitle, historyTopicTitle, countryTopicTitle;
    ImageButton languageButton, userProfileButton;

    private String currentLanguage = "kh"; // Default to Khmer

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Handle Language Selection
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String languageFromIntent = getIntent().getStringExtra("LANGUAGE");
        if (languageFromIntent != null) {
            // Language passed from LanguageActivity, save it
            currentLanguage = languageFromIntent;
            saveLanguagePreference(currentLanguage);
        } else {
            // No language from intent, load from preferences (for subsequent app opens)
            currentLanguage = prefs.getString(KEY_LANGUAGE, "kh");
        }

        // Ensure a user ID is created or retrieved
        String userId = prefs.getString("user_id", null);
        if (userId == null) {
            userId = UUID.randomUUID().toString();
            prefs.edit().putString("user_id", userId).apply();
            Log.d(TAG, "New User ID created: " + userId);
        }

        mathTopic = findViewById(R.id.math_topic);
        logicTopic = findViewById(R.id.logic_topic);
        historyTopic = findViewById(R.id.history_topic);
        countryTopic = findViewById(R.id.country_topic);

        chooseTopicTitle = findViewById(R.id.choose_topic_title);
        mathTopicTitle = findViewById(R.id.math_topic_title);
        logicTopicTitle = findViewById(R.id.logic_topic_title);
        historyTopicTitle = findViewById(R.id.history_topic_title);
        countryTopicTitle = findViewById(R.id.country_topic_title);

        languageButton = findViewById(R.id.language_button);
        userProfileButton = findViewById(R.id.user_profile_button);

        mathTopic.setOnClickListener(v -> startQuiz("Math"));
        logicTopic.setOnClickListener(v -> startQuiz("Logic"));
        historyTopic.setOnClickListener(v -> startQuiz("History"));
        countryTopic.setOnClickListener(v -> startQuiz("Country"));

        languageButton.setOnClickListener(v -> showLanguageDialog());
        userProfileButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, UserProfileActivity.class);
            startActivity(intent);
        });

        updateUIText();
        seedDatabase();
    }

    private void startQuiz(String topic) {
        Intent intent = new Intent(MainActivity.this, Question.class);
        intent.putExtra("TOPIC", topic);
        intent.putExtra("LANGUAGE", currentLanguage);
        startActivity(intent);
    }

    private void showLanguageDialog() {
        final String[] languages = {"English", "ខ្មែរ"};
        int checkedItem = "en".equals(currentLanguage) ? 0 : 1;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Language");
        builder.setSingleChoiceItems(languages, checkedItem, (dialog, which) -> {
            currentLanguage = (which == 0) ? "en" : "kh";
            saveLanguagePreference(currentLanguage);
            updateUIText();
            dialog.dismiss();
        });
        builder.create().show();
    }

    private void saveLanguagePreference(String language) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_LANGUAGE, language).apply();
    }

    private void updateUIText() {
        if ("en".equals(currentLanguage)) {
            chooseTopicTitle.setText("Choose a Topic");
            mathTopicTitle.setText("Math");
            logicTopicTitle.setText("Logic");
            historyTopicTitle.setText("History");
            countryTopicTitle.setText("Country");
        } else {
            chooseTopicTitle.setText("ជ្រើសរើសប្រធានបទ");
            mathTopicTitle.setText("គណិតវិទ្យា");
            logicTopicTitle.setText("តក្កវិទ្យា");
            historyTopicTitle.setText("ប្រវត្តិសាស្ត្រ");
            countryTopicTitle.setText("ប្រទេស");
        }
    }

    private void seedDatabase() {
        SharedPreferences prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        boolean isSeeded = prefs.getBoolean("is_seeded_v8", false); // Incremented version flag

        if (!isSeeded) {
            Log.d(TAG, "Seeding database with large question set...");
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            seedQuestions(db);

            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("is_seeded_v8", true);
            editor.apply();
            Log.d(TAG, "Database seeding v8 complete.");
        }
    }

    private void seedQuestions(FirebaseFirestore db) {
        // --- MATH (20 Questions) --- //
        List<Map<String, Object>> mathQuestionsEn = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            int a = (int) (Math.random() * 20) + 1;
            int b = (int) (Math.random() * 20) + 1;
            mathQuestionsEn.add(createQuestion("math_en_" + i, "What is " + a + " + " + b + "?", Arrays.asList(String.valueOf(a + b), String.valueOf(a + b + 1), String.valueOf(a + b - 1)), 0));
        }
        db.collection("questions").document("math").collection("questions_by_lang").document("en").set(new HashMap<String, Object>() {{
            put("questions", mathQuestionsEn);
        }});

        List<Map<String, Object>> mathQuestionsKm = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            int a = (int) (Math.random() * 10) + 1;
            int b = (int) (Math.random() * 10) + 1;
            mathQuestionsKm.add(createQuestion("math_km_" + i, "តើ " + a + " + " + b + " មានតម្លៃប៉ុន្មាន?", Arrays.asList(String.valueOf(a + b), String.valueOf(a + b + 2), String.valueOf(a + b - 2)), 0));
        }
        db.collection("questions").document("math").collection("questions_by_lang").document("kh").set(new HashMap<String, Object>() {{
            put("questions", mathQuestionsKm);
        }});

        // --- LOGIC (20 Questions) --- //
        List<Map<String, Object>> logicQuestionsEn = new ArrayList<>();
        List<Map<String, Object>> logicQuestionsKm = new ArrayList<>();
        String[][] logicData = {
                {"Which number should come next in the pattern? 1, 1, 2, 3, 5, ...", "8", "7", "6", "0", "តើលេខអ្វីគួរមកបន្ទាប់ក្នុងលំនាំ? ១, ១, ២, ៣, ៥, ...", "៨", "៧", "៦", "0"},
                {"If a plane crashes on the border between the USA and Canada, where do they bury the survivors?", "USA", "Canada", "Nowhere", "2", "បើយន្តហោះធ្លាក់នៅព្រំដែនអាមេរិក និងកាណាដា តើគេកប់អ្នកនៅរស់នៅឯណា?", "អាមេរិក", "កាណាដា", "គ្មានកន្លែងណា", "2"},
                {"What has to be broken before you can use it?", "A promise", "A secret", "An egg", "2", "តើអ្វីដែលត្រូវតែខូចមុននឹងអាចប្រើបាន?", "ការសន្យា", "អាថ៌កំបាំង", "ពងមាន់", "2"},
                {"What is full of holes but still holds water?", "A net", "A sponge", "A strainer", "1", "អ្វីដែលពោរពេញដោយរន្ធតែនៅតែមានទឹក?", "សំណាញ់", "អេប៉ុង", "ឧបករណ៍​ច្រោះ", "1"},
                {"What has one eye, but can’t see?", "A needle", "A potato", "A storm", "0", "អ្វីដែលមានភ្នែកតែមួយ ប៉ុន្តែមើលមិនឃើញ?", "ម្ជុល", "ដំឡូង", "ព្យុះ", "0"}
        };
        for (int i = 0; i < 20; i++) {
            String[] data = logicData[i % logicData.length];
            logicQuestionsEn.add(createQuestion("logic_en_" + i, data[0], Arrays.asList(data[1], data[2], data[3]), Integer.parseInt(data[4])));
            logicQuestionsKm.add(createQuestion("logic_kh_" + i, data[5], Arrays.asList(data[6], data[7], data[8]), Integer.parseInt(data[9])));
        }
        db.collection("questions").document("logic").collection("questions_by_lang").document("en").set(new HashMap<String, Object>() {{
            put("questions", logicQuestionsEn);
        }});
        db.collection("questions").document("logic").collection("questions_by_lang").document("kh").set(new HashMap<String, Object>() {{
            put("questions", logicQuestionsKm);
        }});


        // --- HISTORY (20 Questions) --- //
        List<Map<String, Object>> historyQuestionsEn = new ArrayList<>();
        List<Map<String, Object>> historyQuestionsKm = new ArrayList<>();
        String[][] historyData = {
                {"Who was the first President of the United States?", "George Washington", "Abraham Lincoln", "Thomas Jefferson", "0", "តើនរណាជាប្រធានាធិបតីទីមួយនៃសហរដ្ឋអាមេរិក?", "ចច វ៉ាស៊ីនតោន", "អាប្រាហាំ លីនខុន", "ថូម៉ាស ចេហ្វឺសុន", "0"},
                {"In which year did World War II end?", "1945", "1918", "1939", "0", "តើសង្គ្រាមលោកលើកទី២បានបញ្ចប់នៅឆ្នាំណា?", "១៩៤៥", "១៩១៨", "១៩៣៩", "0"},
                {"Who invented the telephone?", "Alexander Graham Bell", "Thomas Edison", "Nikola Tesla", "0", "តើនរណាជាអ្នកបង្កើតទូរស័ព្ទ?", "អាឡិចសាន់ឌឺ ក្រាហាំ ប៊ែល", "ថូម៉ាស អេឌីសុន", "នីកូឡា តេសឡា", "0"},
                {"Who was the last pharaoh of Ancient Egypt?", "Cleopatra VII", "Ramesses II", "Tutankhamun", "0", "តើនរណាជាស្តេចផារ៉ោនចុងក្រោយនៃប្រទេសអេហ្ស៊ីបបុរាណ?", "ក្លូប៉ាត្រាទី ៧", "រ៉ាមសេសទី ២", "ទូតានខាមុន", "0"},
                {"The Renaissance began in which country?", "Italy", "France", "Greece", "0", "សម័យ Renaissance បានចាប់ផ្តើមនៅប្រទេសណា?", "អ៊ីតាលី", "បារាំង", "ក្រិក", "0"}
        };
        for (int i = 0; i < 20; i++) {
            String[] data = historyData[i % historyData.length];
            historyQuestionsEn.add(createQuestion("history_en_" + i, data[0], Arrays.asList(data[1], data[2], data[3]), Integer.parseInt(data[4])));
            historyQuestionsKm.add(createQuestion("history_kh_" + i, data[5], Arrays.asList(data[6], data[7], data[8]), Integer.parseInt(data[9])));
        }

        db.collection("questions").document("history").collection("questions_by_lang").document("en").set(new HashMap<String, Object>() {{
            put("questions", historyQuestionsEn);
        }});
        db.collection("questions").document("history").collection("questions_by_lang").document("kh").set(new HashMap<String, Object>() {{
            put("questions", historyQuestionsKm);
        }});


        // --- COUNTRY (20 Questions) --- //
        List<Map<String, Object>> countryQuestionsEn = new ArrayList<>();
        List<Map<String, Object>> countryQuestionsKm = new ArrayList<>();
        String[][] countryData = {
                {"What is the capital of Japan?", "Tokyo", "Beijing", "Seoul", "0", "តើអ្វីជារដ្ឋធានីនៃប្រទេសជប៉ុន?", "តូក្យូ", "ប៉េកាំង", "សេអ៊ូល", "0"},
                {"Which country is known as the Land of the Pharaohs?", "Egypt", "Greece", "Italy", "0", "តើប្រទេសណាដែលគេស្គាល់ថាជាទឹកដីនៃស្តេចផារ៉ោន?", "អេហ្ស៊ីប", "ក្រិក", "អ៊ីតាលី", "0"},
                {"What is the smallest country in the world?", "Vatican City", "Monaco", "San Marino", "0", "តើប្រទេសណាដែលតូចជាងគេក្នុងលោក?", "ទីក្រុង​វ៉ាទីកង់", "ម៉ូណាកូ", "សាន ម៉ារីណូ", "0"},
                {"Which country is also an entire continent?", "Australia", "Greenland", "Antarctica", "0", "តើប្រទេសណាក៏ជាទ្វីបទាំងមូល?", "អូស្ត្រាលី", "ហ្គ្រីនលែន", "អង់តាក់ទិក", "0"},
                {"What country is home to the kangaroo?", "Australia", "South Africa", "New Zealand", "0", "តើប្រទេសណាជាផ្ទះរបស់សត្វកង់ហ្គូរូ?", "អូស្ត្រាលី", "អាហ្វ្រិកខាងត្បូង", "នូវែលសេឡង់", "0"}
        };
        for (int i = 0; i < 20; i++) {
            String[] data = countryData[i % countryData.length];
            countryQuestionsEn.add(createQuestion("country_en_" + i, data[0], Arrays.asList(data[1], data[2], data[3]), Integer.parseInt(data[4])));
            countryQuestionsKm.add(createQuestion("country_kh_" + i, data[5], Arrays.asList(data[6], data[7], data[8]), Integer.parseInt(data[9])));
        }

        db.collection("questions").document("country").collection("questions_by_lang").document("en").set(new HashMap<String, Object>() {{
            put("questions", countryQuestionsEn);
        }});
        db.collection("questions").document("country").collection("questions_by_lang").document("kh").set(new HashMap<String, Object>() {{
            put("questions", countryQuestionsKm);
        }});
    }

    private Map<String, Object> createQuestion(String id, String questionText, List<String> answers, int correctOptionIndex) {
        // This method now correctly shuffles the answers and finds the new correct index.
        List<String> mutableAnswers = new ArrayList<>(answers);
        String correctAnswer = mutableAnswers.get(correctOptionIndex);
        Collections.shuffle(mutableAnswers);
        int newCorrectIndex = mutableAnswers.indexOf(correctAnswer);

        Map<String, Object> question = new HashMap<>();
        question.put("id", id);
        question.put("question", questionText);
        question.put("options", mutableAnswers);
        question.put("correctOptionIndex", (long) newCorrectIndex);
        return question;
    }
}
