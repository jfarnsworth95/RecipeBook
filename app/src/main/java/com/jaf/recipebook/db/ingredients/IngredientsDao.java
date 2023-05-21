package com.jaf.recipebook.db.ingredients;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.jaf.recipebook.db.directions.DirectionsModel;

import java.util.List;

@Dao
public interface IngredientsDao {

    @Insert
    void insertIngredient(IngredientsModel model);
    @Insert
    void insertIngredient(List<IngredientsModel> models);
    @Update
    void updateIngredient(IngredientsModel model);
    @Delete
    int deleteIngredient(IngredientsModel model);

    @Query("SELECT * FROM ingredients WHERE recipe_id = :recipe_id ORDER BY ORDER_ID ASC")
    List<IngredientsModel> getIngredientsForRecipeId(int recipe_id);

    @Query("DELETE FROM ingredients WHERE recipe_id = :recipeId")
    int deleteIngredientsById(long recipeId);

}
