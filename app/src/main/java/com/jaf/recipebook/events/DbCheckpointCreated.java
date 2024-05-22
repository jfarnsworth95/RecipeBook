package com.jaf.recipebook.events;

public class DbCheckpointCreated {
    public boolean success;

    public DbCheckpointCreated(boolean success){
        this.success = success;
    }
}
