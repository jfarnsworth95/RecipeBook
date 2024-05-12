package com.jaf.recipebook.adapters;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

import com.jaf.recipebook.db.tags.TagsModel;

public class TagViewAdapter extends ListAdapter<TagsModel, TagViewHolder> {
    View.OnClickListener tagClickListener;
    boolean isEdit;
    private static final String TAG = "JAF-TagViewAdapter";

    public TagViewAdapter(@NonNull DiffUtil.ItemCallback<TagsModel> diffCallback, View.OnClickListener tagClickListener, boolean isEdit) {
        super(diffCallback);
        this.tagClickListener = tagClickListener;
        this.isEdit = isEdit;
    }

    @Override
    public TagViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return TagViewHolder.create(parent, isEdit);
    }

    @Override
    public void onBindViewHolder(TagViewHolder holder, int position) {
        TagsModel current = getItem(position);
        holder.bind(current.getTag());
        holder.itemView.setOnClickListener(tagClickListener);
    }

    public static class TagDiff extends DiffUtil.ItemCallback<TagsModel> {

        @Override
        public boolean areItemsTheSame(@NonNull TagsModel oldItem, @NonNull TagsModel newItem) {
            return oldItem == newItem;
        }

        @Override
        public boolean areContentsTheSame(@NonNull TagsModel oldItem, @NonNull TagsModel newItem) {
            return oldItem.getTag().equals(newItem.getTag());
        }
    }
}

