package com.julyemerson.triviamaster.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.julyemerson.triviamaster.data.TriviaContract.Matches;
import com.julyemerson.triviamaster.data.TriviaContract.Players;
import com.julyemerson.triviamaster.data.TriviaContract.Questions;

/**
 * ÚNICO ponto de acesso ao banco. O resto do app usa o ContentResolver com as URIs do contrato.
 * Os métodos rodam na thread de quem chamou: por isso o app sempre chama a partir de um Executor.
 */
public class TriviaProvider extends ContentProvider {

    private static final int PLAYERS = 100;
    private static final int MATCHES = 200;
    private static final int QUESTIONS = 300;

    // Traduz a URI recebida em um código numérico para o switch
    private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        URI_MATCHER.addURI(TriviaContract.AUTHORITY, Players.TABLE, PLAYERS);
        URI_MATCHER.addURI(TriviaContract.AUTHORITY, Matches.TABLE, MATCHES);
        URI_MATCHER.addURI(TriviaContract.AUTHORITY, Questions.TABLE, QUESTIONS);
    }

    private TriviaDbHelper dbHelper;

    @Override
    public boolean onCreate() {
        // Só cria o helper; o arquivo do banco é aberto na primeira consulta
        dbHelper = new TriviaDbHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
                        @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.query(tableForQuery(uri), projection, selection, selectionArgs,
                null, null, sortOrder);
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long id;
        switch (URI_MATCHER.match(uri)) {
            case PLAYERS:
                id = insertOrGetPlayer(db, values);
                break;
            case MATCHES:
                id = db.insertOrThrow(Matches.TABLE, null, values);
                break;
            case QUESTIONS:
                // Questão repetida (UNIQUE) é simplesmente ignorada
                id = db.insertWithOnConflict(Questions.TABLE, null, values,
                        SQLiteDatabase.CONFLICT_IGNORE);
                if (id == -1) return null;
                break;
            default:
                throw unknownUri(uri);
        }
        return ContentUris.withAppendedId(uri, id);
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        return dbHelper.getWritableDatabase().delete(tableForWrite(uri), selection, selectionArgs);
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection,
                      @Nullable String[] selectionArgs) {
        // O app não precisa alterar registros: partidas e questões só são criadas ou apagadas
        throw new UnsupportedOperationException("update não suportado: " + uri);
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        // Tipo MIME padrão para "lista de registros"
        return "vnd.android.cursor.dir/vnd." + TriviaContract.AUTHORITY + "." + tableForWrite(uri);
    }

    /** Jogador é identificado pelo nome: se já existe, devolve o id dele em vez de duplicar. */
    private long insertOrGetPlayer(SQLiteDatabase db, ContentValues values) {
        String name = values == null ? null : values.getAsString(Players.COLUMN_NAME);
        try (Cursor cursor = db.query(Players.TABLE, new String[]{Players._ID},
                Players.COLUMN_NAME + " = ?", new String[]{name}, null, null, null)) {
            if (cursor.moveToFirst()) return cursor.getLong(0);
        }
        return db.insertOrThrow(Players.TABLE, null, values);
    }

    /** Consultas de partidas leem da VIEW (já traz o nome do jogador). */
    private String tableForQuery(Uri uri) {
        if (URI_MATCHER.match(uri) == MATCHES) return Matches.VIEW_WITH_PLAYER;
        return tableForWrite(uri);
    }

    private String tableForWrite(Uri uri) {
        switch (URI_MATCHER.match(uri)) {
            case PLAYERS: return Players.TABLE;
            case MATCHES: return Matches.TABLE;
            case QUESTIONS: return Questions.TABLE;
            default: throw unknownUri(uri);
        }
    }

    private static IllegalArgumentException unknownUri(Uri uri) {
        return new IllegalArgumentException("URI desconhecida: " + uri);
    }
}
