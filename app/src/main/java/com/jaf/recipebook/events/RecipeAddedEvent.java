package com.jaf.recipebook.events;

public class RecipeAddedEvent {
    public boolean recipeAdded;

    public RecipeAddedEvent(boolean recipeAdded){
        this.recipeAdded = recipeAdded;
    }
}
