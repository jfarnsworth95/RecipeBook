package com.jaf.recipebook.db.directions;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import io.reactivex.rxjava3.core.Single;

@Dao
public interface DirectionsDao {

    @Insert
    void insertDirections(DirectionsModel model);
    @Update
    void updateDirections(DirectionsModel model);
    @Delete
    int deleteDirections(DirectionsModel model);

    @Query("SELECT * FROM directions WHERE recipe_id = :recipe_id")
    Single<DirectionsModel> getDirectionsForRecipeId(long recipe_id);

    @Query("DELETE FROM directions WHERE recipe_id = :recipeId")
    int deleteDirectionsById(long recipeId);
}
