package com.jaf.recipebook.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.jaf.recipebook.R;

public class TagViewHolder extends RecyclerView.ViewHolder {
    private final TextView tagItemView;

    private TagViewHolder(View itemView, boolean isEdit) {
        super(itemView);
        tagItemView = itemView.findViewById(isEdit ? R.id.tag_chip : R.id.tag_view_chip);
    }

    public void bind(String text) {
        tagItemView.setText(text);
    }

    static TagViewHolder create(ViewGroup parent, boolean isEdit) {
        int layoutInt = isEdit ? R.layout.chip_edit : R.layout.chip_view;
        View view = LayoutInflater.from(parent.getContext())
                .inflate(layoutInt, parent, false);
        return new TagViewHolder(view, isEdit);
    }
}
