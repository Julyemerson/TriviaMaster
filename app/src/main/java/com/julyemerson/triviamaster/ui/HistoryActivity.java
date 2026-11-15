package com.julyemerson.triviamaster.ui;

import android.content.ContentResolver;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.julyemerson.triviamaster.R;
import com.julyemerson.triviamaster.data.MatchStorage;
import com.julyemerson.triviamaster.model.MatchResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Lista das partidas salvas, mais recentes primeiro. */
public class HistoryActivity extends AppCompatActivity {

    private static final String TAG = "HistoryActivity";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private ListView listMatches;
    private ProgressBar progressHistory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_history);
        SystemBars.applyPadding(findViewById(R.id.main));

        listMatches = findViewById(R.id.listMatches);
        progressHistory = findViewById(R.id.progressHistory);
        loadMatches();
    }

    /** Lê o banco numa thread de fundo e entrega a lista para a main thread. */
    private void loadMatches() {
        progressHistory.setVisibility(View.VISIBLE);
        ContentResolver resolver = getContentResolver();
        executor.execute(() -> {
            List<MatchResult> matches = loadMatchesSafely(resolver);
            mainHandler.post(() -> showMatches(matches));
        });
    }

    /** Roda na thread de fundo. Devolve null se der erro. */
    private List<MatchResult> loadMatchesSafely(ContentResolver resolver) {
        try {
            return MatchStorage.loadAll(resolver);
        } catch (Exception e) {
            Log.e(TAG, "Erro ao carregar histórico", e);
            return null;
        }
    }

    /** Roda na main thread: só aqui é permitido mexer nas Views. */
    private void showMatches(List<MatchResult> matches) {
        if (isFinishing() || isDestroyed()) return; // usuário saiu antes de terminar

        progressHistory.setVisibility(View.GONE);
        if (matches == null) {
            Toast.makeText(this, R.string.error_load_history, Toast.LENGTH_LONG).show();
            matches = new ArrayList<>();
        }
        // setEmptyView só agora: antes de carregar, não queremos mostrar "nenhuma partida"
        listMatches.setEmptyView(findViewById(R.id.textEmptyHistory));
        listMatches.setAdapter(new MatchAdapter(this, matches));
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow(); // leitura pode ser abandonada se a tela fechou
        super.onDestroy();
    }
}
