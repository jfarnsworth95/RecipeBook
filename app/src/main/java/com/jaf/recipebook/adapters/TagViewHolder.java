package com.jaf.recipebook.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.jaf.recipebook.R;

public class TagViewHolder extends RecyclerView.ViewHolder {
    private final TextView tagItemView;

    private TagViewHolder(View itemView, boolean isViewOnly) {
        super(itemView);
        int idInt;
        if (isViewOnly){
            idInt = R.id.tag_view_chip;
        } else {
            idInt = R.id.tag_chip;
        }
        tagItemView = itemView.findViewById(idInt);
    }

    public void bind(String text) {
        tagItemView.setText(text);
    }

    static TagViewHolder create(ViewGroup parent, boolean isViewOnly) {
        int layoutInt;
        if (isViewOnly){
            layoutInt = R.layout.chip_view;
        } else {
            layoutInt = R.layout.chip_edit;
        }
        View view = LayoutInflater.from(parent.getContext())
                .inflate(layoutInt, parent, false);
        return new TagViewHolder(view, isViewOnly);
    }
}
