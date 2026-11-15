package com.julyemerson.triviamaster.model;

import java.util.ArrayList;
import java.util.List;

/** Partidas fixas para a HistoryActivity na Fase 1 (na Fase 2 virão do ContentProvider). */
public final class MockMatches {

    private static final long HOUR_MS = 60 * 60 * 1000L;

    private MockMatches() { }

    /** Já em ordem: mais recente primeiro. */
    public static List<MatchResult> get() {
        long now = System.currentTimeMillis();
        List<MatchResult> list = new ArrayList<>();
        list.add(new MatchResult("Ana", now - HOUR_MS, "Computação", "Difícil", 8, 10, 312));
        list.add(new MatchResult("Bruno", now - 5 * HOUR_MS, "Geografia", "Fácil", 4, 5, 68));
        list.add(new MatchResult("Ana", now - 26 * HOUR_MS, "Qualquer categoria", "Média", 11, 15, 290));
        list.add(new MatchResult("Carla", now - 50 * HOUR_MS, "Filmes", "Qualquer dificuldade", 3, 20, 61));
        return list;
    }
}
