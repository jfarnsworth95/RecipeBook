package com.jaf.recipebook.db.tags;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;

@Entity(tableName = "tags",
        primaryKeys = {"recipe_id", "tag"},
        indices = {@Index(value = {"recipe_id", "tag"}, unique = true)})
public class TagsModel {

    @NonNull
    private long recipe_id;
    @NonNull
    private String tag;

    public TagsModel(long recipe_id, String tag){
        this.recipe_id = recipe_id;
        this.tag = tag;
    }

    public long getRecipe_id() {
        return recipe_id;
    }
    public void setRecipe_id(long recipe_id) {
        this.recipe_id = recipe_id;
    }

    public String getTag() {
        return tag;
    }
    public void setTag(String tag) {
        this.tag = tag;
    }
}
