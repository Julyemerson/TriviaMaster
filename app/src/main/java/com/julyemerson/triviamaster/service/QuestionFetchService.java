package com.julyemerson.triviamaster.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.julyemerson.triviamaster.R;
import com.julyemerson.triviamaster.data.OpenTriviaClient;
import com.julyemerson.triviamaster.data.TriviaApiException;
import com.julyemerson.triviamaster.model.MatchConfig;
import com.julyemerson.triviamaster.model.Question;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Baixa as questões em segundo plano, desacoplado da tela.
 * Não conhece nenhuma Activity: só avisa o resultado por broadcast (READY ou FAILED).
 */
public class QuestionFetchService extends Service {

    private static final String TAG = "QuestionFetchService";

    public static final String ACTION_QUESTIONS_READY = "com.julyemerson.triviamaster.action.QUESTIONS_READY";
    public static final String ACTION_QUESTIONS_FAILED = "com.julyemerson.triviamaster.action.QUESTIONS_FAILED";
    public static final String EXTRA_CONFIG = "extra_config";
    public static final String EXTRA_QUESTIONS = "extra_questions";
    public static final String EXTRA_ERROR_MESSAGE = "extra_error_message"; // @StringRes int

    private static final long RATE_LIMIT_WAIT_MS = 5000; // a API aceita ~1 requisição a cada 5 s

    // Thread única: se chegarem dois pedidos, rodam um depois do outro (ajuda com o rate limit)
    private ExecutorService executor;

    /** Atalho para quem quer iniciar o Service: não precisa conhecer as chaves dos extras. */
    public static void start(Context context, MatchConfig config) {
        Intent intent = new Intent(context, QuestionFetchService.class);
        intent.putExtra(EXTRA_CONFIG, config);
        context.startService(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        executor = Executors.newSingleThreadExecutor();
    }

    @Override
    @SuppressWarnings("deprecation")
    public int onStartCommand(Intent intent, int flags, int startId) {
        // onStartCommand roda na MAIN thread: aqui só despachamos o trabalho para o Executor
        MatchConfig config = intent == null ? null : (MatchConfig) intent.getSerializableExtra(EXTRA_CONFIG);
        if (config == null) {
            stopSelf(startId);
            return START_NOT_STICKY;
        }

        executor.execute(() -> {
            fetchAndBroadcast(config);
            // stopSelf(startId): só encerra se este for o último pedido recebido
            stopSelf(startId);
        });
        // NOT_STICKY: se o sistema matar o Service, não precisa recriar (a tela já não espera mais)
        return START_NOT_STICKY;
    }

    /** Roda na thread do Executor. */
    private void fetchAndBroadcast(MatchConfig config) {
        try {
            sendReady(config, fetchWithRetry(config));
        } catch (TriviaApiException e) {
            Log.w(TAG, "Falha ao buscar questões", e);
            sendFailed(e.getReason().messageRes);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Service sendo destruído durante a espera
        } catch (RuntimeException e) {
            // Qualquer erro inesperado vira mensagem, nunca deixa a tela esperando para sempre
            Log.e(TAG, "Erro inesperado", e);
            sendFailed(R.string.error_unexpected);
        }
    }

    /** Se cair no rate limit, espera 5 s e tenta mais uma vez. */
    private ArrayList<Question> fetchWithRetry(MatchConfig config)
            throws TriviaApiException, InterruptedException {
        try {
            return fetch(config);
        } catch (TriviaApiException e) {
            if (e.getReason() != TriviaApiException.Reason.RATE_LIMIT) throw e;
            Log.i(TAG, "Rate limit: nova tentativa em 5 s");
            Thread.sleep(RATE_LIMIT_WAIT_MS); // pode dormir: estamos fora da main thread
            return fetch(config);
        }
    }

    private ArrayList<Question> fetch(MatchConfig config) throws TriviaApiException {
        return OpenTriviaClient.fetchQuestions(
                config.getAmount(), config.getCategoryId(), config.getDifficulty());
    }

    private void sendReady(MatchConfig config, ArrayList<Question> questions) {
        Intent intent = new Intent(ACTION_QUESTIONS_READY);
        intent.putExtra(EXTRA_CONFIG, config);
        intent.putExtra(EXTRA_QUESTIONS, questions);
        sendInternalBroadcast(intent);
    }

    private void sendFailed(int messageRes) {
        Intent intent = new Intent(ACTION_QUESTIONS_FAILED);
        intent.putExtra(EXTRA_ERROR_MESSAGE, messageRes);
        sendInternalBroadcast(intent);
    }

    /** setPackage: o broadcast só é entregue para o nosso próprio app. */
    private void sendInternalBroadcast(Intent intent) {
        intent.setPackage(getPackageName());
        sendBroadcast(intent);
    }

    @Override
    public void onDestroy() {
        executor.shutdown();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // Service "started", não "bound"
    }
}
