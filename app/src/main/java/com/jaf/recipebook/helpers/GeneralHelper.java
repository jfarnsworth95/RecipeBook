package com.jaf.recipebook.helpers;

import com.jaf.recipebook.db.ingredients.IngredientsModel;

import java.util.List;

public class GeneralHelper {
    public static final int ACTIVITY_RESULT_DB_ERROR = 2;

    public static StringBuilder convertIngredientModelArrayToString(List<IngredientsModel> ims){
        StringBuilder ingredientSb = new StringBuilder();
        for (IngredientsModel im : ims){
            ingredientSb.append(im.getText());
            ingredientSb.append(System.lineSeparator());
        }
        ingredientSb.deleteCharAt(ingredientSb.length() - 1);
        return ingredientSb;
    }
}
