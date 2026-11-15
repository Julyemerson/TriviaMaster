package com.julyemerson.triviamaster.model;

import java.io.Serializable;

/** O que aconteceu em uma questão da partida, para montar o resumo final. */
public class AnswerRecord implements Serializable {

    private final String questionText;
    private final String chosenAnswer;   // null = tempo esgotado
    private final String correctAnswer;
    private final boolean correct;
    private final int points;

    public AnswerRecord(String questionText, String chosenAnswer, String correctAnswer,
                        boolean correct, int points) {
        this.questionText = questionText;
        this.chosenAnswer = chosenAnswer;
        this.correctAnswer = correctAnswer;
        this.correct = correct;
        this.points = points;
    }

    public boolean isTimeUp() { return chosenAnswer == null; }

    public String getQuestionText() { return questionText; }
    public String getChosenAnswer() { return chosenAnswer; }
    public String getCorrectAnswer() { return correctAnswer; }
    public boolean isCorrect() { return correct; }
    public int getPoints() { return points; }
}
