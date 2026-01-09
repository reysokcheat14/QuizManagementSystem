package com.cscorner.appproject;

import java.util.List;

public class QuestionModel {
    private String questionId;
    private String question;
    private List<String> answers;
    private int correctAnswerIndex;

    public QuestionModel(String questionId, String question, List<String> answers, int correctAnswerIndex) {
        this.questionId = questionId;
        this.question = question;
        this.answers = answers;
        this.correctAnswerIndex = correctAnswerIndex;
    }

    public String getQuestionId() {
        return questionId;
    }

    public String getQuestion() {
        return question;
    }

    public List<String> getAnswers() {
        return answers;
    }

    public int getCorrectAnswerIndex() {
        return correctAnswerIndex;
    }
}
