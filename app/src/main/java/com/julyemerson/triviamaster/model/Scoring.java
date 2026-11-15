package com.julyemerson.triviamaster.model;

/**
 * Fórmula de pontuação:
 *   acerto = 10 × multiplicador da dificuldade (fácil 1, média 2, difícil 3) + segundosRestantes / 2
 *   erro ou tempo esgotado = 0
 * Recompensa dificuldade e rapidez sem punir quem erra.
 */
public final class Scoring {

    public static final int BASE_POINTS = 10;

    private Scoring() { }

    public static int pointsForCorrectAnswer(String difficulty, int secondsLeft) {
        return BASE_POINTS * multiplier(difficulty) + secondsLeft / 2;
    }

    private static int multiplier(String difficulty) {
        // constante.equals(variavel) não quebra se difficulty vier null
        if (Question.DIFFICULTY_HARD.equals(difficulty)) return 3;
        if (Question.DIFFICULTY_MEDIUM.equals(difficulty)) return 2;
        return 1;
    }
}
