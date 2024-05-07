package com.jaf.recipebook.db;

import android.content.Context;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.jaf.recipebook.R;
import com.jaf.recipebook.db.directions.DirectionsDao;
import com.jaf.recipebook.db.directions.DirectionsModel;
import com.jaf.recipebook.db.ingredients.IngredientsDao;
import com.jaf.recipebook.db.ingredients.IngredientsModel;
import com.jaf.recipebook.db.recipes.RecipeDao;
import com.jaf.recipebook.db.recipes.RecipesModel;
import com.jaf.recipebook.db.tags.TagsDao;
import com.jaf.recipebook.db.tags.TagsModel;

@Database( entities = {
                RecipesModel.class,
                TagsModel.class,
                DirectionsModel.class,
                IngredientsModel.class
            }, version=7)
public abstract class RecipeBookDatabase extends RoomDatabase {

    private static RecipeBookDatabase instance;
    public abstract RecipeBookDao recipeBookDao();
    public abstract TagsDao tagsDao();
    public abstract DirectionsDao directionsDao();
    public abstract IngredientsDao ingredientsDao();
    public abstract RecipeDao recipeDao();

    public static synchronized RecipeBookDatabase getInstance(Context context){
        if (instance == null){
            instance = Room
                    .databaseBuilder(
                            context.getApplicationContext(),
                            RecipeBookDatabase.class,
                            context.getString(R.string.database_name))
                    .fallbackToDestructiveMigration()
                    .addCallback(roomCallback)
                    .build();
        }

        return instance;
    }

    public static void stopDb() {
        if (instance != null && instance.isOpen()){
            instance.close();
        }
        instance = null;
    }

    public static void destroyAndRecreate(Context context) {
        if (instance != null && instance.isOpen()){
            instance.close();
        }
        context.deleteDatabase(context.getString(R.string.database_name));
        instance = null;
    }

    private static RoomDatabase.Callback roomCallback = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
            // this method is called when database is created
            // and below line is to populate our data.
            new PopulateDbAsyncTask(instance).execute();
        }
    };

    // we are creating an async task class to perform task in background.
    private static class PopulateDbAsyncTask extends AsyncTask<Void, Void, Void> {
        PopulateDbAsyncTask(RecipeBookDatabase instance) {
            RecipeBookDao recipeBookDao = instance.recipeBookDao();
            TagsDao tagsDao = instance.tagsDao();
            DirectionsDao directionsDao = instance.directionsDao();
            IngredientsDao ingredientsDao = instance.ingredientsDao();
            RecipeDao recipeDao = instance.recipeDao();
        }
        @Override
        protected Void doInBackground(Void... voids) {
            return null;
        }
    }
}
