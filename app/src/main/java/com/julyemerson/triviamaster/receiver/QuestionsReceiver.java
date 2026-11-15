package com.julyemerson.triviamaster.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.julyemerson.triviamaster.R;
import com.julyemerson.triviamaster.model.MatchConfig;
import com.julyemerson.triviamaster.model.Question;
import com.julyemerson.triviamaster.service.QuestionFetchService;

import java.util.ArrayList;

/**
 * Recebe o aviso do QuestionFetchService e repassa para quem implementa o Listener.
 * Não conhece a MainActivity: só a interface. onReceive roda na main thread.
 */
public class QuestionsReceiver extends BroadcastReceiver {

    public interface Listener {
        /** @param offlineReasonRes 0 = veio da API; senão, motivo de estar usando o cache */
        void onQuestionsReady(MatchConfig config, ArrayList<Question> questions, int offlineReasonRes);

        void onQuestionsFailed(int messageRes);
    }

    private final Listener listener;

    public QuestionsReceiver(Listener listener) {
        this.listener = listener;
    }

    /** As duas ações que este receiver escuta. */
    public static IntentFilter createFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(QuestionFetchService.ACTION_QUESTIONS_READY);
        filter.addAction(QuestionFetchService.ACTION_QUESTIONS_FAILED);
        return filter;
    }

    @Override
    @SuppressWarnings({"unchecked", "deprecation"})
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (QuestionFetchService.ACTION_QUESTIONS_READY.equals(action)) {
            MatchConfig config = (MatchConfig) intent.getSerializableExtra(QuestionFetchService.EXTRA_CONFIG);
            ArrayList<Question> questions =
                    (ArrayList<Question>) intent.getSerializableExtra(QuestionFetchService.EXTRA_QUESTIONS);
            if (config == null || questions == null || questions.isEmpty()) {
                listener.onQuestionsFailed(R.string.error_unexpected);
            } else {
                listener.onQuestionsReady(config, questions,
                        intent.getIntExtra(QuestionFetchService.EXTRA_OFFLINE_REASON, 0));
            }

        } else if (QuestionFetchService.ACTION_QUESTIONS_FAILED.equals(action)) {
            listener.onQuestionsFailed(intent.getIntExtra(
                    QuestionFetchService.EXTRA_ERROR_MESSAGE, R.string.error_unexpected));
        }
    }
}
