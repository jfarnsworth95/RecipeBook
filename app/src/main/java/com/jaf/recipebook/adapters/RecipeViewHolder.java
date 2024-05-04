package com.jaf.recipebook.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.jaf.recipebook.R;

public class RecipeViewHolder extends RecyclerView.ViewHolder {
    private final TextView recipeItemNameTextView;
    private final TextView recipeItemIdTextView;

    private RecipeViewHolder(View itemView) {
        super(itemView);
        recipeItemNameTextView = itemView.findViewById(R.id.frag_recipe_list_row_text_view);
        recipeItemIdTextView = itemView.findViewById(R.id.frag_recipe_list_row_recipe_id);
    }

    public void bind(String text, Long id) {
        recipeItemNameTextView.setText(text);
        recipeItemIdTextView.setText(id.toString());
    }

    static RecipeViewHolder create(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_recipe_list_row, parent, false);
        return new RecipeViewHolder(view);
    }
}
