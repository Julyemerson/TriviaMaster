package com.julyemerson.triviamaster.model;

import java.io.Serializable;

/**
 * Configuração escolhida na tela inicial.
 * Guarda o valor da API (categoryId, difficulty) e o rótulo exibido (categoryName, difficultyLabel).
 */
public class MatchConfig implements Serializable {

    private final String playerName;
    private final int categoryId;          // 0 = qualquer categoria
    private final String categoryName;     // rótulo em português
    private final String categoryApiName;  // nome gravado nas questões pela API ("" = qualquer)
    private final String difficulty;       // "" = qualquer, senão easy/medium/hard
    private final String difficultyLabel;
    private final int amount;

    public MatchConfig(String playerName, int categoryId, String categoryName, String categoryApiName,
                       String difficulty, String difficultyLabel, int amount) {
        this.playerName = playerName;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.categoryApiName = categoryApiName;
        this.difficulty = difficulty;
        this.difficultyLabel = difficultyLabel;
        this.amount = amount;
    }

    public String getPlayerName() { return playerName; }
    public int getCategoryId() { return categoryId; }
    public String getCategoryName() { return categoryName; }
    public String getCategoryApiName() { return categoryApiName; }
    public String getDifficulty() { return difficulty; }
    public String getDifficultyLabel() { return difficultyLabel; }
    public int getAmount() { return amount; }
}
