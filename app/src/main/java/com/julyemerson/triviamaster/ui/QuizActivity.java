package com.julyemerson.triviamaster.ui;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.ColorRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.julyemerson.triviamaster.R;
import com.julyemerson.triviamaster.model.AnswerRecord;
import com.julyemerson.triviamaster.model.MatchConfig;
import com.julyemerson.triviamaster.model.MatchResult;
import com.julyemerson.triviamaster.model.Question;
import com.julyemerson.triviamaster.model.Scoring;

import java.util.ArrayList;
import java.util.List;

/** Mostra uma questão por vez, com temporizador, feedback e placar. */
public class QuizActivity extends AppCompatActivity {

    private static final String EXTRA_CONFIG = "extra_config";
    private static final String EXTRA_QUESTIONS = "extra_questions";

    private static final int QUESTION_TIME_SECONDS = 20;
    private static final long TIMER_TICK_MS = 100;      // tick curto = barra anda suave
    private static final long FEEDBACK_DELAY_MS = 1000; // tempo mostrando certo/errado

    // Handler da main thread: agenda o "avançar" depois do feedback
    private final Handler handler = new Handler(Looper.getMainLooper());

    private MatchConfig config;
    private ArrayList<Question> questions;
    private final ArrayList<AnswerRecord> answerRecords = new ArrayList<>();
    private int currentIndex = 0;
    private int correctCount = 0;
    private int score = 0;
    private int secondsLeft;
    private boolean answerLocked; // impede responder duas vezes a mesma questão
    private CountDownTimer timer;

    private TextView textProgress, textScore, textTimer, textCategory, textQuestion, textFeedback;
    private ProgressBar progressTimer;
    private Button[] answerButtons;
    private ColorStateList defaultButtonTint;

    /** Monta o Intent com tudo que a tela precisa; quem chama não precisa conhecer as chaves. */
    public static Intent newIntent(Context context, MatchConfig config, ArrayList<Question> questions) {
        Intent intent = new Intent(context, QuizActivity.class);
        intent.putExtra(EXTRA_CONFIG, config);
        intent.putExtra(EXTRA_QUESTIONS, questions);
        return intent;
    }

