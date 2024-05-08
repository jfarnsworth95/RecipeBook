package com.jaf.recipebook.db.recipes;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Relation;
import androidx.room.Update;

import java.util.List;
import java.util.UUID;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

@Dao
public interface RecipeDao {

    @Insert
    long insertRecipe(RecipesModel model);
    @Update
    int updateRecipe(RecipesModel model);
    @Delete
    int deleteRecipe(RecipesModel model);

    @Query("SELECT * FROM recipes WHERE id = :id")
    Single<RecipesModel> getRecipe(long id);

    @Query("SELECT * FROM recipes WHERE uuid = :uuid")
    Single<RecipesModel> getRecipeByUuid(UUID uuid);

    @Query("SELECT * FROM recipes ORDER BY name ASC")
    List<RecipesModel> getAllRecipes();

    @Query("SELECT * FROM recipes WHERE name LIKE '%' || :search || '%' ORDER BY name ASC")
    List<RecipesModel> getRecipesLike(String search);

    @Query("SELECT DISTINCT category FROM recipes")
    List<String> getDistinctCategories();

    @Query("SELECT * FROM recipes WHERE category = :category")
    List<RecipesModel> getRecipesForCategory(String category);

}
