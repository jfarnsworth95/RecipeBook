package com.jaf.recipebook.db.tags;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface TagsDao {

    @Insert
    void insertTag(TagsModel model);
    @Insert
    void insertTag(List<TagsModel> models);
    @Update
    void updateTag(TagsModel model);
    @Delete
    int deleteTag(TagsModel model);

    @Query("SELECT * FROM tags WHERE recipe_id = :recipe_id")
    List<TagsModel> getTagsForRecipeId(long recipe_id);

    @Query("SELECT * FROM tags WHERE tag = :tag")
    List<TagsModel> getRecipesWithTag(String tag);

    @Query("SELECT DISTINCT tag FROM tags")
    List<String> getAllTags();

    @Query("DELETE FROM tags WHERE recipe_id = :recipeId")
    int deleteTagsById(long recipeId);

}
