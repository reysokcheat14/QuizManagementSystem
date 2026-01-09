package com.cscorner.appproject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Question extends AppCompatActivity {

    private static final int TARGET_SCORE_TO_WIN = 10;
    private static final String TAG = "QuestionActivity";

    TextView timerView, txtScore, questionText;
    Button ans1, ans2, ans3;
    ImageView heart1, heart2, heart3;
    ImageButton pauseButton;

    int correctAnswers = 0;
    int totalScore = 0;
    int lives = 3;
    long timeLeft = 10000; // 10 seconds in milliseconds

    CountDownTimer timer;

    private List<QuestionModel> allQuestions;
    private int currentQuestionIndex = 0;
    private String selectedTopic = "";
    private String selectedLanguage = "kh";
    private ColorStateList defaultButtonColor;
    private final List<QuestionModel> answeredQuestions = new ArrayList<>();
    private boolean isGameOver = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_question);

        timerView = findViewById(R.id.timerView);
        txtScore = findViewById(R.id.txtScore);
        questionText = findViewById(R.id.questionText);
        ans1 = findViewById(R.id.ans1);
        ans2 = findViewById(R.id.ans2);
        ans3 = findViewById(R.id.ans3);
        heart1 = findViewById(R.id.heart1);
        heart2 = findViewById(R.id.heart2);
        heart3 = findViewById(R.id.heart3);
        pauseButton = findViewById(R.id.pause_button);

        defaultButtonColor = ans1.getBackgroundTintList();

        selectedTopic = getIntent().getStringExtra("TOPIC");
        String language = getIntent().getStringExtra("LANGUAGE");
        Log.d(TAG, "Received language from intent: " + language);
        if (language != null) {
            selectedLanguage = language;
        }
        Log.d(TAG, "Selected language after check: " + selectedLanguage);

        updateScoreDisplay();
        initializeQuestions();

        pauseButton.setOnClickListener(v -> showPauseDialog());
    }

    private void initializeQuestions() {
        allQuestions = new ArrayList<>();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        SharedPreferences prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        String userId = prefs.getString("user_id", null);

        if (userId == null) {
            handleQuestionLoadFailure("User ID not found. Please restart the app.");
            return;
        }

        db.collection("users").document(userId).get()
                .addOnSuccessListener(userDoc -> {
                    List<String> seenQuestionIds = userDoc.exists() ? getSeenQuestionIds(userDoc) : new ArrayList<>();
                    fetchQuestionsForTopic(seenQuestionIds);
                })
                .addOnFailureListener(e -> handleQuestionLoadFailure("Failed to retrieve user profile. Please check your connection."));
    }

    private void fetchQuestionsForTopic(@NonNull List<String> seenQuestionIds) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String topic = selectedTopic.toLowerCase();
        String language = selectedLanguage;

        Log.d(TAG, "Fetching questions for topic: " + topic + ", language: " + language);
        String path = "questions/" + topic + "/questions_by_lang/" + language;
        Log.d(TAG, "Firestore path: " + path);


        db.collection("questions").document(topic)
                .collection("questions_by_lang").document(language)
                .get()
                .addOnSuccessListener(questionDoc -> {
                    if (questionDoc.exists()) {
                        Log.d(TAG, "Successfully fetched question document for topic: " + topic);
                        parseQuestions(questionDoc, seenQuestionIds);
                        prepareRound();
                    } else {
                        handleQuestionLoadFailure("No questions found at path: " + path);
                    }
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch questions from path: " + path, e);
                    handleQuestionLoadFailure(e);
                });
    }


    @SuppressWarnings("unchecked")
    private List<String> getSeenQuestionIds(@NonNull DocumentSnapshot userDoc) {
        List<String> seenQuestionIds = new ArrayList<>();
        try {
            Map<String, Object> data = userDoc.getData();
            if (data != null && data.containsKey("seenQuestions")) {
                Map<String, Object> seenQuestionsMap = (Map<String, Object>) data.get("seenQuestions");
                if (seenQuestionsMap != null && seenQuestionsMap.containsKey(selectedTopic.toLowerCase())) {
                    Object idsObject = seenQuestionsMap.get(selectedTopic.toLowerCase());
                    if (idsObject instanceof List) {
                        seenQuestionIds.addAll((List<String>) idsObject);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading seen questions from Firestore.", e);
        }
        return seenQuestionIds;
    }

    @SuppressWarnings("unchecked")
    private void parseQuestions(@NonNull DocumentSnapshot questionDoc, @NonNull List<String> seenQuestionIds) {
        try {
            Object questionsObject = questionDoc.get("questions");
            if (!(questionsObject instanceof List)) {
                Log.e(TAG, "'questions' field is not a List or is missing in document: " + questionDoc.getId());
                return;
            }

            List<Map<String, Object>> questionsData = (List<Map<String, Object>>) questionsObject;
            Log.d(TAG, "Found " + questionsData.size() + " questions in the document.");

            for (Map<String, Object> questionMap : questionsData) {
                if (questionMap == null) {
                    Log.w(TAG, "Found a null question map, skipping.");
                    continue;
                }

                String id = (String) questionMap.get("id"); // CORRECTED KEY
                if (id == null) {
                    Log.w(TAG, "Question with null ID found, skipping.");
                    continue;
                }
                if (seenQuestionIds.contains(id)) {
                    Log.d(TAG, "Skipping seen question with ID: " + id);
                    continue;
                }

                String text = (String) questionMap.get("question"); // CORRECTED KEY
                Object answersObject = questionMap.get("options"); // CORRECTED KEY
                Object correctIndexObj = questionMap.get("correctOptionIndex");

                if (text != null && answersObject instanceof List && ((List<?>) answersObject).size() >= 3 && correctIndexObj instanceof Long) {
                    int correctIndex = ((Long) correctIndexObj).intValue();
                    List<String> answers = new ArrayList<>((List<String>) answersObject);

                    while (answers.size() > 3) {
                        answers.remove(answers.size() - 1);
                    }
                    while (answers.size() < 3) {
                        answers.add("N/A");
                    }


                    if (correctIndex >= 0 && correctIndex < 3) {
                        allQuestions.add(new QuestionModel(id, text, answers, correctIndex));
                        Log.d(TAG, "Successfully parsed and added question with ID: " + id);
                    } else {
                        Log.w(TAG, "Invalid correctOptionIndex for question ID: " + id + ". Index was: " + correctIndex);
                    }
                } else {
                    Log.w(TAG, "Skipping malformed question with ID: " + id + ". Check fields: question, options, correctOptionIndex.");
                }
            }
            Log.d(TAG, "Finished parsing. Total questions added: " + allQuestions.size());
        } catch (Exception e) {
            Log.e(TAG, "A critical error occurred while parsing questions from Firestore.", e);
        }
    }


    private void prepareRound() {
        if (allQuestions.isEmpty()) {
            resetTopicProgressAndReload();
            return;
        }
        Collections.shuffle(allQuestions);
        loadQuestion();
    }

    private void resetTopicProgressAndReload() {
        SharedPreferences prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        String userId = prefs.getString("user_id", null);
        final String topic = selectedTopic.toLowerCase();

        if (userId == null) {
            handleQuestionLoadFailure("Cannot reset progress: User ID not found.");
            return;
        }

        Toast.makeText(this, "You've completed this topic! Resetting progress.", Toast.LENGTH_LONG).show();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference userRef = db.collection("users").document(userId);
        String fieldPath = "seenQuestions." + topic;

        userRef.update(fieldPath, FieldValue.delete())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Successfully reset progress for topic: " + topic);
                    allQuestions.clear();
                    initializeQuestions();
                })
                .addOnFailureListener(e -> {
                    Log.d(TAG, "Could not delete field (it might not exist), reloading anyway.");
                    allQuestions.clear();
                    initializeQuestions();
                });
    }


    private void loadQuestion() {
        if (isGameOver) return;
        if (currentQuestionIndex >= allQuestions.size()) {
            gameOver(correctAnswers >= TARGET_SCORE_TO_WIN);
            return;
        }

        resetButtonUI();
        startTimer();

        QuestionModel currentQuestion = allQuestions.get(currentQuestionIndex);
        questionText.setText(currentQuestion.getQuestion());
        ans1.setText(currentQuestion.getAnswers().get(0));
        ans2.setText(currentQuestion.getAnswers().get(1));
        ans3.setText(currentQuestion.getAnswers().get(2));

        ans1.setOnClickListener(v -> handleAnswer(0));
        ans2.setOnClickListener(v -> handleAnswer(1));
        ans3.setOnClickListener(v -> handleAnswer(2));
    }

    private void handleAnswer(int selectedAnswerIndex) {
        if (isGameOver) return;
        timer.cancel();
        ans1.setEnabled(false);
        ans2.setEnabled(false);
        ans3.setEnabled(false);

        QuestionModel currentQuestion = allQuestions.get(currentQuestionIndex);
        answeredQuestions.add(currentQuestion);
        boolean isCorrect = selectedAnswerIndex == currentQuestion.getCorrectAnswerIndex();

        Button selectedButton = getButtonByIndex(selectedAnswerIndex);
        if (selectedButton == null) return;


        if (isCorrect) {
            correctAnswers++;
            totalScore += 10;
            updateScoreDisplay();
            selectedButton.setBackgroundTintList(ColorStateList.valueOf(Color.GREEN));

            if (correctAnswers >= TARGET_SCORE_TO_WIN) {
                new Handler(Looper.getMainLooper()).postDelayed(() -> gameOver(true), 1500);
                return;
            }
        } else {
            lives--;
            updateLivesDisplay();
            selectedButton.setBackgroundTintList(ColorStateList.valueOf(Color.RED));

            Button correctButton = getButtonByIndex(currentQuestion.getCorrectAnswerIndex());
            if (correctButton != null) {
                correctButton.setBackgroundTintList(ColorStateList.valueOf(Color.GREEN));
            }


            if (lives == 0) {
                new Handler(Looper.getMainLooper()).postDelayed(() -> gameOver(false), 1500);
                return;
            }
        }

        currentQuestionIndex++;
        new Handler(Looper.getMainLooper()).postDelayed(this::loadQuestion, 1500);
    }


    private void updateScoreDisplay() {
        String scoreLabel = "en".equals(selectedLanguage) ? "Correct: " : "ត្រឹមត្រូវ: ";
        txtScore.setText(scoreLabel + correctAnswers + "/" + TARGET_SCORE_TO_WIN);
    }

    private void updateLivesDisplay() {
        if (lives < 3) heart3.setVisibility(View.GONE);
        if (lives < 2) heart2.setVisibility(View.GONE);
        if (lives < 1) heart1.setVisibility(View.GONE);
    }

    private void resetButtonUI() {
        ans1.setBackgroundTintList(defaultButtonColor);
        ans2.setBackgroundTintList(defaultButtonColor);
        ans3.setBackgroundTintList(defaultButtonColor);
        ans1.setEnabled(true);
        ans2.setEnabled(true);
        ans3.setEnabled(true);
    }

    private void startTimer() {
        if (timer != null) {
            timer.cancel();
        }
        timeLeft = 10000;
        timerView.setText(String.valueOf(timeLeft / 1000));
        timer = new CountDownTimer(timeLeft, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeft = millisUntilFinished;
                timerView.setText(String.valueOf(timeLeft / 1000));
            }

            @Override
            public void onFinish() {
                if (isGameOver) return;
                if (currentQuestionIndex < allQuestions.size()) {
                    answeredQuestions.add(allQuestions.get(currentQuestionIndex));
                }
                lives--;
                updateLivesDisplay();
                if (lives > 0) {
                    currentQuestionIndex++;
                    new Handler(Looper.getMainLooper()).postDelayed(() -> loadQuestion(), 1500);
                } else {
                    gameOver(false);
                }
            }
        }.start();
    }

    private void showPauseDialog() {
        if (timer != null) {
            timer.cancel();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Game Paused");
        builder.setPositiveButton("Resume", (dialog, which) -> startTimer());
        builder.setNegativeButton("Quit", (dialog, which) -> gameOver(false));
        builder.setCancelable(false);
        builder.show();
    }

    private void handleQuestionLoadFailure(String message) {
        if (!isFinishing()) {
            Log.e(TAG, message);
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Question.this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }


    private void handleQuestionLoadFailure(Exception e) {
        if (e != null && e.getMessage() != null) {
            handleQuestionLoadFailure(e.getMessage());
        } else {
            handleQuestionLoadFailure("An unknown error occurred while loading questions.");
        }
    }

    private Button getButtonByIndex(int index) {
        if (index == 0) return ans1;
        if (index == 1) return ans2;
        if (index == 2) return ans3;
        return null; // Should not happen with valid data
    }


    private void gameOver(boolean isWin) {
        if (isGameOver) return; // Prevent multiple calls
        isGameOver = true;

        if (timer != null) {
            timer.cancel();
        }

        updateSeenQuestionsInFirestore(() -> {
            if (!isFinishing()) { 
                Intent intent;
                if (isWin) {
                    intent = new Intent(Question.this, GameWin.class);
                } else {
                    intent = new Intent(Question.this, GameOver.class);
                }
                intent.putExtra("score", totalScore);
                intent.putExtra("LANGUAGE", selectedLanguage);
                intent.putExtra("TOPIC", selectedTopic);
                startActivity(intent);
                finish();
            }
        });
    }

    private void updateSeenQuestionsInFirestore(Runnable onComplete) {
        SharedPreferences prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        String userId = prefs.getString("user_id", null);

        if (userId == null || answeredQuestions.isEmpty()) {
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }

        List<String> seenIds = new ArrayList<>();
        for (QuestionModel q : answeredQuestions) {
            seenIds.add(q.getQuestionId());
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String fieldPath = "seenQuestions." + selectedTopic.toLowerCase();

        db.collection("users").document(userId)
                .update(fieldPath, FieldValue.arrayUnion(seenIds.toArray(new String[0])))
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Successfully updated seen questions.");
                    } else {
                        Log.w(TAG, "Update failed, attempting to set with merge.", task.getException());
                        Map<String, Object> topicMap = new HashMap<>();
                        topicMap.put(selectedTopic.toLowerCase(), seenIds);
                        Map<String, Object> seenQuestionsMap = new HashMap<>();
                        seenQuestionsMap.put("seenQuestions", topicMap);

                        db.collection("users").document(userId).set(seenQuestionsMap, SetOptions.merge())
                                .addOnCompleteListener(retryTask -> {
                                    if (retryTask.isSuccessful()) {
                                        Log.d(TAG, "Successfully set seen questions with merge.");
                                    } else {
                                        Log.e(TAG, "Error updating seen questions, even on retry.", retryTask.getException());
                                    }
                                });
                    }
                    if (onComplete != null) {
                        onComplete.run();
                    }
                });
    }
}
