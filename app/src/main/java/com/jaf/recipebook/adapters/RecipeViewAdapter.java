package com.jaf.recipebook.adapters;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

import com.jaf.recipebook.db.RecipeBookDao.BasicRecipeTuple;

public class RecipeViewAdapter extends ListAdapter<BasicRecipeTuple, RecipeViewHolder> {
    private static final String TAG = "JAF-RecipeViewAdapter";
    View.OnClickListener recipeClickListener;

    public RecipeViewAdapter(@NonNull DiffUtil.ItemCallback<BasicRecipeTuple> diffCallback, View.OnClickListener recipeClickListener) {
        super(diffCallback);
        this.recipeClickListener = recipeClickListener;
    }

    @Override
    public RecipeViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return RecipeViewHolder.create(parent);
    }

    @Override
    public void onBindViewHolder(RecipeViewHolder holder, int position) {
        BasicRecipeTuple current = getItem(position);
        holder.bind(current.getName(), current.getId());
        holder.itemView.setOnClickListener(recipeClickListener);
    }

    public static class RecipeDiff extends DiffUtil.ItemCallback<BasicRecipeTuple> {

        @Override
        public boolean areItemsTheSame(@NonNull BasicRecipeTuple oldItem, @NonNull BasicRecipeTuple newItem) {
            return oldItem == newItem;
        }

        @Override
        public boolean areContentsTheSame(@NonNull BasicRecipeTuple oldItem, @NonNull BasicRecipeTuple newItem) {
            return oldItem.getId() == newItem.getId() && oldItem.getName().equals(newItem.getName());
        }
    }
}
