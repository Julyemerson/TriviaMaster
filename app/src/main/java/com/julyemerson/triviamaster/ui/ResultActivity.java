package com.julyemerson.triviamaster.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.julyemerson.triviamaster.R;
import com.julyemerson.triviamaster.model.AnswerRecord;
import com.julyemerson.triviamaster.model.MatchResult;

import java.util.ArrayList;

/** Resumo completo da partida. Na Fase 2 também salvará o resultado via ContentProvider. */
public class ResultActivity extends AppCompatActivity {

    private static final String EXTRA_RESULT = "extra_result";
    private static final String EXTRA_ANSWERS = "extra_answers";

    public static Intent newIntent(Context context, MatchResult result, ArrayList<AnswerRecord> answers) {
        Intent intent = new Intent(context, ResultActivity.class);
        intent.putExtra(EXTRA_RESULT, result);
        intent.putExtra(EXTRA_ANSWERS, answers);
        return intent;
    }

    @Override
    @SuppressWarnings({"unchecked", "deprecation"})
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_result);
        SystemBars.applyPadding(findViewById(R.id.main));

        MatchResult result = (MatchResult) getIntent().getSerializableExtra(EXTRA_RESULT);
        ArrayList<AnswerRecord> answers =
                (ArrayList<AnswerRecord>) getIntent().getSerializableExtra(EXTRA_ANSWERS);
        if (result == null || answers == null) {
            Toast.makeText(this, R.string.error_no_result, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        showSummary(result);
        showAnswers(answers);

        // "Jogar novamente": a MainActivity já está embaixo na pilha, basta fechar esta tela
        findViewById(R.id.buttonPlayAgain).setOnClickListener(v -> finish());
        findViewById(R.id.buttonResultHistory).setOnClickListener(
                v -> startActivity(new Intent(this, HistoryActivity.class)));
    }

    private void showSummary(MatchResult result) {
        ((TextView) findViewById(R.id.textPlayer)).setText(result.getPlayerName());
        ((TextView) findViewById(R.id.textFinalScore))
                .setText(getString(R.string.result_score, result.getScore()));
        ((TextView) findViewById(R.id.textCorrectCount)).setText(getString(R.string.result_correct_count,
                result.getCorrectCount(), result.getTotalQuestions(), result.getPercentage()));
        ((TextView) findViewById(R.id.textMatchConfig)).setText(getString(R.string.quiz_category,
                result.getCategory(), result.getDifficulty()));
    }

    /** Infla um item_answer para cada questão (no máximo 20, então não precisa de ListView). */
    private void showAnswers(ArrayList<AnswerRecord> answers) {
        LinearLayout container = findViewById(R.id.containerAnswers);
        LayoutInflater inflater = LayoutInflater.from(this);

        for (int i = 0; i < answers.size(); i++) {
            AnswerRecord answer = answers.get(i);
            View row = inflater.inflate(R.layout.item_answer, container, false);

            ((TextView) row.findViewById(R.id.textAnswerQuestion)).setText(
                    getString(R.string.result_question_number, i + 1, answer.getQuestionText()));

            TextView textChosen = row.findViewById(R.id.textAnswerChosen);
            textChosen.setText(chosenAnswerText(answer));
            textChosen.setTextColor(ContextCompat.getColor(this,
                    answer.isCorrect() ? R.color.answer_correct : R.color.answer_wrong));

            ((TextView) row.findViewById(R.id.textAnswerCorrect)).setText(
                    getString(R.string.result_correct_answer, answer.getCorrectAnswer()));

            container.addView(row);
        }
    }

    private String chosenAnswerText(AnswerRecord answer) {
        if (answer.isTimeUp()) return getString(R.string.result_time_up);
        if (answer.isCorrect()) {
            return getString(R.string.result_chosen_correct, answer.getChosenAnswer(), answer.getPoints());
        }
        return getString(R.string.result_chosen_wrong, answer.getChosenAnswer());
    }
}
