package com.julyemerson.triviamaster.data;

import androidx.annotation.StringRes;

import com.julyemerson.triviamaster.R;

/** Falha ao buscar questões. O motivo já sabe qual mensagem amigável mostrar ao usuário. */
public class TriviaApiException extends Exception {

    public enum Reason {
        NO_CONNECTION(R.string.error_no_connection),
        TIMEOUT(R.string.error_timeout),
        SERVER_UNAVAILABLE(R.string.error_server_unavailable),
        NOT_ENOUGH_QUESTIONS(R.string.error_not_enough_questions), // response_code 1
        INVALID_PARAMETER(R.string.error_invalid_parameter),       // response_code 2
        RATE_LIMIT(R.string.error_rate_limit),                     // response_code 5
        INVALID_RESPONSE(R.string.error_invalid_response);         // JSON quebrado ou código inesperado

        @StringRes
        public final int messageRes;

        Reason(@StringRes int messageRes) {
            this.messageRes = messageRes;
        }
    }

    private final Reason reason;

    public TriviaApiException(Reason reason, String detail) {
        super(reason + ": " + detail); // texto técnico, só para o Log
        this.reason = reason;
    }

    public TriviaApiException(Reason reason, Throwable cause) {
        super(reason.toString(), cause);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
