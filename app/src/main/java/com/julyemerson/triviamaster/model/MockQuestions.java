package com.julyemerson.triviamaster.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Questões fixas usadas só na Fase 1, antes de ligar a API.
 * Algumas têm entidades HTML (&quot; &#039;) de propósito, como a API devolve.
 */
public final class MockQuestions {

    private MockQuestions() { } // classe utilitária, não instanciar

    public static ArrayList<Question> get() {
        ArrayList<Question> list = new ArrayList<>();
        list.add(new Question("General Knowledge", Question.DIFFICULTY_EASY,
                "What is the capital of Brazil?",
                "Brasília", Arrays.asList("Rio de Janeiro", "São Paulo", "Salvador")));
        list.add(new Question("Entertainment: Film", Question.DIFFICULTY_MEDIUM,
                "Which movie features the quote &quot;May the Force be with you&quot;?",
                "Star Wars", Arrays.asList("Star Trek", "Dune", "Avatar")));
        list.add(new Question("Science: Computers", Question.DIFFICULTY_EASY,
                "What does &quot;CPU&quot; stand for?",
                "Central Processing Unit",
                Arrays.asList("Computer Personal Unit", "Central Program Utility", "Core Power Unit")));
        list.add(new Question("Geography", Question.DIFFICULTY_HARD,
                "Which is the world&#039;s longest river?",
                "Nile", Arrays.asList("Amazon", "Yangtze", "Mississippi")));
        list.add(new Question("Science & Nature", Question.DIFFICULTY_MEDIUM,
                "What is the chemical symbol for gold?",
                "Au", Arrays.asList("Ag", "Gd", "Go")));
        return list;
    }

    /** Versão com quantidade: repete a lista se pedirem mais do que 5. */
    public static ArrayList<Question> get(int amount) {
        List<Question> base = get();
        ArrayList<Question> result = new ArrayList<>();
        for (int i = 0; i < amount; i++) {
            result.add(base.get(i % base.size()));
        }
        return result;
    }
}
