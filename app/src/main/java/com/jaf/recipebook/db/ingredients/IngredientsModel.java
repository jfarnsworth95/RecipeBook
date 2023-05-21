package com.jaf.recipebook.db.ingredients;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;

@Entity(tableName = "ingredients",
        primaryKeys = {"recipe_id", "order_id"},
        indices = {@Index(value = {"recipe_id", "order_id"}, unique = true)})
public class IngredientsModel {

    @NonNull
    private long recipe_id;
    @NonNull
    private int order_id;
    @NonNull
    private String text;

    public IngredientsModel(long recipe_id, int order_id, String text){
        this.recipe_id = recipe_id;
        this.order_id = order_id;
        this.text = text;
    }

    public long getRecipe_id() {
        return recipe_id;
    }
    public void setRecipe_id(long recipe_id) {
        this.recipe_id = recipe_id;
    }

    public int getOrder_id() {
        return order_id;
    }
    public void setOrder_id(int order_id) {
        this.order_id = order_id;
    }

    public String getText() {
        return text;
    }
    public void setText(String text) {
        this.text = text;
    }
}
