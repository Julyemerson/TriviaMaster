package com.julyemerson.triviamaster.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Uma questão de múltipla escolha.
 * Serializable para poder ser enviada entre telas dentro de um Intent.
 */
public class Question implements Serializable {

    public static final String DIFFICULTY_EASY = "easy";
    public static final String DIFFICULTY_MEDIUM = "medium";
    public static final String DIFFICULTY_HARD = "hard";

    private final String category;
    private final String difficulty;
    private final String text;
    private final String correctAnswer;
    private final List<String> answers; // correta + incorretas, já embaralhadas

    public Question(String category, String difficulty, String text,
                    String correctAnswer, List<String> incorrectAnswers) {
        this.category = category;
        this.difficulty = difficulty;
        this.text = text;
        this.correctAnswer = correctAnswer;

        // Embaralha uma vez só, na criação, para a correta não ficar sempre na mesma posição
        this.answers = new ArrayList<>(incorrectAnswers);
        this.answers.add(correctAnswer);
        Collections.shuffle(this.answers);
    }

    public boolean isCorrect(String answer) {
        return correctAnswer.equals(answer);
    }

    public String getCategory() { return category; }
    public String getDifficulty() { return difficulty; }
    public String getText() { return text; }
    public String getCorrectAnswer() { return correctAnswer; }
    public List<String> getAnswers() { return answers; }
}