    @Override
    @SuppressWarnings({"unchecked", "deprecation"})
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_quiz);
        SystemBars.applyPadding(findViewById(R.id.main));

        config = (MatchConfig) getIntent().getSerializableExtra(EXTRA_CONFIG);
        questions = (ArrayList<Question>) getIntent().getSerializableExtra(EXTRA_QUESTIONS);
        if (config == null || questions == null || questions.isEmpty()) {
            // Nunca fechar com exceção: avisa e volta
            Toast.makeText(this, R.string.error_no_questions, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        bindViews();
        showQuestion();
    }

    private void bindViews() {
        textProgress = findViewById(R.id.textProgress);
        textScore = findViewById(R.id.textScore);
        textTimer = findViewById(R.id.textTimer);
        textCategory = findViewById(R.id.textCategory);
        textQuestion = findViewById(R.id.textQuestion);
        textFeedback = findViewById(R.id.textFeedback);
        progressTimer = findViewById(R.id.progressTimer);
        progressTimer.setMax(QUESTION_TIME_SECONDS * 1000);

        answerButtons = new Button[]{
                findViewById(R.id.buttonAnswer1), findViewById(R.id.buttonAnswer2),
                findViewById(R.id.buttonAnswer3), findViewById(R.id.buttonAnswer4)};
        defaultButtonTint = answerButtons[0].getBackgroundTintList();
        for (Button button : answerButtons) {
            button.setOnClickListener(v -> onAnswerSelected((Button) v));
        }
    }

    private void showQuestion() {
        Question question = currentQuestion();
        answerLocked = false;

        textProgress.setText(getString(R.string.quiz_progress, currentIndex + 1, questions.size()));
        textScore.setText(getString(R.string.quiz_score, score));
        textCategory.setText(getString(R.string.quiz_category,
                question.getCategory(), question.getDifficulty()));
        textQuestion.setText(question.getText());
        textFeedback.setText("");

        List<String> answers = question.getAnswers();
        for (int i = 0; i < answerButtons.length; i++) {
            Button button = answerButtons[i];
            button.setBackgroundTintList(defaultButtonTint);
            // Proteção: se vier uma questão com menos de 4 alternativas, esconde o botão
            if (i < answers.size()) {
                button.setText(answers.get(i));
                button.setVisibility(View.VISIBLE);
            } else {
                button.setVisibility(View.GONE);
            }
        }
        startTimer();
    }

    private void startTimer() {
        cancelTimer();
        long totalMs = QUESTION_TIME_SECONDS * 1000L;
        updateTimerViews(totalMs);
        timer = new CountDownTimer(totalMs, TIMER_TICK_MS) {
            @Override
            public void onTick(long millisUntilFinished) {
                updateTimerViews(millisUntilFinished);
            }

            @Override
            public void onFinish() {
                updateTimerViews(0);
                onTimeUp();
            }
        }.start();
    }

    private void updateTimerViews(long millisLeft) {
        secondsLeft = (int) Math.ceil(millisLeft / 1000.0);
        progressTimer.setProgress((int) millisLeft);
        textTimer.setText(getString(R.string.quiz_seconds, secondsLeft));
    }

    private void onAnswerSelected(Button chosen) {
        if (answerLocked) return;
        answerLocked = true;
        cancelTimer();

        Question question = currentQuestion();
        String chosenAnswer = chosen.getText().toString();
        boolean correct = question.isCorrect(chosenAnswer);
        int points = 0;
        if (correct) {
            points = Scoring.pointsForCorrectAnswer(question.getDifficulty(), secondsLeft);
            score += points;
            correctCount++;
            showFeedback(getString(R.string.feedback_correct, points), R.color.answer_correct);
        } else {
            paint(chosen, R.color.answer_wrong);
            showFeedback(getString(R.string.feedback_wrong), R.color.answer_wrong);
        }
        recordAnswer(question, chosenAnswer, correct, points);
        revealCorrectAnswerAndAdvance();
    }

    /** Tempo esgotado conta como erro (0 pontos). */
    private void onTimeUp() {
        if (answerLocked) return;
        answerLocked = true;
        showFeedback(getString(R.string.feedback_time_up), R.color.answer_wrong);
        recordAnswer(currentQuestion(), null, false, 0); // null = tempo esgotado
        revealCorrectAnswerAndAdvance();
    }

    private void recordAnswer(Question question, String chosenAnswer, boolean correct, int points) {
        answerRecords.add(new AnswerRecord(question.getText(), chosenAnswer,
                question.getCorrectAnswer(), correct, points));
    }

    private void revealCorrectAnswerAndAdvance() {
        Question question = currentQuestion();
        for (Button button : answerButtons) {
            if (question.isCorrect(button.getText().toString())) {
                paint(button, R.color.answer_correct);
            }
        }
        textScore.setText(getString(R.string.quiz_score, score));
        handler.postDelayed(this::goToNextQuestion, FEEDBACK_DELAY_MS);
    }

    private void goToNextQuestion() {
        currentIndex++;
        if (currentIndex < questions.size()) {
            showQuestion();
        } else {
            finishQuiz();
        }
    }

    private void finishQuiz() {
        MatchResult result = new MatchResult(config.getPlayerName(), System.currentTimeMillis(),
                config.getCategoryName(), config.getDifficultyLabel(),
                correctCount, questions.size(), score);
        startActivity(ResultActivity.newIntent(this, result, answerRecords));
        finish(); // tira o quiz da pilha: "Voltar" no resultado não reabre a partida
    }

    private Question currentQuestion() {
        return questions.get(currentIndex);
    }

    private void showFeedback(String message, @ColorRes int colorRes) {
        textFeedback.setText(message);
        textFeedback.setTextColor(ContextCompat.getColor(this, colorRes));
    }

    private void paint(Button button, @ColorRes int colorRes) {
        button.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, colorRes)));
    }

    private void cancelTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    @Override
    protected void onDestroy() {
        // Sem isso, o timer e o "avançar" agendado continuariam rodando com a tela já fechada
        cancelTimer();
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
