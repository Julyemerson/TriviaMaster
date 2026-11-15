package com.julyemerson.triviamaster.model;

import java.io.Serializable;

/** Resultado de uma partida: é o que vai para o banco e aparece no histórico. */
public class MatchResult implements Serializable {

    private final String playerName;
    private final long dateMillis;
    private final String category;
    private final String difficulty;
    private final int correctCount;
    private final int totalQuestions;
    private final int score;

    public MatchResult(String playerName, long dateMillis, String category, String difficulty,
                       int correctCount, int totalQuestions, int score) {
        this.playerName = playerName;
        this.dateMillis = dateMillis;
        this.category = category;
        this.difficulty = difficulty;
        this.correctCount = correctCount;
        this.totalQuestions = totalQuestions;
        this.score = score;
    }

    /** Percentual de acertos (0 a 100). */
    public int getPercentage() {
        return totalQuestions == 0 ? 0 : correctCount * 100 / totalQuestions;
    }

    public String getPlayerName() { return playerName; }
    public long getDateMillis() { return dateMillis; }
    public String getCategory() { return category; }
    public String getDifficulty() { return difficulty; }
    public int getCorrectCount() { return correctCount; }
    public int getTotalQuestions() { return totalQuestions; }
    public int getScore() { return score; }
}
