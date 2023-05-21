package com.jaf.recipebook.events;

public class DbShutdownEvent {
    public boolean shouldShutdown;

    public DbShutdownEvent(boolean shouldShutdown){
        this.shouldShutdown = shouldShutdown;
    }
}
