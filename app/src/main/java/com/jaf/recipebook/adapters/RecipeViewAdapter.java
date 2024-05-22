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
    View.OnLongClickListener recipeLongClickListener;

    public RecipeViewAdapter(
            @NonNull DiffUtil.ItemCallback<BasicRecipeTuple> diffCallback,
            View.OnClickListener recipeClickListener,
            View.OnLongClickListener recipeLongClickListener) {
        super(diffCallback);
        this.recipeClickListener = recipeClickListener;
        this.recipeLongClickListener = recipeLongClickListener;
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
        holder.itemView.setOnLongClickListener(recipeLongClickListener);
    }

    public static class RecipeDiff extends DiffUtil.ItemCallback<BasicRecipeTuple> {

        @Override
        public boolean areItemsTheSame(@NonNull BasicRecipeTuple oldItem, @NonNull BasicRecipeTuple newItem) {
            return oldItem.getId() == newItem.getId() && oldItem.getName().equals(newItem.getName());
        }

        @Override
        public boolean areContentsTheSame(@NonNull BasicRecipeTuple oldItem, @NonNull BasicRecipeTuple newItem) {
            return oldItem.getId() == newItem.getId() && oldItem.getName().equals(newItem.getName());
        }
    }
}

