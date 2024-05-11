package com.jaf.recipebook.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textview.MaterialTextView;
import com.jaf.recipebook.R;

public class CategoryViewHolder extends RecyclerView.ViewHolder {
    private final MaterialTextView categoryTextView;

    private CategoryViewHolder(View itemView) {
        super(itemView);
        categoryTextView = itemView.findViewById(R.id.category_item_text);
    }

    public void bind(String text) {
        categoryTextView.setText(text);
    }

    static CategoryViewHolder create(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_reorderable_category, parent, false);
        return new CategoryViewHolder(view);
    }
}
