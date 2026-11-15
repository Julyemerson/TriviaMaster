package com.julyemerson.triviamaster.ui;

import android.os.Bundle;
import android.widget.ListView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.julyemerson.triviamaster.R;
import com.julyemerson.triviamaster.model.MockMatches;

/** Lista das partidas jogadas, mais recentes primeiro. */
public class HistoryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_history);
        SystemBars.applyPadding(findViewById(R.id.main));

        ListView listMatches = findViewById(R.id.listMatches);
        listMatches.setEmptyView(findViewById(R.id.textEmptyHistory));

        // Fase 1: dados mockados. Na Fase 2: leitura via ContentProvider num Executor.
        listMatches.setAdapter(new MatchAdapter(this, MockMatches.get()));
    }
}
