package com.jaf.recipebook.adapters;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

public class CategoryViewAdapter extends ListAdapter<String, CategoryViewHolder> {
    private static final String TAG = "JAF-CategoryViewAdapter";

    public CategoryViewAdapter(
            @NonNull DiffUtil.ItemCallback<String> diffCallback) {
        super(diffCallback);
    }

    @Override
    public CategoryViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return CategoryViewHolder.create(parent);
    }

    @Override
    public void onBindViewHolder(CategoryViewHolder holder, int position) {
        String current = getItem(position);
        holder.bind(current);
    }

    public static class CategoryDiff extends DiffUtil.ItemCallback<String> {

        @Override
        public boolean areItemsTheSame(@NonNull String oldItem, @NonNull String newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areContentsTheSame(@NonNull String oldItem, @NonNull String newItem) {
            return oldItem.equals(newItem);
        }
    }
}
