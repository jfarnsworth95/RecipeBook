package com.jaf.recipebook.db;

import com.jaf.recipebook.db.directions.DirectionsModel;
import com.jaf.recipebook.db.ingredients.IngredientsModel;
import com.jaf.recipebook.db.recipes.RecipesModel;
import com.jaf.recipebook.db.tags.TagsModel;

import java.util.List;

import io.reactivex.rxjava3.core.Single;

public class FullRecipeTuple {
    public RecipesModel recipesModel;
    public DirectionsModel directionsModel;
    public List<IngredientsModel> ingredientsModel;
    public List<TagsModel> tagsModel;
}
