package com.julyemerson.triviamaster.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.julyemerson.triviamaster.R;
import com.julyemerson.triviamaster.model.MatchResult;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

/** Transforma cada MatchResult em uma linha (item_match.xml) do ListView do histórico. */
class MatchAdapter extends ArrayAdapter<MatchResult> {

    // Formato de data/hora curto do idioma do aparelho (ex.: 15/11/26 09:50)
    private final DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);

    MatchAdapter(Context context, List<MatchResult> matches) {
        super(context, 0, matches);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        // Reaproveita a linha que saiu da tela (convertView) em vez de inflar uma nova
        View row = convertView != null ? convertView
                : LayoutInflater.from(getContext()).inflate(R.layout.item_match, parent, false);

        MatchResult match = getItem(position);
        if (match == null) return row;

        Context context = getContext();
        ((TextView) row.findViewById(R.id.textMatchPlayer)).setText(match.getPlayerName());
        ((TextView) row.findViewById(R.id.textMatchScore))
                .setText(context.getString(R.string.history_score, match.getScore()));
        ((TextView) row.findViewById(R.id.textMatchDetails)).setText(context.getString(
                R.string.history_details, match.getCategory(), match.getDifficulty(),
                match.getCorrectCount(), match.getTotalQuestions()));
        ((TextView) row.findViewById(R.id.textMatchDate))
                .setText(dateFormat.format(new Date(match.getDateMillis())));
        return row;
    }
}
