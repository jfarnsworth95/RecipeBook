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

    private TagViewHolder(View itemView) {
        super(itemView);
        tagItemView = itemView.findViewById(R.id.tag_chip);
    }

    public void bind(String text) {
        tagItemView.setText(text);
    }

    static TagViewHolder create(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.edit_chip, parent, false);
        return new TagViewHolder(view);
    }
}
