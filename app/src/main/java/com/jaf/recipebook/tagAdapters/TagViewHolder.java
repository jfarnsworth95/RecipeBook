package com.jaf.recipebook.tagAdapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.jaf.recipebook.R;

public class TagViewHolder extends RecyclerView.ViewHolder {
    private final TextView tagItemView;
    private static final String TAG = "JAF-TagViewHolder";

    private TagViewHolder(View itemView) {
        super(itemView);
        Log.i(TAG, "INIT");
        tagItemView = itemView.findViewById(R.id.tag_chip);
    }

    public void bind(String text) {
        Log.i(TAG, "BIND");
        tagItemView.setText(text);
    }

    static TagViewHolder create(ViewGroup parent) {
        Log.i(TAG, "CREATE");
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.chip, parent, false);
        return new TagViewHolder(view);
    }
}
