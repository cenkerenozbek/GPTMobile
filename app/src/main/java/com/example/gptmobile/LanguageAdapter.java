package com.example.gptmobile;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class LanguageAdapter extends ArrayAdapter<String> {
    private final Context context;
    private final String[] languages;
    private final int[] flagIcons;

    public LanguageAdapter(Context context, String[] languages, int[] flagIcons) {
        super(context, R.layout.language_item, languages);
        this.context = context;
        this.languages = languages;
        this.flagIcons = flagIcons;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.language_item, parent, false);

        TextView textView = rowView.findViewById(R.id.language_name);
        ImageView imageView = rowView.findViewById(R.id.flag_icon);

        textView.setText(languages[position]);
        imageView.setImageResource(flagIcons[position]);

        return rowView;
    }
}
