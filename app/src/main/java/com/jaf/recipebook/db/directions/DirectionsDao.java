package com.jaf.recipebook.db.directions;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface DirectionsDao {

    @Insert
    void insertDirections(DirectionsModel model);
    @Update
    void updateDirections(DirectionsModel model);
    @Delete
    int deleteDirections(DirectionsModel model);

    @Query("SELECT * FROM directions WHERE recipe_id = :recipe_id")
    List<DirectionsModel> getDirectionsForRecipeId(int recipe_id);

    @Query("DELETE FROM directions WHERE recipe_id = :recipeId")
    int deleteDirectionsById(long recipeId);
}
