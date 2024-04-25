package com.jaf.recipebook.events;

public class RecipeSavedEvent {
    public boolean recipeAdded;

    public RecipeSavedEvent(boolean recipeAdded){
        this.recipeAdded = recipeAdded;
    }
}
