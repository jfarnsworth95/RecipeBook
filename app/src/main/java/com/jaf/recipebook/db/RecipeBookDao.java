package com.jaf.recipebook.db;

import androidx.annotation.Nullable;
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
        public long id;

        @ColumnInfo(name = "name")
        public String name;

        public String getName(){
            return name;
        }

        public long getId(){
            return id;
        }
    }

    /**
     * Search all recipes for matching string
     * @return list of recipes (id & name) with matching string for 'name', 'category', 'source_url' or 'tag'
     */
    @Query( "SELECT DISTINCT recipes.id, recipes.name  " +
            "FROM recipes " +
            "LEFT JOIN ingredients " +
                "ON recipes.id = ingredients.recipe_id " +
            "LEFT JOIN directions " +
                "ON recipes.id = directions.recipe_id " +
            "LEFT JOIN tags " +
                "ON recipes.id = tags.recipe_id " +
            "WHERE " +
                "(:category IS NOT NULL AND recipes.category LIKE '%' || :category || '%') AND " +
                "((:name IS NOT NULL AND recipes.name LIKE '%' || :name || '%') OR " +
                "(:source_url IS NOT NULL AND recipes.source_url LIKE '%' || :source_url || '%') OR " +
                "(:ingredients IS NOT NULL AND ingredients.text LIKE '%' || :ingredients || '%') OR " +
                "(:directions IS NOT NULL AND directions.text LIKE '%' || :directions || '%') OR " +
                "(:tag IS NOT NULL AND tags.tag LIKE '%' || :tag || '%')) " +
            "ORDER BY recipes.name")
    List<BasicRecipeTuple> searchAllForParameter(
            @Nullable String name,
            @Nullable String category,
            @Nullable String source_url,
            @Nullable String ingredients,
            @Nullable String directions,
            @Nullable String tag
        );

    /**
     * Search all recipes for matching string
     * @return list of recipes (id & name) with matching string for 'name', 'category', 'source_url' or 'tag'
     */
    @Query( "SELECT DISTINCT recipes.id, recipes.name  " +
            "FROM recipes " +
            "LEFT JOIN ingredients " +
            "ON recipes.id = ingredients.recipe_id " +
            "LEFT JOIN directions " +
            "ON recipes.id = directions.recipe_id " +
            "LEFT JOIN tags " +
            "ON recipes.id = tags.recipe_id " +
            "WHERE " +
            "(:name IS NOT NULL AND recipes.name LIKE '%' || :name || '%') OR " +
            "(:source_url IS NOT NULL AND recipes.source_url LIKE '%' || :source_url || '%') OR " +
            "(:ingredients IS NOT NULL AND ingredients.text LIKE '%' || :ingredients || '%') OR " +
            "(:directions IS NOT NULL AND directions.text LIKE '%' || :directions || '%') OR " +
            "(:tag IS NOT NULL AND tags.tag LIKE '%' || :tag || '%') " +
            "ORDER BY recipes.name")
    List<BasicRecipeTuple> searchAllForParameter(
            @Nullable String name,
            @Nullable String source_url,
            @Nullable String ingredients,
            @Nullable String directions,
            @Nullable String tag
    );

    @Query("SELECT id, name FROM recipes WHERE category = :category ORDER BY name")
    List<BasicRecipeTuple> getRecipesForCategory(String category);

    @Query( "SELECT DISTINCT id, name FROM recipes ORDER BY name")
    List<BasicRecipeTuple> getAllRecipes();

}
