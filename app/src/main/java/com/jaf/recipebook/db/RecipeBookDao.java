package com.jaf.recipebook.db;

import androidx.lifecycle.LiveData;
import androidx.room.ColumnInfo;
import androidx.room.Dao;
import androidx.room.Query;

import com.jaf.recipebook.db.recipes.RecipesModel;

import java.util.List;

/**
 * Multi-table Queries
 */
@Dao
public interface RecipeBookDao {

    // Search All Return
    class BasicRecipeTuple {
        @ColumnInfo(name = "id")
        public int id;

        @ColumnInfo(name = "name")
        public String name;
    }

    /**
     * Search all recipes for matching string
     * @param search string to search for
     * @return list of recipes (id & name) with matching string for 'name', 'category', 'source_url' or 'tag'
     */
    @Query( "SELECT DISTINCT recipes.id, recipes.name  " +
            "FROM recipes " +
            "LEFT JOIN ingredients " +
                "ON recipes.id = ingredients.recipe_id " +
            "LEFT JOIN tags " +
                "ON recipes.id = tags.recipe_id " +
            "WHERE " +
                "recipes.name LIKE '%' || :search || '%' OR " +
                "recipes.category LIKE '%' || :search || '%' OR " +
                "recipes.source_url LIKE '%' || :search || '%' OR " +
                "ingredients.text LIKE '%' || :search || '%' OR " +
                "tags.tag LIKE '%' || :search || '%' " +
            "ORDER BY recipes.name")
    LiveData<List<BasicRecipeTuple>> searchAllForParameter(String search);

    // Get Recipe Data

}
