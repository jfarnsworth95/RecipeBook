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
import com.jaf.recipebook.events.RecipeSavedEvent;

import org.greenrobot.eventbus.EventBus;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class RecipeBookRepo {
    private final RecipeBookDatabase rbdb;
    private final RecipeBookDao recipeBookDao;
    private final TagsDao tagsDao;
    private final DirectionsDao directionsDao;
    private final IngredientsDao ingredientsDao;
    private final RecipeDao recipeDao;

    private String TAG = "RecipeBookRepo";

    public RecipeBookRepo(RecipeBookDatabase rbdb){
        this.rbdb = rbdb;
        recipeBookDao = rbdb.recipeBookDao();
        recipeDao = rbdb.recipeDao();
        ingredientsDao = rbdb.ingredientsDao();
        directionsDao = rbdb.directionsDao();
        tagsDao = rbdb.tagsDao();
    }

    // Add Recipe
    public void insertRecipe(@NonNull RecipesModel rm, @NonNull List<IngredientsModel> ims,
                             @NonNull List<TagsModel> tms, @NonNull DirectionsModel dm, boolean isBulk){
        rbdb.getTransactionExecutor().execute(() -> {
            rbdb.runInTransaction(() -> {
                try {
                    long pk = recipeDao.insertRecipe(rm);
                    Log.i(TAG, "Primary Key of inserted Row: " + pk);

                    for (IngredientsModel im : ims) {
                        im.setRecipe_id(pk);
                    }
                    for (TagsModel tm : tms) {
                        tm.setRecipe_id(pk);
                    }
                    dm.setRecipe_id(pk);

                    ingredientsDao.insertIngredient(ims);
                    directionsDao.insertDirections(dm);
                    tagsDao.insertTag(tms);

                    if (!isBulk) EventBus.getDefault().post(new RecipeSavedEvent(true));
                } catch (Exception ex){
                    Log.e(TAG, "Failed to save recipe data while running INSERT: ", ex);
                    EventBus.getDefault().post(new RecipeSavedEvent(false));
                } finally {
                    Log.d(TAG, "INSERT Transaction END");
                }
            });
        });
    }

    // Delete Recipe
    public void deleteRecipe(RecipesModel rpModel, boolean isBulk){
        rbdb.getTransactionExecutor().execute(() -> {
            rbdb.runInTransaction(() -> {
                try {
                    long deleted = recipeDao.deleteRecipe(rpModel);
                    if (deleted > 0) {
                        long ingDeleted = ingredientsDao.deleteIngredientsById(rpModel.getId());
                        int dirDeleted = directionsDao.deleteDirectionsById(rpModel.getId());
                        int tagDeleted = tagsDao.deleteTagsById(rpModel.getId());
                        Log.d(TAG, "Ingredient entries deleted: " + (ingDeleted > 0));
                        Log.d(TAG, "Directions entry deleted: " + (dirDeleted > 0));
                        Log.d(TAG, "Tag entries deleted: " + (tagDeleted > 0));
                    }

                    if (!isBulk) EventBus.getDefault().post(new RecipeSavedEvent(true));
                } catch (Exception ex){
                    Log.e(TAG, "Failed to save recipe data while running DELETE: ", ex);
                    EventBus.getDefault().post(new RecipeSavedEvent(false));
                } finally {
                    Log.d(TAG, "DELETE Transaction END");
                }
            });
        });
    }

    // Update Recipe
    public void updateRecipe(@NonNull RecipesModel rm, @NonNull List<IngredientsModel> ims,
                             @NonNull List<TagsModel> tms, @NonNull DirectionsModel dm, boolean isBulk) {
        rbdb.getTransactionExecutor().execute(() -> {
            rbdb.runInTransaction(() -> {
                try {
                    if (rm.getId() < 1 && rm.getUuid() != null){
                        rm.setId(recipeDao.getRecipeByUuid(rm.getUuid()).blockingGet().getId());
                        for (IngredientsModel im : ims){
                            im.setRecipe_id(rm.getId());
                        }
                    }
                    recipeDao.updateRecipe(rm);
                    directionsDao.updateDirections(dm);

                    long ingredientsDeleted = ingredientsDao.deleteIngredientsById(rm.getId());
                    if (ingredientsDeleted > 0) {
                        ingredientsDao.insertIngredient(ims);
                    }

                    int tagsDeleted = tagsDao.deleteTagsById(rm.getId());
                    if (tagsDeleted > 0) {
                        tagsDao.insertTag(tms);
                    }

                    if (!isBulk) EventBus.getDefault().post(new RecipeSavedEvent(true));

                } catch (Exception ex) {
                    Log.e(TAG, "Failed to save recipe data while running UPDATE: ", ex);
                    EventBus.getDefault().post(new RecipeSavedEvent(false));
                } finally {
                    Log.d(TAG, "UPDATE Transaction END");
                }
            });
        });
    }

    public FullRecipeTuple getFullRecipeData(@NonNull long recipeId){
        FullRecipeTuple frt = new FullRecipeTuple();
        frt.recipesModel = recipeDao.getRecipe(recipeId).blockingGet();
        frt.ingredientsModel = ingredientsDao.getIngredientsForRecipeId(recipeId);
        frt.directionsModel = directionsDao.getDirectionsForRecipeId(recipeId).blockingGet();
        frt.tagsModel = tagsDao.getTagsForRecipeId(recipeId);

        return frt;
    }
}
