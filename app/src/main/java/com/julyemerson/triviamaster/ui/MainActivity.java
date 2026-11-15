package com.julyemerson.triviamaster.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.julyemerson.triviamaster.R;
import com.julyemerson.triviamaster.model.MatchConfig;
import com.julyemerson.triviamaster.model.Question;
import com.julyemerson.triviamaster.receiver.QuestionsReceiver;
import com.julyemerson.triviamaster.service.QuestionFetchService;

import java.util.ArrayList;

/** Tela inicial: identifica o jogador, configura a partida e pede as questões ao Service. */
public class MainActivity extends AppCompatActivity implements QuestionsReceiver.Listener {

    private EditText editPlayerName;
    private Spinner spinnerCategory;
    private Spinner spinnerDifficulty;
    private Spinner spinnerAmount;
    private Button buttonStart;
    private ProgressBar progressLoading;

    private final QuestionsReceiver questionsReceiver = new QuestionsReceiver(this);

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
        buttonStart = findViewById(R.id.buttonStart);
        progressLoading = findViewById(R.id.progressLoading);

        buttonStart.setOnClickListener(v -> onStartClicked());
        findViewById(R.id.buttonHistory).setOnClickListener(
                v -> startActivity(new Intent(this, HistoryActivity.class)));
    }

    @Override
    protected void onStart() {
        super.onStart();
        // NOT_EXPORTED: só broadcasts do nosso próprio app chegam aqui
        ContextCompat.registerReceiver(this, questionsReceiver,
                QuestionsReceiver.createFilter(), ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    @Override
    protected void onStop() {
        // Tela invisível não deve reagir (nem abrir o quiz por cima de outro app)
        unregisterReceiver(questionsReceiver);
        setLoading(false);
        super.onStop();
    }

    private void onStartClicked() {
        String playerName = editPlayerName.getText().toString().trim();
        if (playerName.isEmpty()) {
            editPlayerName.setError(getString(R.string.error_empty_name));
            editPlayerName.requestFocus();
            return;
        }
        setLoading(true); // botão desabilitado = sem cliques duplos (evita rate limit)
        QuestionFetchService.start(this, buildMatchConfig(playerName));
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

    // ----- QuestionsReceiver.Listener (chamados na main thread) -----

    @Override
    public void onQuestionsReady(MatchConfig config, ArrayList<Question> questions) {
        setLoading(false);
        startActivity(QuizActivity.newIntent(this, config, questions));
    }

    @Override
    public void onQuestionsFailed(int messageRes) {
        setLoading(false);
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_error_title)
                .setMessage(messageRes)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void setLoading(boolean loading) {
        progressLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        buttonStart.setEnabled(!loading);
    }
}
