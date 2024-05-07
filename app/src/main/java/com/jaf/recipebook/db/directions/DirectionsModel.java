package com.jaf.recipebook.db.directions;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;

@Entity(tableName = "directions",
        primaryKeys = {"recipe_id"},
        indices = {@Index(value = {"recipe_id"}, unique = true)})
public class DirectionsModel {

    @NonNull
    private long recipe_id;
    @NonNull
    private String text;

    public DirectionsModel(long recipe_id, String text){
        this.recipe_id = recipe_id;
        this.text = text;
    }

    public long getRecipe_id() {
        return recipe_id;
    }
    public void setRecipe_id(long recipe_id) {
        this.recipe_id = recipe_id;
    }

    public String getText() {
        return text;
    }
    public void setText(String text) {
        this.text = text;
    }
}
