package com.julyemerson.triviamaster.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.julyemerson.triviamaster.R;
import com.julyemerson.triviamaster.model.MatchConfig;
import com.julyemerson.triviamaster.model.MockQuestions;

/** Tela inicial: identifica o jogador e configura a partida. */
public class MainActivity extends AppCompatActivity {

    private EditText editPlayerName;
    private Spinner spinnerCategory;
    private Spinner spinnerDifficulty;
    private Spinner spinnerAmount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        SystemBars.applyPadding(findViewById(R.id.main));

        editPlayerName = findViewById(R.id.editPlayerName);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerDifficulty = findViewById(R.id.spinnerDifficulty);
        spinnerAmount = findViewById(R.id.spinnerAmount);

        Button buttonStart = findViewById(R.id.buttonStart);
        Button buttonHistory = findViewById(R.id.buttonHistory);
        buttonStart.setOnClickListener(v -> onStartClicked());
        buttonHistory.setOnClickListener(v -> onHistoryClicked());
    }

    private void onStartClicked() {
        String playerName = editPlayerName.getText().toString().trim();
        if (playerName.isEmpty()) {
            editPlayerName.setError(getString(R.string.error_empty_name));
            editPlayerName.requestFocus();
            return;
        }

        MatchConfig config = buildMatchConfig(playerName);
        // Fase 1: questões mockadas. Na Fase 3 isso vira o Service que chama a API.
        startActivity(QuizActivity.newIntent(this, config, MockQuestions.get(config.getAmount())));
    }

    /** Os arrays de nomes e de valores têm a mesma ordem: a posição do Spinner é o índice. */
    private MatchConfig buildMatchConfig(String playerName) {
        int categoryPos = spinnerCategory.getSelectedItemPosition();
        int difficultyPos = spinnerDifficulty.getSelectedItemPosition();
        return new MatchConfig(
                playerName,
                getResources().getIntArray(R.array.category_ids)[categoryPos],
                spinnerCategory.getSelectedItem().toString(),
                getResources().getStringArray(R.array.difficulty_values)[difficultyPos],
                spinnerDifficulty.getSelectedItem().toString(),
                Integer.parseInt(spinnerAmount.getSelectedItem().toString()));
    }

    private void onHistoryClicked() {
        startActivity(new Intent(this, HistoryActivity.class));
    }
}
