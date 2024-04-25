package com.jaf.recipebook.tagAdapters;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

import com.jaf.recipebook.db.tags.TagsModel;

public class TagViewAdapter extends ListAdapter<TagsModel, TagViewHolder> {
    View.OnClickListener tagClickListener;
    private static final String TAG = "JAF-TagViewAdapter";

    public TagViewAdapter(@NonNull DiffUtil.ItemCallback<TagsModel> diffCallback, View.OnClickListener tagClickListener) {
        super(diffCallback);
        Log.i(TAG, "INIT");
        this.tagClickListener = tagClickListener;
    }

    @Override
    public TagViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Log.i(TAG, "onCreateViewHolder");
        return TagViewHolder.create(parent);
    }

    @Override
    public void onBindViewHolder(TagViewHolder holder, int position) {
        Log.i(TAG, "onBindViewHolder");
        TagsModel current = getItem(position);
        holder.bind(current.getTag());
        holder.itemView.setOnClickListener(tagClickListener);
    }

    public static class TagDiff extends DiffUtil.ItemCallback<TagsModel> {

        @Override
        public boolean areItemsTheSame(@NonNull TagsModel oldItem, @NonNull TagsModel newItem) {
            Log.i(TAG, "areItemsTheSame");
            Log.i(TAG, String.valueOf(oldItem == newItem));
            return oldItem == newItem;
        }

        @Override
        public boolean areContentsTheSame(@NonNull TagsModel oldItem, @NonNull TagsModel newItem) {
            Log.i(TAG, "areContentsTheSame");
            Log.i(TAG, String.valueOf(oldItem.getTag().equals(newItem.getTag())));
            return oldItem.getTag().equals(newItem.getTag());
        }
    }
}

