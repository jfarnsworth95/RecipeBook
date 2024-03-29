package com.jaf.recipebook.db;

import android.util.Log;

import androidx.annotation.NonNull;

import com.jaf.recipebook.db.directions.DirectionsDao;
import com.jaf.recipebook.db.directions.DirectionsModel;
import com.jaf.recipebook.db.ingredients.IngredientsDao;
import com.jaf.recipebook.db.ingredients.IngredientsModel;
import com.jaf.recipebook.db.recipes.RecipeDao;
import com.jaf.recipebook.db.recipes.RecipesModel;
import com.jaf.recipebook.db.tags.TagsDao;
import com.jaf.recipebook.db.tags.TagsModel;
import com.jaf.recipebook.events.RecipeAddedEvent;

import org.greenrobot.eventbus.EventBus;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class RecipeBookRepo {
    private final RecipeBookDao recipeBookDao;
    private final TagsDao tagsDao;
    private final DirectionsDao directionsDao;
    private final IngredientsDao ingredientsDao;
    private final RecipeDao recipeDao;
    private final Executor executor = Executors.newSingleThreadExecutor();

    private String TAG = "RecipeBookRepo";

    public RecipeBookRepo(RecipeBookDatabase rbdb){
        recipeBookDao = rbdb.recipeBookDao();
        tagsDao = rbdb.tagsDao();
        directionsDao = rbdb.directionsDao();
        ingredientsDao = rbdb.ingredientsDao();
        recipeDao = rbdb.recipeDao();
    }

    // Add Recipe
    public void insertRecipe(@NonNull RecipesModel rm, @NonNull List<IngredientsModel> ims,
                             @NonNull List<TagsModel> tms, @NonNull DirectionsModel dm){
        executor.execute(new Runnable() {
            @Override
            public void run() {
                long pk = recipeDao.insertRecipe(rm);
                Log.i(TAG, "Primary Key of inserted Row: " + pk);

                for (IngredientsModel im : ims) {
                    im.setRecipe_id(pk);
                }
                for (TagsModel tm : tms){
                    tm.setRecipe_id(pk);
                }
                dm.setRecipe_id(pk);

                ingredientsDao.insertIngredient(ims);
                directionsDao.insertDirections(dm);
                tagsDao.insertTag(tms);

                EventBus.getDefault().post(new RecipeAddedEvent(true));
            }
        });
    }

    // Delete Recipe
    public void deleteRecipe(RecipesModel rpModel){
        executor.execute(new Runnable() {
            @Override
            public void run() {
                int deleted = recipeDao.deleteRecipe(rpModel);
                if (deleted > 0){
                    int ingDeleted = ingredientsDao.deleteIngredientsById(rpModel.getId());
                    int dirDeleted = directionsDao.deleteDirectionsById(rpModel.getId());
                    int tagDeleted = tagsDao.deleteTagsById(rpModel.getId());
                    Log.d(TAG, "Ingredient entries deleted: " + (ingDeleted > 0));
                    Log.d(TAG, "Directions entry deleted: " + (dirDeleted > 0));
                    Log.d(TAG, "Tag entries deleted: " + (tagDeleted > 0));
                }
            }
        });
    }

    // Update Recipe
    public void updateRecipe(@NonNull RecipesModel rm, @NonNull List<IngredientsModel> ims,
                             @NonNull List<TagsModel> tms, @NonNull DirectionsModel dm){
        executor.execute(new Runnable() {
            @Override
            public void run() {
                recipeDao.updateRecipe(rm);
                directionsDao.updateDirections(dm);

                int ingredientsDeleted = ingredientsDao.deleteIngredientsById(rm.getId());
                if (ingredientsDeleted > 0){
                    ingredientsDao.insertIngredient(ims);
                }

                int tagsDeleted = tagsDao.deleteTagsById(rm.getId());
                if (tagsDeleted > 0){
                    tagsDao.insertTag(tms);
                }
            }
        });
    }
}
