package com.julyemerson.triviamaster.data;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Contrato do banco: AUTHORITY, URIs e nomes de colunas.
 * Quem usa o ContentResolver só conhece estas constantes, nunca o SQL.
 */
public final class TriviaContract {

    /** Deve ser igual ao android:authorities do <provider> no Manifest. */
    public static final String AUTHORITY = "com.julyemerson.triviamaster.provider";
    public static final Uri BASE_URI = Uri.parse("content://" + AUTHORITY);

    private TriviaContract() { }

    /** content://.../players  (BaseColumns traz a coluna _ID) */
    public static final class Players implements BaseColumns {
        public static final String TABLE = "players";
        public static final Uri CONTENT_URI = BASE_URI.buildUpon().appendPath(TABLE).build();

        public static final String COLUMN_NAME = "name";

        private Players() { }
    }

    /** content://.../matches */
    public static final class Matches implements BaseColumns {
        public static final String TABLE = "matches";
        /** VIEW usada nas consultas: partida + nome do jogador (JOIN com players). */
        public static final String VIEW_WITH_PLAYER = "match_history";
        public static final Uri CONTENT_URI = BASE_URI.buildUpon().appendPath(TABLE).build();

        public static final String COLUMN_PLAYER_ID = "player_id";
        public static final String COLUMN_DATE = "date";              // System.currentTimeMillis()
        public static final String COLUMN_CATEGORY = "category";
        public static final String COLUMN_DIFFICULTY = "difficulty";
        public static final String COLUMN_CORRECT_COUNT = "correct_count";
        public static final String COLUMN_TOTAL = "total_questions";
        public static final String COLUMN_SCORE = "score";
        /** Só existe no resultado da consulta (vem da VIEW). */
        public static final String COLUMN_PLAYER_NAME = "player_name";

        private Matches() { }
    }

    /** content://.../questions — cache para jogar offline (usado na Fase 3). */
    public static final class Questions implements BaseColumns {
        public static final String TABLE = "questions";
        public static final Uri CONTENT_URI = BASE_URI.buildUpon().appendPath(TABLE).build();

        public static final String COLUMN_CATEGORY = "category";
        public static final String COLUMN_DIFFICULTY = "difficulty";
        public static final String COLUMN_TEXT = "question";
        public static final String COLUMN_CORRECT_ANSWER = "correct_answer";
        public static final String COLUMN_INCORRECT_ANSWERS = "incorrect_answers"; // array JSON em texto
        public static final String COLUMN_CREATED_AT = "created_at";

        private Questions() { }
    }
}
