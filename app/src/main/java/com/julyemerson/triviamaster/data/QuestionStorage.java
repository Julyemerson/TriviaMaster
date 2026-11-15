package com.julyemerson.triviamaster.data;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.julyemerson.triviamaster.data.TriviaContract.Questions;
import com.julyemerson.triviamaster.model.MatchConfig;
import com.julyemerson.triviamaster.model.Question;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Cache de questões para jogar offline, sempre via TriviaProvider.
 * Estratégia: toda questão baixada é salva (máx. 300, apaga as mais antigas).
 * Offline: primeiro questões da mesma categoria/dificuldade; se faltar, completa com qualquer uma.
 * ATENÇÃO: I/O de banco. Sempre chamar fora da main thread.
 */
public final class QuestionStorage {

    private static final String TAG = "QuestionStorage";
    public static final int MAX_CACHED_QUESTIONS = 300;

    private QuestionStorage() { }

    /** Salva as questões baixadas (repetidas são ignoradas) e apaga o excesso. */
    public static void saveAll(ContentResolver resolver, List<Question> questions) {
        long now = System.currentTimeMillis();
        ContentValues[] rows = new ContentValues[questions.size()];
        for (int i = 0; i < questions.size(); i++) {
            rows[i] = toContentValues(questions.get(i), now);
        }
        resolver.bulkInsert(Questions.CONTENT_URI, rows);
        trimToMaxSize(resolver);
    }

    /** Mantém só as 300 mais recentes (maior _id = inserida por último). */
    private static void trimToMaxSize(ContentResolver resolver) {
        String keepNewest = Questions._ID + " NOT IN (SELECT " + Questions._ID
                + " FROM " + Questions.TABLE
                + " ORDER BY " + Questions._ID + " DESC LIMIT " + MAX_CACHED_QUESTIONS + ")";
        resolver.delete(Questions.CONTENT_URI, keepNewest, null);
    }

    /** Até config.getAmount() questões do cache, sorteadas. Pode devolver menos (ou nenhuma). */
    public static ArrayList<Question> loadForOffline(ContentResolver resolver, MatchConfig config) {
        int amount = config.getAmount();
        ArrayList<Question> result = new ArrayList<>();
        Set<Long> usedIds = new HashSet<>();

        // 1ª tentativa: mesma categoria e dificuldade escolhidas
        List<String> args = new ArrayList<>();
        String filter = buildFilter(config, args);
        addRandomQuestions(resolver, filter, args.toArray(new String[0]), amount, result, usedIds);

        // 2ª tentativa: completa com qualquer questão salva
        if (result.size() < amount) {
            addRandomQuestions(resolver, null, null, amount, result, usedIds);
        }
        return result;
    }

    /** Monta o WHERE com "?" (os valores vão em args, nunca concatenados no SQL). */
    private static String buildFilter(MatchConfig config, List<String> args) {
        List<String> conditions = new ArrayList<>();
        if (!config.getCategoryApiName().isEmpty()) {
            conditions.add(Questions.COLUMN_CATEGORY + " = ?");
            args.add(config.getCategoryApiName());
        }
        if (!config.getDifficulty().isEmpty()) {
            conditions.add(Questions.COLUMN_DIFFICULTY + " = ?");
            args.add(config.getDifficulty());
        }
        // TextUtils.join funciona no minSdk 24 (String.join só existe a partir da API 26)
        return conditions.isEmpty() ? null : TextUtils.join(" AND ", conditions);
    }

    /** Sorteia (ORDER BY RANDOM()) e adiciona em result, sem repetir, até chegar em amount. */
    private static void addRandomQuestions(ContentResolver resolver, String selection, String[] args,
                                           int amount, List<Question> result, Set<Long> usedIds) {
        Uri uri = Questions.CONTENT_URI.buildUpon()
                .appendQueryParameter(TriviaContract.PARAM_LIMIT, String.valueOf(amount))
                .build();
        try (Cursor cursor = resolver.query(uri, null, selection, args, "RANDOM()")) {
            if (cursor == null) return;
            while (cursor.moveToNext() && result.size() < amount) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(Questions._ID));
                if (usedIds.add(id)) { // add devolve false se o id já estava no Set
                    Question question = fromCursor(cursor);
                    if (question != null) result.add(question);
                }
            }
        }
    }

    private static ContentValues toContentValues(Question question, long createdAt) {
        // As alternativas incorretas = todas menos a correta, guardadas como array JSON em texto
        JSONArray incorrect = new JSONArray();
        for (String answer : question.getAnswers()) {
            if (!question.isCorrect(answer)) incorrect.put(answer);
        }
        ContentValues values = new ContentValues();
        values.put(Questions.COLUMN_CATEGORY, question.getCategory());
        values.put(Questions.COLUMN_DIFFICULTY, question.getDifficulty());
        values.put(Questions.COLUMN_TEXT, question.getText());
        values.put(Questions.COLUMN_CORRECT_ANSWER, question.getCorrectAnswer());
        values.put(Questions.COLUMN_INCORRECT_ANSWERS, incorrect.toString());
        values.put(Questions.COLUMN_CREATED_AT, createdAt);
        return values;
    }

    /** Devolve null se a linha estiver corrompida (ela é pulada, sem derrubar o app). */
    private static Question fromCursor(Cursor cursor) {
        try {
            JSONArray incorrectJson = new JSONArray(cursor.getString(
                    cursor.getColumnIndexOrThrow(Questions.COLUMN_INCORRECT_ANSWERS)));
            List<String> incorrect = new ArrayList<>();
            for (int i = 0; i < incorrectJson.length(); i++) {
                incorrect.add(incorrectJson.getString(i));
            }
            // O construtor da Question embaralha as alternativas de novo
            return new Question(
                    cursor.getString(cursor.getColumnIndexOrThrow(Questions.COLUMN_CATEGORY)),
                    cursor.getString(cursor.getColumnIndexOrThrow(Questions.COLUMN_DIFFICULTY)),
                    cursor.getString(cursor.getColumnIndexOrThrow(Questions.COLUMN_TEXT)),
                    cursor.getString(cursor.getColumnIndexOrThrow(Questions.COLUMN_CORRECT_ANSWER)),
                    incorrect);
        } catch (JSONException e) {
            Log.w(TAG, "Questão inválida no cache, ignorada", e);
            return null;
        }
    }
}
