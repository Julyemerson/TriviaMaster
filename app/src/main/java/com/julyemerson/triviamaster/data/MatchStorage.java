package com.julyemerson.triviamaster.data;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import com.julyemerson.triviamaster.data.TriviaContract.Matches;
import com.julyemerson.triviamaster.data.TriviaContract.Players;
import com.julyemerson.triviamaster.model.MatchResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Converte MatchResult <-> ContentValues/Cursor e conversa com o TriviaProvider.
 * ATENÇÃO: faz I/O de banco. Sempre chamar fora da main thread (num Executor).
 */
public final class MatchStorage {

    private MatchStorage() { }

    /** Salva o jogador (se ainda não existe) e a partida. Lança exceção se falhar. */
    public static void save(ContentResolver resolver, MatchResult result) {
        ContentValues player = new ContentValues();
        player.put(Players.COLUMN_NAME, result.getPlayerName());
        Uri playerUri = resolver.insert(Players.CONTENT_URI, player);
        if (playerUri == null) throw new IllegalStateException("Falha ao salvar jogador");

        ContentValues match = new ContentValues();
        match.put(Matches.COLUMN_PLAYER_ID, ContentUris.parseId(playerUri));
        match.put(Matches.COLUMN_DATE, result.getDateMillis());
        match.put(Matches.COLUMN_CATEGORY, result.getCategory());
        match.put(Matches.COLUMN_DIFFICULTY, result.getDifficulty());
        match.put(Matches.COLUMN_CORRECT_COUNT, result.getCorrectCount());
        match.put(Matches.COLUMN_TOTAL, result.getTotalQuestions());
        match.put(Matches.COLUMN_SCORE, result.getScore());
        if (resolver.insert(Matches.CONTENT_URI, match) == null) {
            throw new IllegalStateException("Falha ao salvar partida");
        }
    }

    /** Todas as partidas, mais recentes primeiro. */
    public static List<MatchResult> loadAll(ContentResolver resolver) {
        List<MatchResult> matches = new ArrayList<>();
        // try-with-resources fecha o Cursor automaticamente, mesmo se der erro
        try (Cursor cursor = resolver.query(Matches.CONTENT_URI, null, null, null,
                Matches.COLUMN_DATE + " DESC")) {
            if (cursor == null) return matches;

            int name = cursor.getColumnIndexOrThrow(Matches.COLUMN_PLAYER_NAME);
            int date = cursor.getColumnIndexOrThrow(Matches.COLUMN_DATE);
            int category = cursor.getColumnIndexOrThrow(Matches.COLUMN_CATEGORY);
            int difficulty = cursor.getColumnIndexOrThrow(Matches.COLUMN_DIFFICULTY);
            int correct = cursor.getColumnIndexOrThrow(Matches.COLUMN_CORRECT_COUNT);
            int total = cursor.getColumnIndexOrThrow(Matches.COLUMN_TOTAL);
            int score = cursor.getColumnIndexOrThrow(Matches.COLUMN_SCORE);

            while (cursor.moveToNext()) {
                matches.add(new MatchResult(cursor.getString(name), cursor.getLong(date),
                        cursor.getString(category), cursor.getString(difficulty),
                        cursor.getInt(correct), cursor.getInt(total), cursor.getInt(score)));
            }
        }
        return matches;
    }
}
