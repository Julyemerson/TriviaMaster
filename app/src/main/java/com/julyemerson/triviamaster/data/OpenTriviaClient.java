package com.julyemerson.triviamaster.data;

import android.net.Uri;
import android.text.Html;

import com.julyemerson.triviamaster.data.TriviaApiException.Reason;
import com.julyemerson.triviamaster.model.Question;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Cliente REST do Open Trivia DB: GET + parse do JSON + decodificação HTML.
 * Faz rede de forma bloqueante: NUNCA chamar na main thread.
 */
public final class OpenTriviaClient {

    private static final String BASE_URL = "https://opentdb.com/api.php";
    private static final int TIMEOUT_MS = 8000;
    private static final String TYPE_MULTIPLE = "multiple";

    // Valores do campo "response_code" da API
    private static final int CODE_SUCCESS = 0;
    private static final int CODE_NO_RESULTS = 1;
    private static final int CODE_INVALID_PARAMETER = 2;
    private static final int CODE_RATE_LIMIT = 5;
    private static final int HTTP_TOO_MANY_REQUESTS = 429;

    private OpenTriviaClient() { }

    /**
     * @param categoryId 0 = qualquer categoria
     * @param difficulty "" = qualquer, senão easy/medium/hard
     */
    public static ArrayList<Question> fetchQuestions(int amount, int categoryId, String difficulty)
            throws TriviaApiException {
        String json = httpGet(buildUrl(amount, categoryId, difficulty));
        return parseQuestions(json);
    }

    /** Ex.: https://opentdb.com/api.php?amount=10&type=multiple&category=18&difficulty=hard */
    static String buildUrl(int amount, int categoryId, String difficulty) {
        Uri.Builder builder = Uri.parse(BASE_URL).buildUpon()
                .appendQueryParameter("amount", String.valueOf(amount))
                .appendQueryParameter("type", TYPE_MULTIPLE);
        // Parâmetro omitido = "qualquer" para a API
        if (categoryId > 0) {
            builder.appendQueryParameter("category", String.valueOf(categoryId));
        }
        if (difficulty != null && !difficulty.isEmpty()) {
            builder.appendQueryParameter("difficulty", difficulty);
        }
        return builder.build().toString();
    }

    private static String httpGet(String url) throws TriviaApiException {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);

            int httpCode = connection.getResponseCode();
            if (httpCode == HTTP_TOO_MANY_REQUESTS) {
                throw new TriviaApiException(Reason.RATE_LIMIT, "HTTP 429");
            }
            if (httpCode != HttpURLConnection.HTTP_OK) {
                throw new TriviaApiException(Reason.SERVER_UNAVAILABLE, "HTTP " + httpCode);
            }
            return readBody(connection);

        // As subclasses vêm antes de IOException, senão nunca seriam alcançadas
        } catch (SocketTimeoutException e) {
            throw new TriviaApiException(Reason.TIMEOUT, e);
        } catch (UnknownHostException e) {
            throw new TriviaApiException(Reason.NO_CONNECTION, e); // DNS falhou = sem internet
        } catch (IOException e) {
            throw new TriviaApiException(Reason.NO_CONNECTION, e);
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static String readBody(HttpURLConnection connection) throws IOException {
        StringBuilder body = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
        }
        return body.toString();
    }

    /** { "response_code": 0, "results": [ { "category", "difficulty", "question", ... } ] } */
    static ArrayList<Question> parseQuestions(String json) throws TriviaApiException {
        try {
            JSONObject root = new JSONObject(json);
            checkResponseCode(root.getInt("response_code"));

            JSONArray results = root.getJSONArray("results");
            ArrayList<Question> questions = new ArrayList<>();
            for (int i = 0; i < results.length(); i++) {
                questions.add(parseQuestion(results.getJSONObject(i)));
            }
            return questions;
        } catch (JSONException e) {
            throw new TriviaApiException(Reason.INVALID_RESPONSE, e);
        }
    }

    private static Question parseQuestion(JSONObject item) throws JSONException {
        JSONArray incorrectJson = item.getJSONArray("incorrect_answers");
        List<String> incorrect = new ArrayList<>();
        for (int i = 0; i < incorrectJson.length(); i++) {
            incorrect.add(decodeHtml(incorrectJson.getString(i)));
        }
        // Decodifica tudo AQUI, na entrada: o resto do app só vê texto limpo
        return new Question(
                decodeHtml(item.getString("category")),
                item.getString("difficulty"),
                decodeHtml(item.getString("question")),
                decodeHtml(item.getString("correct_answer")),
                incorrect);
    }

    private static void checkResponseCode(int code) throws TriviaApiException {
        switch (code) {
            case CODE_SUCCESS:
                return;
            case CODE_NO_RESULTS:
                throw new TriviaApiException(Reason.NOT_ENOUGH_QUESTIONS, "response_code 1");
            case CODE_INVALID_PARAMETER:
                throw new TriviaApiException(Reason.INVALID_PARAMETER, "response_code 2");
            case CODE_RATE_LIMIT:
                throw new TriviaApiException(Reason.RATE_LIMIT, "response_code 5");
            default: // 3 e 4 são de token de sessão, que não usamos
                throw new TriviaApiException(Reason.INVALID_RESPONSE, "response_code " + code);
        }
    }

    /** &quot; → "   &#039; → '   &amp; → & */
    static String decodeHtml(String text) {
        return Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY).toString();
    }
}
