package com.jaf.recipebook.events;

public class RecipeSavedEvent {
    public boolean recipeSaved;

    public RecipeSavedEvent(boolean recipeSaved){
        this.recipeSaved = recipeSaved;
    }
}
