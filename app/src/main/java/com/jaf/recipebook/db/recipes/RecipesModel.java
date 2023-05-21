package com.jaf.recipebook.db.recipes;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import javax.annotation.Nullable;

@Entity(tableName = "recipes")
public class RecipesModel {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private String name;
    private String category;
    private Float servings;
    private String source_url;

    public RecipesModel(String name, @Nullable String category,
                        @Nullable Float servings, @Nullable String source_url){
        this.name = name;
        this.category = category;
        this.servings = servings;
        this.source_url = source_url;
    }

    public long getId() {
        return id;
    }
    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }
    public void setCategory(String category) {
        this.category = category;
    }

    public Float getServings() {
        return servings;
    }
    public void setServings(Float servings) {
        this.servings = servings;
    }

    public String getSource_url() {
        return source_url;
    }
    public void setSource_url(String source_url) {
        this.source_url = source_url;
    }
}
