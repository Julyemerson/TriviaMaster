package com.julyemerson.triviamaster.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.julyemerson.triviamaster.data.TriviaContract.Matches;
import com.julyemerson.triviamaster.data.TriviaContract.Players;
import com.julyemerson.triviamaster.data.TriviaContract.Questions;

/** Cria e versiona o banco SQLite. Só o TriviaProvider usa esta classe. */
class TriviaDbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "trivia.db";
    private static final int DATABASE_VERSION = 1;

    // COLLATE NOCASE: "Ana" e "ana" são o mesmo jogador
    private static final String CREATE_PLAYERS = "CREATE TABLE " + Players.TABLE + " ("
            + Players._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + Players.COLUMN_NAME + " TEXT NOT NULL UNIQUE COLLATE NOCASE)";

    private static final String CREATE_MATCHES = "CREATE TABLE " + Matches.TABLE + " ("
            + Matches._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + Matches.COLUMN_PLAYER_ID + " INTEGER NOT NULL REFERENCES "
            + Players.TABLE + "(" + Players._ID + "), "
            + Matches.COLUMN_DATE + " INTEGER NOT NULL, "
            + Matches.COLUMN_CATEGORY + " TEXT NOT NULL, "
            + Matches.COLUMN_DIFFICULTY + " TEXT NOT NULL, "
            + Matches.COLUMN_CORRECT_COUNT + " INTEGER NOT NULL, "
            + Matches.COLUMN_TOTAL + " INTEGER NOT NULL, "
            + Matches.COLUMN_SCORE + " INTEGER NOT NULL)";

    // VIEW = consulta salva. O histórico lê daqui e já recebe o nome do jogador.
    private static final String CREATE_MATCH_HISTORY_VIEW = "CREATE VIEW " + Matches.VIEW_WITH_PLAYER
            + " AS SELECT m.*, p." + Players.COLUMN_NAME + " AS " + Matches.COLUMN_PLAYER_NAME
            + " FROM " + Matches.TABLE + " m JOIN " + Players.TABLE + " p"
            + " ON m." + Matches.COLUMN_PLAYER_ID + " = p." + Players._ID;

    // UNIQUE no texto: a mesma questão baixada duas vezes não duplica o cache
    private static final String CREATE_QUESTIONS = "CREATE TABLE " + Questions.TABLE + " ("
            + Questions._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + Questions.COLUMN_CATEGORY + " TEXT NOT NULL, "
            + Questions.COLUMN_DIFFICULTY + " TEXT NOT NULL, "
            + Questions.COLUMN_TEXT + " TEXT NOT NULL UNIQUE, "
            + Questions.COLUMN_CORRECT_ANSWER + " TEXT NOT NULL, "
            + Questions.COLUMN_INCORRECT_ANSWERS + " TEXT NOT NULL, "
            + Questions.COLUMN_CREATED_AT + " INTEGER NOT NULL)";

    TriviaDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true); // SQLite vem com FKs desligadas por padrão
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_PLAYERS);
        db.execSQL(CREATE_MATCHES);
        db.execSQL(CREATE_MATCH_HISTORY_VIEW);
        db.execSQL(CREATE_QUESTIONS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Estratégia simples para projeto acadêmico: recria tudo ao mudar a versão
        db.execSQL("DROP VIEW IF EXISTS " + Matches.VIEW_WITH_PLAYER);
        db.execSQL("DROP TABLE IF EXISTS " + Matches.TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + Players.TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + Questions.TABLE);
        onCreate(db);
    }
}
